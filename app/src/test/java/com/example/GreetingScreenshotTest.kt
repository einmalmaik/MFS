package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.MessageEntity
import com.example.ui.components.StoryMessageItem
import com.example.ui.theme.StoryForgeTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun story_message_screenshot() {
    val sampleMessage = MessageEntity(
      id = 1L,
      storyId = 1L,
      sender = "model",
      content = "Der Regen schlägt unbarmherzig gegen die verrosteten Wellblechwände der Lagerhalle. Elena steht im Halbdunkel.",
      inGameTimeTag = "Tag 1, 21:30 Uhr"
    )

    composeTestRule.setContent {
      StoryForgeTheme {
        StoryMessageItem(
          message = sampleMessage,
          onRewind = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
