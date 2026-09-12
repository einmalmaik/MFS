package com.example.domain.service

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.data.api.UpdateClient
import com.example.data.model.UpdateRelease
import com.example.data.model.UpdateState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Entscheidet, ob, wann und was aktualisiert wird.
 *
 * Drei Grenzen, die MSF sich selbst setzt, weil "kein unbemerktes Handeln" sonst nur ein
 * Werbespruch wäre:
 *
 * 1. **Die Prüfung braucht eine Zustimmung.** MSF wirbt damit, dass Google die einzige
 *    Gegenstelle ist. Eine Prüfung beim Start fügt eine zweite hinzu und verrät GitHub bei
 *    jedem Start die IP-Adresse. Wenig, aber nicht nichts — also nur nach Zustimmung, Vorgabe aus.
 * 2. **Der Download braucht einen eigenen Tastendruck.** Still im Hintergrund zu laden
 *    verbraucht fremdes Mobilfunkvolumen.
 * 3. **Die Installation zeigt immer der System-Dialog.** Auf `USER_ACTION_NOT_REQUIRED` wird
 *    bewusst und dauerhaft verzichtet.
 *
 * Die automatische Prüfung schweigt in jedem Fall — auch im Fehlerfall. Die vom Nutzer
 * ausgelöste Prüfung antwortet in jedem Fall. Das ist die Auflösung des Widerspruchs zwischen
 * "nicht stören" und "nichts verschweigen".
 */
class UpdateService(
  private val context: Context,
  private val preferences: StoryPreferences,
  private val client: UpdateClient = UpdateClient()
) {

  companion object {
    private const val TAG = "UpdateService"

    /** Höchstens eine automatische Prüfung pro Tag. Eine Erzähl-App braucht kein Funkfeuer. */
    private const val PRUEF_INTERVALL_MS = 24L * 60 * 60 * 1000

    /** Kaltstart, erste Erzählung und Modellkatalog haben Vorrang. */
    private const val START_VERZOEGERUNG_MS = 5_000L

    private const val CACHE_ORDNER = "updates"

    /** Entscheidet über den Vergleich. Rein, ohne Android — damit im JVM-Test prüfbar. */
    fun istNeuereVersion(fernerCode: Int, lokalerCode: Int): Boolean =
      fernerCode > lokalerCode
  }

  private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
  val state: StateFlow<UpdateState> = _state.asStateFlow()

  /** Der versionCode der tatsächlich installierten App — nicht der des gebauten Artefakts. */
  val installierterVersionCode: Int by lazy {
    try {
      val info = context.packageManager.getPackageInfo(context.packageName, 0)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.longVersionCode.toInt()
      } else {
        @Suppress("DEPRECATION")
        info.versionCode
      }
    } catch (e: Exception) {
      Log.w(TAG, "Eigene Version nicht lesbar", e)
      0
    }
  }

  val installierterVersionName: String by lazy {
    try {
      context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    } catch (_: Exception) {
      ""
    }
  }

  fun istPruefungAktiv(): Boolean = preferences.isUpdateCheckEnabled()

  fun wurdeZustimmungGefragt(): Boolean = preferences.wasUpdateConsentAsked()

  fun letztePruefung(): Long = preferences.getUpdateLastCheckAt()

  fun setzePruefung(aktiv: Boolean) {
    preferences.setUpdateCheckEnabled(aktiv)
    preferences.markUpdateConsentAsked()
  }

  /**
   * Die Prüfung beim App-Start.
   *
   * Bricht an jeder Bedingung still ab. [manuell] dreht das um: Dann wird jedes Ergebnis
   * gemeldet, auch "es gibt nichts Neues" und "keine Verbindung".
   */
  suspend fun pruefe(manuell: Boolean) {
    if (!manuell) {
      if (!preferences.isUpdateCheckEnabled()) return
      val seitLetzter = System.currentTimeMillis() - preferences.getUpdateLastCheckAt()
      if (seitLetzter < PRUEF_INTERVALL_MS) return
      delay(START_VERZOEGERUNG_MS)
    }

    if (manuell) _state.value = UpdateState.Pruefend

    val release = client.fetchLatestRelease()
    if (release == null) {
      // 404 und Netzfehler sind hier nicht unterscheidbar — GitHub antwortet auf private
      // Repositories mit 404, um ihre Existenz nicht zu verraten. Der Zeitstempel wird
      // trotzdem gesetzt: Jeden Start erneut anzufragen wäre genau das Funkfeuer, das
      // die Zustimmung nicht abdeckt.
      preferences.setUpdateLastCheckAt(System.currentTimeMillis())
      _state.value = if (manuell) {
        UpdateState.Meldung("Keine Veröffentlichung gefunden oder GitHub nicht erreichbar.")
      } else {
        UpdateState.Idle
      }
      return
    }

    preferences.setUpdateLastCheckAt(System.currentTimeMillis())

    if (!istNeuereVersion(release.versionCode, installierterVersionCode)) {
      _state.value = if (manuell) {
        UpdateState.Meldung("Version $installierterVersionName ist aktuell.")
      } else {
        UpdateState.Idle
      }
      return
    }

    if (!manuell && preferences.getUpdateSkippedVersionCode() >= release.versionCode) {
      _state.value = UpdateState.Idle
      return
    }

    _state.value = UpdateState.Verfuegbar(release)
  }

  /** Lädt die APK und prüft sie, bevor sie überhaupt angeboten wird. */
  suspend fun lade(release: UpdateRelease) {
    _state.value = UpdateState.Laedt(release, 0f)

    val ziel = withContext(Dispatchers.IO) {
      val ordner = File(context.cacheDir, CACHE_ORDNER)
      // Vor jedem Download leeren: Es liegt immer höchstens eine Datei dort.
      ordner.deleteRecursively()
      ordner.mkdirs()
      File(ordner, "msf-${release.versionCode}.apk")
    }

    val hash = client.downloadApk(release.apkUrl, ziel) { anteil ->
      _state.value = UpdateState.Laedt(release, anteil)
    }

    if (hash == null) {
      _state.value = UpdateState.Fehler("Die Aktualisierung konnte nicht geladen werden.")
      return
    }

    val problem = pruefeDatei(ziel, release, hash)
    if (problem != null) {
      withContext(Dispatchers.IO) { ziel.delete() }
      _state.value = UpdateState.Fehler(problem)
      return
    }

    _state.value = UpdateState.Bereit(release)
  }

  fun apkDatei(release: UpdateRelease): File =
    File(File(context.cacheDir, CACHE_ORDNER), "msf-${release.versionCode}.apk")

  fun ueberspringe(release: UpdateRelease) {
    preferences.setUpdateSkippedVersionCode(release.versionCode)
    _state.value = UpdateState.Idle
  }

  fun verwerfeZustand() {
    _state.value = UpdateState.Idle
  }

  /**
   * Die drei Kontrollen vor der Installation. @return die Fehlermeldung, oder null wenn alles passt.
   *
   * Die dritte ist die wichtigste. Passt das Signaturzertifikat nicht, lehnt Android die
   * Installation mit `INSTALL_FAILED_UPDATE_INCOMPATIBLE` ab — auf dem Gerät meist als
   * "App nicht installiert". Der naheliegende Ausweg wäre dann, die alte App zu deinstallieren.
   * Damit wären alle Geschichten, Checkpoints und das gesamte Gedächtnis weg. Diese Prüfung
   * kann den Fehler nicht heilen, aber sie ersetzt eine unverständliche Systemmeldung durch
   * einen Satz, der das Schlimmste verhindert.
   */
  private suspend fun pruefeDatei(
    datei: File,
    release: UpdateRelease,
    hash: String
  ): String? = withContext(Dispatchers.IO) {
    if (release.sha256.isNotBlank() && !hash.equals(release.sha256, ignoreCase = true)) {
      return@withContext "Die geladene Datei stimmt nicht mit der Veröffentlichung überein " +
        "und wurde verworfen."
    }

    val paket = try {
      context.packageManager.getPackageArchiveInfo(datei.absolutePath, 0)
    } catch (e: Exception) {
      Log.w(TAG, "APK nicht lesbar", e)
      null
    } ?: return@withContext "Die geladene Datei ist keine lesbare Android-App."

    if (paket.packageName != context.packageName) {
      return@withContext "Die geladene Datei gehört zu einer anderen App " +
        "(${paket.packageName}) und wurde verworfen."
    }

    val eigene = zertifikatsFingerabdruecke(context.packageName, null)
    val neue = zertifikatsFingerabdruecke(null, datei.absolutePath)
    if (eigene.isNotEmpty() && neue.isNotEmpty() && eigene.intersect(neue).isEmpty()) {
      return@withContext "Diese Aktualisierung wurde mit einem anderen Schlüssel signiert und " +
        "kann nicht über die installierte App gelegt werden. Deine Geschichten sind sicher — " +
        "deinstalliere die App nicht, sonst gehen sie verloren."
    }

    null
  }

  /** SHA-256 der Signaturzertifikate, entweder der installierten App oder einer APK-Datei. */
  private fun zertifikatsFingerabdruecke(paketName: String?, apkPfad: String?): Set<String> {
    return try {
      @Suppress("DEPRECATION")
      val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES
      } else {
        PackageManager.GET_SIGNATURES
      }

      val info = if (paketName != null) {
        context.packageManager.getPackageInfo(paketName, flag)
      } else {
        context.packageManager.getPackageArchiveInfo(apkPfad!!, flag)
      } ?: return emptySet()

      val signaturen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.signingInfo?.apkContentsSigners
      } else {
        @Suppress("DEPRECATION")
        info.signatures
      } ?: return emptySet()

      val digest = MessageDigest.getInstance("SHA-256")
      signaturen.filterNotNull()
        .map { digest.digest(it.toByteArray()).joinToString("") { b -> "%02x".format(b) } }
        .toSet()
    } catch (e: Exception) {
      Log.w(TAG, "Signatur nicht lesbar", e)
      emptySet()
    }
  }
}
