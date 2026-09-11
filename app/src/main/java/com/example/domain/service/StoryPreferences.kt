package com.example.domain.service

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.api.GeminiClient

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
