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

  fun startRecording(): Boolean {
    try {
      val outputDir = context.cacheDir
      val outputFile = File(outputDir, "voice_input_${System.currentTimeMillis()}.m4a")
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
