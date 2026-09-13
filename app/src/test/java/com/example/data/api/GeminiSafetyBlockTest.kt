package com.example.data.api

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GeminiSafetyBlockTest {

  @Test
  fun `gueltige antwort ohne filter wirft keine exception`() {
    val json = JSONObject().apply {
      put("candidates", JSONArray().apply {
        put(JSONObject().apply {
          put("content", JSONObject().apply {
            put("parts", JSONArray().apply {
              put(JSONObject().apply { put("text", "Die Nacht ist still.") })
            })
          })
          put("finishReason", "STOP")
        })
      })
    }
    // Darf keine Exception werfen
    GeminiClient.checkContentSafetyBlock(json)
  }

  @Test
  fun `promptFeedback mit blockReason PROHIBITED_CONTENT wirft IllegalStateException mit klarer Meldung`() {
    val json = JSONObject().apply {
      put("promptFeedback", JSONObject().apply {
        put("blockReason", "PROHIBITED_CONTENT")
      })
    }

    val ex = assertThrows(IllegalStateException::class.java) {
      GeminiClient.checkContentSafetyBlock(json)
    }

    assertTrue("Sollte Inhaltsrichtlinien erwähnen", ex.message!!.contains("Inhaltsrichtlinien"))
    assertTrue("Sollte PROHIBITED_CONTENT erwähnen", ex.message!!.contains("PROHIBITED_CONTENT"))
  }

  @Test
  fun `candidate mit finishReason SAFETY wirft IllegalStateException`() {
    val json = JSONObject().apply {
      put("candidates", JSONArray().apply {
        put(JSONObject().apply {
          put("finishReason", "SAFETY")
        })
      })
    }

    val ex = assertThrows(IllegalStateException::class.java) {
      GeminiClient.checkContentSafetyBlock(json)
    }

    assertTrue("Sollte Sicherheitsfilter erwähnen", ex.message!!.contains("Sicherheitsfilter"))
    assertTrue("Sollte SAFETY erwähnen", ex.message!!.contains("SAFETY"))
  }
}
