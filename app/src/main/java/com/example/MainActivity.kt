package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.ui.StoryScreen
import com.example.ui.StoryViewModel
import com.example.ui.theme.StoryForgeTheme

class MainActivity : ComponentActivity() {
  private val storyViewModel: StoryViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      StoryForgeTheme {
        StoryScreen(
          viewModel = storyViewModel,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}
