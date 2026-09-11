package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CheckpointEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("StoryForge", appName)
  }

  @Test
  fun `checkpoint entity parsing is correct`() {
    val checkpoint = CheckpointEntity(
      storyId = 1L,
      turnNumber = 1,
      inGameTime = "Tag 1, 21:45 Uhr",
      location = "Alte Lagerhalle am Hafen",
      weather = "Kalter Regen",
      playerOutfit = "Durchnässte Lederjacke, grauer Hoodie",
      playerInventory = "[\"Taschenlampe\", \"Dietrich-Set\"]",
      playerCondition = "Leichte Schürfwunde",
      npcsJson = "[{\"name\": \"Elena\", \"outfit\": \"Dunkelblauer Wollmantel\", \"relationship_to_player\": \"Misstrauisch\", \"current_mood\": \"Angespannt\", \"status\": \"Anwesend\"}]",
      previousEventsSummary = "Elena und der Spieler flohen in die Lagerhalle.",
      rawStateJson = "{}"
    )

    val npcs = checkpoint.getNpcList()
    assertEquals(1, npcs.size)
    assertEquals("Elena", npcs[0].name)
    assertEquals("Dunkelblauer Wollmantel", npcs[0].outfit)
    assertEquals("Misstrauisch", npcs[0].relationshipToPlayer)
    assertEquals("Angespannt", npcs[0].currentMood)

    val inv = checkpoint.getInventoryList()
    assertEquals(2, inv.size)
    assertEquals("Taschenlampe", inv[0])
    assertEquals("Dietrich-Set", inv[1])
  }
}
