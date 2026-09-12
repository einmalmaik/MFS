package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

/**
 * Übergibt eine geprüfte APK an den System-Installer.
 *
 * Bewusst über `ACTION_VIEW` und nicht über `PackageInstaller`. Der einzige Vorteil des
 * letzteren wäre `USER_ACTION_NOT_REQUIRED` — die Installation ohne Nutzerbestätigung. Genau
 * das darf MSF nicht tun. Ohne dieses Flag bliebe von `PackageInstaller` nur der Aufwand:
 * Session, Streaming, PendingIntent, ein zusätzlicher BroadcastReceiver im Manifest und die
 * Behandlung von `STATUS_PENDING_USER_ACTION` — rund 90 Zeilen für ein Ergebnis, das der
 * Nutzer identisch sieht.
 *
 * Der `content://`-URI aus dem FileProvider ist der Teil, der wirklich nötig ist: Ein
 * `file://`-URI löst ab Android 7 eine `FileUriExposedException` aus.
 */
object ApkInstaller {
  private const val TAG = "ApkInstaller"

  /** true, wenn das System MSF erlaubt, eine Installation anzustossen. */
  fun darfInstallieren(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.packageManager.canRequestPackageInstalls()
    } else {
      // Vor Android 8 gibt es die App-eigene Berechtigung nicht; dort greift der globale
      // Schalter "Unbekannte Herkunft", den die App nicht abfragen kann.
      true
    }

  /**
   * Öffnet die Systemeinstellung, in der der Nutzer MSF die Installation erlauben kann.
   * Die Hürde gehört dem System — MSF kann und soll sie nicht umgehen, nur erklären.
   */
  fun oeffneFreigabeEinstellung(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    try {
      context.startActivity(
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
          .setData(Uri.parse("package:${context.packageName}"))
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      )
    } catch (e: Exception) {
      Log.w(TAG, "Freigabe-Einstellung nicht erreichbar", e)
    }
  }

  /** @return false, wenn kein Installer erreichbar war. */
  fun installiere(context: Context, apk: File): Boolean {
    if (!apk.exists()) return false
    return try {
      val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
      context.startActivity(
        Intent(Intent.ACTION_VIEW).apply {
          setDataAndType(uri, "application/vnd.android.package-archive")
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      )
      true
    } catch (e: Exception) {
      Log.w(TAG, "Installation konnte nicht gestartet werden", e)
      false
    }
  }
}
