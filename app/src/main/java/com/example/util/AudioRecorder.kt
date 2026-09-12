package com.example.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileInputStream

class AudioRecorder(private val context: Context) {
  private var mediaRecorder: MediaRecorder? = null
  private var currentFile: File? = null
  private var isRecording = false

  companion object {
    private const val FILE_PREFIX = "voice_input_"

    /**
     * Harte Obergrenze einer Aufnahme.
     *
     * Ohne sie läuft das Mikrofon, bis jemand den Haken drückt — und beim Drücken geht die
     * gesamte Spanne an Google. Fünf Minuten sind mehr, als ein Spielzug je braucht, und
     * wenig genug, dass ein vergessenes Mikrofon nicht den halben Abend mitschneidet.
     */
    const val MAX_RECORDING_SECONDS = 300
  }

  fun startRecording(): Boolean {
    try {
      // Reste aufräumen, bevor neue entstehen. Stirbt der Prozess während einer Aufnahme,
      // bliebe die halbfertige Datei mit allem bis dahin Gehörten sonst für immer im Cache.
      deleteOrphanedRecordings()

      val outputDir = context.cacheDir
      val outputFile = File(outputDir, "$FILE_PREFIX${System.currentTimeMillis()}.m4a")
      currentFile = outputFile

      val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
      } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
      }

      recorder.apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setAudioEncodingBitRate(128000)
        setAudioSamplingRate(44100)
        setOutputFile(outputFile.absolutePath)
        prepare()
        start()
      }

      mediaRecorder = recorder
      isRecording = true
      return true
    } catch (e: Exception) {
      Log.e("AudioRecorder", "Failed to start recording", e)
      cancelRecording()
      return false
    }
  }

  fun stopRecording(): ByteArray? {
    if (!isRecording) return null
    return try {
      mediaRecorder?.apply {
        stop()
        reset()
        release()
      }
      mediaRecorder = null
      isRecording = false

      val file = currentFile
      if (file != null && file.exists() && file.length() > 0) {
        val bytes = FileInputStream(file).use { it.readBytes() }
        file.delete()
        currentFile = null
        bytes
      } else {
        file?.delete()
        currentFile = null
        null
      }
    } catch (e: Exception) {
      Log.e("AudioRecorder", "Failed to stop recording cleanly", e)
      cancelRecording()
      null
    }
  }

  fun cancelRecording() {
    try {
      mediaRecorder?.apply {
        try {
          stop()
        } catch (_: Exception) {}
        reset()
        release()
      }
    } catch (_: Exception) {}
    mediaRecorder = null
    isRecording = false
    try {
      currentFile?.delete()
    } catch (_: Exception) {}
    currentFile = null
  }

  /**
   * Löscht Aufnahmedateien, die ein abgestürzter oder vom System beendeter Prozess
   * zurückgelassen hat. Die laufende Aufnahme bleibt unberührt.
   */
  private fun deleteOrphanedRecordings() {
    try {
      context.cacheDir.listFiles { file -> file.name.startsWith(FILE_PREFIX) }
        ?.forEach { file ->
          if (file.absolutePath != currentFile?.absolutePath) file.delete()
        }
    } catch (e: Exception) {
      Log.w("AudioRecorder", "Alte Aufnahmen konnten nicht aufgeräumt werden", e)
    }
  }

  fun getMaxAmplitudeRatio(): Float {
    if (!isRecording) return 0f
    return try {
      val amp = mediaRecorder?.maxAmplitude ?: 0
      (amp / 32767f).coerceIn(0f, 1f)
    } catch (_: Exception) {
      0f
    }
  }

  fun isCurrentlyRecording(): Boolean = isRecording
}
