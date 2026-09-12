package com.example.domain.service

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AiSettings
import com.example.domain.engine.StoryPrompts

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

    private const val PREF_UPDATE_CHECK_ENABLED = "update_check_enabled"
    private const val PREF_UPDATE_CONSENT_ASKED = "update_consent_asked"
    private const val PREF_UPDATE_LAST_CHECK_AT = "update_last_check_at"
    private const val PREF_UPDATE_SKIPPED_VERSION = "update_skipped_version_code"
  }

  // --- AKTUALISIERUNG ---

  /**
   * Vorgabe false. Eine Prüfung beim Start fügt GitHub als zweite Gegenstelle neben Google
   * hinzu und verrät ihr bei jedem Start die IP-Adresse. Das ist wenig, aber es geschieht ohne
   * Anlass des Nutzers — also erst nach ausdrücklicher Zustimmung.
   */
  fun isUpdateCheckEnabled(): Boolean = prefs.getBoolean(PREF_UPDATE_CHECK_ENABLED, false)

  fun setUpdateCheckEnabled(enabled: Boolean) {
    prefs.edit().putBoolean(PREF_UPDATE_CHECK_ENABLED, enabled).apply()
  }

  /** Verhindert, dass die einmalige Frage nach der Zustimmung wiedervorgelegt wird. */
  fun wasUpdateConsentAsked(): Boolean = prefs.getBoolean(PREF_UPDATE_CONSENT_ASKED, false)

  fun markUpdateConsentAsked() {
    prefs.edit().putBoolean(PREF_UPDATE_CONSENT_ASKED, true).apply()
  }

  fun getUpdateLastCheckAt(): Long = prefs.getLong(PREF_UPDATE_LAST_CHECK_AT, 0L)

  fun setUpdateLastCheckAt(timestamp: Long) {
    prefs.edit().putLong(PREF_UPDATE_LAST_CHECK_AT, timestamp).apply()
  }

  /** Eine übersprungene Fassung wird nie wieder angeboten — eine neuere schon. */
  fun getUpdateSkippedVersionCode(): Int = prefs.getInt(PREF_UPDATE_SKIPPED_VERSION, 0)

  fun setUpdateSkippedVersionCode(versionCode: Int) {
    prefs.edit().putInt(PREF_UPDATE_SKIPPED_VERSION, versionCode).apply()
  }

  fun getCustomApiKey(): String? {
    return prefs.getString(PREF_CUSTOM_API_KEY, null)
  }

  fun setCustomApiKey(key: String?) {
    prefs.edit().putString(PREF_CUSTOM_API_KEY, key?.trim()).apply()
  }

  fun getGlobalDefaultSystemPrompt(): String {
    return prefs.getString(PREF_GLOBAL_DEFAULT_PROMPT, StoryPrompts.DEFAULT_SYSTEM_PROMPT)
      ?: StoryPrompts.DEFAULT_SYSTEM_PROMPT
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

  /**
   * Speichert die globalen KI-Einstellungen und erklärt die Übernahme aus Altbeständen für
   * erledigt.
   *
   * Das Setzen des Seed-Riegels gehört hierher, nicht nur in [seedAiSettingsOnce]: Auf einer
   * frischen Installation gibt es beim ersten Start keine Geschichte, aus der übernommen werden
   * könnte — der Riegel blieb also offen. Legte der Nutzer danach eine Geschichte an und stellte
   * ein anderes Einbettungsmodell ein, lief die Übernahme beim nächsten App-Start zum ersten Mal
   * und überschrieb genau diese Wahl mit den Altbestand-Vorgaben der Story-Zeile. Das Modell
   * sprang zurück, und weil [MemoryEngine.cosineSimilarity] bei abweichender Dimension bewusst 0
   * liefert, war das gesamte bis dahin aufgebaute Gedächtnis unauffindbar — ohne dass irgendetwas
   * sichtbar kaputt war.
   */
  fun setAiSettings(settings: AiSettings) {
    prefs.edit()
      .putBoolean(PREF_AI_SETTINGS_SEEDED, true)
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
    return true
  }

  /**
   * Der Schlüssel des Nutzers — und ausschließlich der.
   *
   * Ein Rückfall auf `BuildConfig.GEMINI_API_KEY` stand hier früher. Er hätte einen in die
   * `.env` eingetragenen Schlüssel als lesbare Zeichenkette ins APK gebracht und stillschweigend
   * benutzt, obwohl in den Einstellungen nichts steht.
   */
  fun getEffectiveApiKey(): String = getCustomApiKey()?.trim().orEmpty()
}
