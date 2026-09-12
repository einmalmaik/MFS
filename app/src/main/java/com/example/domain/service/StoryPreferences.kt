package com.example.domain.service

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.api.GeminiClient
import com.example.data.model.AiSettings

/**
 * Manages persisted user preferences and secure credentials.
 * Decoupled from database and network logic for easy migration into any project.
 */
class StoryPreferences(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("storyforge_prefs", Context.MODE_PRIVATE)

  companion object {
    private const val PREF_CUSTOM_API_KEY = "custom_gemini_api_key"
    private const val PREF_GLOBAL_DEFAULT_PROMPT = "global_default_system_prompt"

    private const val PREF_CHAT_MODEL = "ai_chat_model"
    private const val PREF_EMBEDDING_MODEL = "ai_embedding_model"
    private const val PREF_TRANSCRIPTION_MODEL = "ai_transcription_model"
    private const val PREF_THINKING_LEVEL = "ai_thinking_level"
    private const val PREF_THINKING_BUDGET = "ai_thinking_budget"
    private const val PREF_TEMPERATURE = "ai_temperature"
    private const val PREF_SUPPORTS_TEMPERATURE = "ai_supports_temperature"
    private const val PREF_ADULT_CONTENT = "ai_adult_content"
    private const val PREF_AI_SETTINGS_SEEDED = "ai_settings_seeded"
  }

  fun getCustomApiKey(): String? {
    return prefs.getString(PREF_CUSTOM_API_KEY, null)
  }

  fun setCustomApiKey(key: String?) {
    prefs.edit().putString(PREF_CUSTOM_API_KEY, key?.trim()).apply()
  }

  fun getGlobalDefaultSystemPrompt(): String {
    return prefs.getString(PREF_GLOBAL_DEFAULT_PROMPT, GeminiClient.DEFAULT_SYSTEM_PROMPT)
      ?: GeminiClient.DEFAULT_SYSTEM_PROMPT
  }

  fun setGlobalDefaultSystemPrompt(prompt: String) {
    prefs.edit().putString(PREF_GLOBAL_DEFAULT_PROMPT, prompt.trim()).apply()
  }

  // --- GLOBALE KI-EINSTELLUNGEN ---

  fun getAiSettings(): AiSettings {
    val fallback = AiSettings()
    return AiSettings(
      chatModel = prefs.getString(PREF_CHAT_MODEL, null)?.takeIf { it.isNotBlank() }
        ?: fallback.chatModel,
      embeddingModel = prefs.getString(PREF_EMBEDDING_MODEL, null)?.takeIf { it.isNotBlank() }
        ?: fallback.embeddingModel,
      transcriptionModel = prefs.getString(PREF_TRANSCRIPTION_MODEL, null)?.takeIf { it.isNotBlank() }
        ?: fallback.transcriptionModel,
      thinkingLevel = prefs.getString(PREF_THINKING_LEVEL, null)?.takeIf { it.isNotBlank() }
        ?: fallback.thinkingLevel,
      thinkingBudget = prefs.getInt(PREF_THINKING_BUDGET, fallback.thinkingBudget),
      temperature = prefs.getFloat(PREF_TEMPERATURE, fallback.temperature),
      supportsTemperature = prefs.getBoolean(PREF_SUPPORTS_TEMPERATURE, fallback.supportsTemperature),
      adultContentEnabled = prefs.getBoolean(PREF_ADULT_CONTENT, fallback.adultContentEnabled)
    )
  }

  fun setAiSettings(settings: AiSettings) {
    prefs.edit()
      .putString(PREF_CHAT_MODEL, settings.chatModel)
      .putString(PREF_EMBEDDING_MODEL, settings.embeddingModel)
      .putString(PREF_TRANSCRIPTION_MODEL, settings.transcriptionModel)
      .putString(PREF_THINKING_LEVEL, settings.thinkingLevel)
      .putInt(PREF_THINKING_BUDGET, settings.thinkingBudget)
      .putFloat(PREF_TEMPERATURE, settings.temperature)
      .putBoolean(PREF_SUPPORTS_TEMPERATURE, settings.supportsTemperature)
      .putBoolean(PREF_ADULT_CONTENT, settings.adultContentEnabled)
      .apply()
  }

  /**
   * Übernimmt die Einstellungen einer bestehenden Geschichte genau einmal.
   *
   * Wer die App aktualisiert, hat seine Wahl bereits pro Geschichte getroffen. Ohne diese
   * Übernahme stünde nach dem Update überall wieder der Vorgabewert — und ein Wechsel des
   * Einbettungsmodells macht das gesamte bisherige Gedächtnis unlesbar.
   *
   * @return true, wenn übernommen wurde. false, wenn das schon geschehen ist.
   */
  fun seedAiSettingsOnce(from: AiSettings): Boolean {
    if (prefs.getBoolean(PREF_AI_SETTINGS_SEEDED, false)) return false
    setAiSettings(from)
    prefs.edit().putBoolean(PREF_AI_SETTINGS_SEEDED, true).apply()
    return true
  }

  /**
   * Returns the user's custom key if provided, otherwise falls back to BuildConfig.
   */
  fun getEffectiveApiKey(): String {
    val custom = getCustomApiKey()
    if (!custom.isNullOrBlank()) return custom.trim()
    val buildKey = BuildConfig.GEMINI_API_KEY
    if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
      return buildKey
    }
    return ""
  }
}
