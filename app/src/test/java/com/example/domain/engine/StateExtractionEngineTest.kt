package com.example.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.data.api.GeminiClient
import com.example.data.db.StoryDao

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StateExtractionEngineTest {
  
  // We'll use simple reflection to just test the method directly instead of relying on Mockito 
  // since Mockito is not in the dependencies.
  @Test
  fun `test deterministic time progression without skips`() {
    val result = invokeTimeProgression(
      "Tag 1, 20:00 Uhr",
      "Tag 1, 20:00 Uhr",
      "Ich gehe durch die Tür.",
      "Du gehst durch die Tür in den nächsten Raum."
    )
    assertEquals("Tag 1, 20:00 Uhr", result)
  }

  @Test
  fun `test deterministic time progression with explicit days skip in action`() {
    val result = invokeTimeProgression(
      "Tag 1, 20:00 Uhr",
      "Tag 1, 20:00 Uhr", // Simulation: Model failed to advance time
      "Es vergehen zwei Tage.",
      "Zwei Tage verstreichen ohne weitere Vorkommnisse."
    )
    // The engine should auto-correct to target day (Day 3) and default to 20:00
    assertEquals("Tag 3, 20:00 Uhr", result)
  }
  
  @Test
  fun `test deterministic time progression with next morning skip`() {
    val result = invokeTimeProgression(
      "Tag 1, 22:00 Uhr",
      "Tag 1, 08:00 Uhr", // Model got the time right but missed the day switch
      "Wir schlafen bis zum nächsten Morgen.",
      "Die Nacht vergeht ruhig. Am nächsten Morgen wacht ihr auf."
    )
    assertEquals("Tag 2, 08:00 Uhr", result)
  }

  @Test
  fun `test model correctly advances time itself`() {
    val result = invokeTimeProgression(
      "Tag 2, 14:00 Uhr",
      "Tag 5, 12:00 Uhr", // Model correctly skipped 3 days
      "Drei Tage später treffen wir uns wieder.",
      "Nach drei Tagen am vereinbarten Ort..."
    )
    assertEquals("Tag 5, 12:00 Uhr", result)
  }

  private fun invokeTimeProgression(
    previousTime: String,
    extractedTime: String,
    userAction: String,
    modelResponse: String
  ): String {
    return StateExtractionEngine.computeDeterministicTimeProgression(
      previousTime, extractedTime, userAction, modelResponse
    )
  }
}

