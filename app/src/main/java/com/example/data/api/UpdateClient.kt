package com.example.data.api

import android.util.Log
import com.example.data.model.UpdateRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * Holt die Veröffentlichungsdaten von GitHub und lädt die APK.
 *
 * Bewusst ein **eigener** OkHttpClient, getrennt von [GeminiClient]: Der dortige Client trägt
 * den Gemini-Schlüssel im Kopffeld jeder Anfrage. Ein geteilter Client wäre eine Einladung,
 * ihn eines Tages versehentlich an GitHub mitzuschicken (CLAUDE.md §6).
 *
 * Diese Klasse kennt keine Einstellungen, keine Versionslogik und keinen Zwischenspeicher —
 * nur das Netz. Die Regeln stehen in [com.example.domain.service.UpdateService].
 */
class UpdateClient {

  private val client = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()

  companion object {
    private const val TAG = "UpdateClient"

    private const val REPO = "einmalmaik/MFS"

    /**
     * Der bevorzugte Weg. `releases/latest/download/<name>` kommt vom Release-CDN und
     * unterliegt nicht dem API-Limit von 60 anonymen Anfragen pro Stunde und IP-Adresse —
     * hinter Mobilfunk-CGNAT teilen sich viele Geräte eine IP.
     */
    private const val LATEST_JSON_URL =
      "https://github.com/$REPO/releases/latest/download/latest.json"

    /** Nur der Rückfall, wenn latest.json fehlt. */
    private const val RELEASES_API_URL =
      "https://api.github.com/repos/$REPO/releases/latest"

    /**
     * Wohin die APK heruntergeladen werden darf.
     *
     * Ohne diese Liste könnte eine veränderte latest.json den Download auf einen beliebigen
     * Server umlenken. Die Datei würde dort zwar an der Signaturprüfung scheitern — aber erst
     * nach etlichen Megabyte Mobilfunkvolumen und einem Kontakt zu einem fremden Server, der
     * die IP-Adresse des Spielers mitschreibt.
     */
    private val ERLAUBTE_HOSTS = setOf(
      "github.com",
      "objects.githubusercontent.com",
      "release-assets.githubusercontent.com"
    )

    /** true, wenn die Adresse per HTTPS auf einen der erlaubten Hosts zeigt. */
    fun istErlaubteApkAdresse(url: String): Boolean {
      if (url.isBlank()) return false
      val uri = try {
        java.net.URI(url)
      } catch (_: Exception) {
        return false
      }
      return uri.scheme == "https" && uri.host in ERLAUBTE_HOSTS
    }
  }

  /**
   * Fragt die neueste Veröffentlichung ab.
   *
   * @return null, wenn es keine gibt, das Repo nicht erreichbar ist oder die Antwort nicht
   *   vollständig gelesen werden kann. Es wird nie geraten: Eine unvollständige latest.json
   *   gilt wie "keine Veröffentlichung".
   */
  suspend fun fetchLatestRelease(): UpdateRelease? = withContext(Dispatchers.IO) {
    parseLatestJson(get(LATEST_JSON_URL))
      ?: parseReleasesApi(get(RELEASES_API_URL))
  }

  private fun get(url: String): String? = try {
    client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
      // 404 heisst "kein Release" oder "Repo privat" — GitHub unterscheidet das bewusst nicht.
      // Beides ist eine gültige, abschliessende Antwort und kein Fehler.
      if (response.isSuccessful) response.body?.string() else null
    }
  } catch (e: Exception) {
    Log.d(TAG, "Update-Abfrage nicht möglich: ${e.message}")
    null
  }

  private fun parseLatestJson(body: String?): UpdateRelease? {
    if (body.isNullOrBlank()) return null
    return try {
      val json = JSONObject(body)
      val versionCode = json.optInt("versionCode", 0)
      val apk = json.optString("apk")
      if (versionCode <= 0 || !istErlaubteApkAdresse(apk)) return null

      UpdateRelease(
        versionName = json.optString("versionName").ifBlank { versionCode.toString() },
        versionCode = versionCode,
        apkUrl = apk,
        sha256 = json.optString("sha256").lowercase(),
        sizeBytes = json.optLong("sizeBytes", 0L),
        notes = json.optString("notes")
      )
    } catch (e: Exception) {
      Log.d(TAG, "latest.json unlesbar: ${e.message}")
      null
    }
  }

  /**
   * Rückfall über die GitHub-API. Hier fehlt der versionCode — er wird aus dem Tag hergeleitet,
   * und nur dann, wenn der Tag ausschliesslich aus Zahlen und Punkten besteht. Ein Tag wie
   * `v1.0.2-beta` liefert bewusst nichts: Im Zweifel wird kein Update angeboten.
   */
  private fun parseReleasesApi(body: String?): UpdateRelease? {
    if (body.isNullOrBlank()) return null
    return try {
      val json = JSONObject(body)
      val tag = json.optString("tag_name").removePrefix("v").trim()
      if (tag.isBlank() || !tag.split(".").all { it.isNotEmpty() && it.all(Char::isDigit) }) {
        return null
      }

      val assets = json.optJSONArray("assets") ?: return null
      var apkUrl = ""
      var size = 0L
      for (i in 0 until assets.length()) {
        val asset = assets.optJSONObject(i) ?: continue
        if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
          apkUrl = asset.optString("browser_download_url")
          size = asset.optLong("size", 0L)
          break
        }
      }
      if (!istErlaubteApkAdresse(apkUrl)) return null

      UpdateRelease(
        versionName = tag,
        versionCode = versionCodeAusTag(tag) ?: return null,
        apkUrl = apkUrl,
        // Die API liefert keinen SHA-256 der Datei. Ohne ihn entfällt die Hash-Prüfung; die
        // Signaturprüfung vor der Installation bleibt und ist die tragende Absicherung.
        sha256 = "",
        sizeBytes = size,
        notes = json.optString("body")
      )
    } catch (e: Exception) {
      Log.d(TAG, "Releases-API unlesbar: ${e.message}")
      null
    }
  }

  /**
   * Lädt die APK nach [ziel] und meldet den Fortschritt als Anteil von 0 bis 1.
   *
   * Geschrieben wird zuerst nach `<ziel>.part` und erst am Ende umbenannt. Ein abgebrochener
   * Download hinterlässt so nie eine abgeschnittene Datei, die der Installer für vollständig hält.
   *
   * @return der SHA-256 der geladenen Datei, oder null bei Abbruch oder Fehler.
   */
  suspend fun downloadApk(
    url: String,
    ziel: File,
    onProgress: (Float) -> Unit
  ): String? = withContext(Dispatchers.IO) {
    if (!istErlaubteApkAdresse(url)) {
      Log.w(TAG, "Download abgelehnt: Adresse zeigt nicht auf GitHub")
      return@withContext null
    }

    val teil = File(ziel.absolutePath + ".part")
    try {
      client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
        if (!response.isSuccessful) return@withContext null
        val body = response.body ?: return@withContext null
        val gesamt = body.contentLength()

        val digest = MessageDigest.getInstance("SHA-256")
        teil.outputStream().use { out ->
          body.byteStream().use { input ->
            val puffer = ByteArray(64 * 1024)
            var geladen = 0L
            while (true) {
              coroutineContext.ensureActive()
              val gelesen = input.read(puffer)
              if (gelesen <= 0) break
              out.write(puffer, 0, gelesen)
              digest.update(puffer, 0, gelesen)
              geladen += gelesen
              if (gesamt > 0) onProgress((geladen.toFloat() / gesamt).coerceIn(0f, 1f))
            }
          }
        }

        if (!teil.renameTo(ziel)) return@withContext null
        onProgress(1f)
        digest.digest().joinToString("") { "%02x".format(it) }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Download fehlgeschlagen: ${e.message}")
      teil.delete()
      null
    }
  }
}

/**
 * Leitet einen versionCode aus einem rein numerischen Tag ab: `1.0.2` wird zu 10002.
 *
 * Nur für den API-Rückfall. Liefert latest.json den echten versionCode mit, wird diese
 * Schätzung nicht gebraucht — und sie ist ausdrücklich eine Schätzung.
 */
internal fun versionCodeAusTag(tag: String): Int? {
  val teile = tag.trim().removePrefix("v").split(".")
  if (teile.isEmpty() || teile.size > 3) return null

  val zahlen = teile.map { it.toIntOrNull() ?: return null }
  if (zahlen.any { it < 0 }) return null

  val major = zahlen.getOrElse(0) { 0 }
  val minor = zahlen.getOrElse(1) { 0 }
  val patch = zahlen.getOrElse(2) { 0 }
  if (minor > 99 || patch > 99) return null

  return major * 10_000 + minor * 100 + patch
}
