package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.GeminiClient
import com.example.data.model.StoryEntity
import com.example.ui.theme.AmberGoldContainer
import com.example.ui.theme.AmberGoldDark
import com.example.ui.theme.AmberGoldLight
import com.example.ui.theme.AmberGoldPrimary
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SlateDark600
import com.example.ui.theme.SlateDark700
import com.example.ui.theme.SlateDark800
import com.example.ui.theme.SlateDark900
import com.example.ui.theme.TextParchment
import com.example.ui.theme.TextParchmentFaint
import com.example.ui.theme.TextParchmentMuted
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
  story: StoryEntity,
  globalDefaultPrompt: String,
  customApiKey: String,
  onSaveStorySettings: (title: String, systemPrompt: String, model: String, temperature: Float, thinkingBudget: Int, adultContent: Boolean) -> Unit,
  onSaveGlobalDefaultPrompt: (String) -> Unit,
  onSaveApiKey: (String) -> Unit,
  onTestApiKey: suspend () -> Pair<Boolean, String>,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var title by remember(story) { mutableStateOf(story.title) }
  var storySystemPrompt by remember(story) { mutableStateOf(story.systemPrompt) }
  var globalPrompt by remember(globalDefaultPrompt) { mutableStateOf(globalDefaultPrompt) }
  var promptTabSelected by remember { mutableIntStateOf(if (story.systemPrompt.isNotBlank()) 0 else 1) }

  var selectedModel by remember(story) { mutableStateOf(story.selectedModel) }
  var temperature by remember(story) { mutableFloatStateOf(story.temperature) }
  var thinkingBudget by remember(story) { mutableIntStateOf(story.thinkingBudget) }
  var adultContent by remember(story) { mutableStateOf(story.adultContentEnabled) }

  var apiKeyInput by remember(customApiKey) { mutableStateOf(customApiKey) }
  var showApiKey by remember { mutableStateOf(false) }
  var isTestingKey by remember { mutableStateOf(false) }
  var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

  var modelDropdownExpanded by remember { mutableStateOf(false) }
  var thinkingDropdownExpanded by remember { mutableStateOf(false) }

  val availableModels = GeminiClient.AVAILABLE_MODELS
  val thinkingPresets = GeminiClient.THINKING_BUDGET_PRESETS

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(SlateDark900)
      .padding(horizontal = 20.dp, vertical = 16.dp)
      .verticalScroll(rememberScrollState())
  ) {
    // Top Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Tune,
          contentDescription = null,
          tint = AmberGoldPrimary,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Einstellungen",
          style = MaterialTheme.typography.titleLarge,
          color = TextParchment,
          fontFamily = FontFamily.Serif
        )
      }

      IconButton(
        onClick = onClose,
        modifier = Modifier.testTag("close_settings_button")
      ) {
        Icon(imageVector = Icons.Default.Close, contentDescription = "Schließen", tint = TextParchmentMuted)
      }
    }

    Text(
      text = "Konfiguriere KI-Modelle, Denkintensität, System-Prompts und Google AI Studio Key.",
      style = MaterialTheme.typography.bodySmall,
      color = TextParchmentMuted
    )

    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider(color = SlateDark700)
    Spacer(modifier = Modifier.height(16.dp))

    // 1. Google Gemini Account & API Key Section
    Text(
      text = "GEMINI-KONTO & API-SCHLÜSSEL",
      style = MaterialTheme.typography.labelSmall,
      color = AmberGoldPrimary,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(6.dp))

    Card(
      colors = CardDefaults.cardColors(containerColor = SlateDark800),
      shape = RoundedCornerShape(12.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, SlateDark600)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text(
          text = "Eigenes Gemini-Konto nutzen",
          style = MaterialTheme.typography.titleSmall,
          color = TextParchment,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Dein Google AI Studio Key bindet direkt dein Google Gemini Konto an. Volle Kostenkontrolle & Datenhoheit.",
          style = MaterialTheme.typography.bodySmall,
          color = TextParchmentMuted
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
          onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
            context.startActivity(intent)
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = SlateDark700,
            contentColor = AmberGoldPrimary
          ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("AI Studio öffnen / Key erstellen", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = apiKeyInput,
          onValueChange = { apiKeyInput = it },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("api_key_input"),
          placeholder = { Text("Gemini API Key einfügen...", color = TextParchmentFaint) },
          visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
          trailingIcon = {
            IconButton(onClick = { showApiKey = !showApiKey }) {
              Icon(
                imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = "Key einblenden/ausblenden",
                tint = TextParchmentMuted
              )
            }
          },
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SlateDark900,
            unfocusedContainerColor = SlateDark900,
            focusedBorderColor = AmberGoldPrimary,
            unfocusedBorderColor = SlateDark600,
            focusedTextColor = TextParchment,
            unfocusedTextColor = TextParchment
          ),
          shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = {
              onSaveApiKey(apiKeyInput)
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = SlateDark700,
              contentColor = TextParchment
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Speichern")
          }

          Button(
            onClick = {
              scope.launch {
                isTestingKey = true
                testResult = null
                onSaveApiKey(apiKeyInput)
                val res = onTestApiKey()
                testResult = res
                isTestingKey = false
              }
            },
            enabled = !isTestingKey,
            colors = ButtonDefaults.buttonColors(
              containerColor = AmberGoldContainer,
              contentColor = AmberGoldPrimary
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            if (isTestingKey) {
              CircularProgressIndicator(color = AmberGoldPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
              Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Testen")
            }
          }
        }

        val result = testResult
        if (result != null) {
          val (success, msg) = result
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            color = if (success) EmeraldSuccess.copy(alpha = 0.15f) else CrimsonDanger.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (success) EmeraldSuccess else CrimsonDanger),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = msg,
              style = MaterialTheme.typography.bodySmall,
              color = if (success) EmeraldSuccess else CrimsonDanger,
              modifier = Modifier.padding(10.dp)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 2. Aktuelle Gemini-Modelle
    Text(
      text = "AKTUELLES GEMINI-MODELL",
      style = MaterialTheme.typography.labelSmall,
      color = AmberGoldPrimary,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(6.dp))

    ExposedDropdownMenuBox(
      expanded = modelDropdownExpanded,
      onExpandedChange = { modelDropdownExpanded = it },
      modifier = Modifier.fillMaxWidth()
    ) {
      val currentModelLabel = availableModels.firstOrNull { it.first == selectedModel }?.second
        ?: selectedModel

      OutlinedTextField(
        value = currentModelLabel,
        onValueChange = {},
        readOnly = true,
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded) },
        modifier = Modifier
          .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
          .fillMaxWidth()
          .testTag("model_selector"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = SlateDark800,
          unfocusedContainerColor = SlateDark800,
          focusedBorderColor = AmberGoldPrimary,
          unfocusedBorderColor = SlateDark600,
          focusedTextColor = TextParchment,
          unfocusedTextColor = TextParchment
        ),
        shape = RoundedCornerShape(10.dp)
      )

      ExposedDropdownMenu(
        expanded = modelDropdownExpanded,
        onDismissRequest = { modelDropdownExpanded = false },
        modifier = Modifier.background(SlateDark800)
      ) {
        availableModels.forEach { (modelId, label) ->
          DropdownMenuItem(
            text = {
              Column {
                Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextParchment)
                Text(text = modelId, style = MaterialTheme.typography.labelSmall, color = TextParchmentFaint)
              }
            },
            onClick = {
              selectedModel = modelId
              modelDropdownExpanded = false
            },
            modifier = Modifier.testTag("model_option_$modelId")
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 3. Denkintensität (Thinking Budget)
    Text(
      text = "DENKINTENSITÄT (THINKING BUDGET)",
      style = MaterialTheme.typography.labelSmall,
      color = AmberGoldPrimary,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = "Gibt der KI Zeit für innere logische Konsistenz, NPC-Psychologie & Handlungsprüfung vor der Antwort.",
      style = MaterialTheme.typography.bodySmall,
      color = TextParchmentFaint
    )
    Spacer(modifier = Modifier.height(6.dp))

    ExposedDropdownMenuBox(
      expanded = thinkingDropdownExpanded,
      onExpandedChange = { thinkingDropdownExpanded = it },
      modifier = Modifier.fillMaxWidth()
    ) {
      val currentThinkingLabel = thinkingPresets.firstOrNull { it.first == thinkingBudget }?.second
        ?: "$thinkingBudget Token"

      OutlinedTextField(
        value = currentThinkingLabel,
        onValueChange = {},
        readOnly = true,
        leadingIcon = {
          Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = AmberGoldPrimary)
        },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = thinkingDropdownExpanded) },
        modifier = Modifier
          .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
          .fillMaxWidth()
          .testTag("thinking_budget_selector"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = SlateDark800,
          unfocusedContainerColor = SlateDark800,
          focusedBorderColor = AmberGoldPrimary,
          unfocusedBorderColor = SlateDark600,
          focusedTextColor = TextParchment,
          unfocusedTextColor = TextParchment
        ),
        shape = RoundedCornerShape(10.dp)
      )

      ExposedDropdownMenu(
        expanded = thinkingDropdownExpanded,
        onDismissRequest = { thinkingDropdownExpanded = false },
        modifier = Modifier.background(SlateDark800)
      ) {
        thinkingPresets.forEach { (budget, label) ->
          DropdownMenuItem(
            text = {
              Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextParchment)
            },
            onClick = {
              thinkingBudget = budget
              thinkingDropdownExpanded = false
            },
            modifier = Modifier.testTag("thinking_option_$budget")
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 4. Kreativität (Temperature)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "KREATIVITÄT (TEMPERATURE)",
        style = MaterialTheme.typography.labelSmall,
        color = AmberGoldPrimary,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = String.format("%.2f", temperature),
        style = MaterialTheme.typography.labelMedium,
        color = TextParchment,
        fontWeight = FontWeight.Bold
      )
    }
    Slider(
      value = temperature,
      onValueChange = { temperature = it },
      valueRange = 0.2f..1.5f,
      steps = 26,
      colors = SliderDefaults.colors(
        thumbColor = AmberGoldPrimary,
        activeTrackColor = AmberGoldPrimary,
        inactiveTrackColor = SlateDark700
      ),
      modifier = Modifier.testTag("temperature_slider")
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text("Präziser (0.2)", style = MaterialTheme.typography.labelSmall, color = TextParchmentFaint)
      Text("Kreativ & Variantenreich (1.5)", style = MaterialTheme.typography.labelSmall, color = TextParchmentFaint)
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 5. Mature / Adult Content (Full BLOCK_NONE across all 5 categories)
    Card(
      colors = CardDefaults.cardColors(containerColor = SlateDark800),
      shape = RoundedCornerShape(12.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, SlateDark600)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.fillMaxWidth(0.78f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = null,
              tint = if (adultContent) AmberGoldPrimary else TextParchmentMuted,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Mature / Adult-Content",
              style = MaterialTheme.typography.titleMedium,
              color = TextParchment
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Setzt ALLE 5 Gemini-Sicherheitsfilter auf BLOCK_NONE (keine Filterung für Erotik, viszerale Kämpfe oder düstere Themen).",
            style = MaterialTheme.typography.bodySmall,
            color = TextParchmentMuted
          )
        }

        Switch(
          checked = adultContent,
          onCheckedChange = { adultContent = it },
          colors = SwitchDefaults.colors(
            checkedThumbColor = SlateDark900,
            checkedTrackColor = AmberGoldPrimary,
            uncheckedThumbColor = TextParchmentMuted,
            uncheckedTrackColor = SlateDark700
          ),
          modifier = Modifier.testTag("adult_content_switch")
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 6. System-Prompt: Per-Story vs. Global Default
    Text(
      text = "SYSTEM-PROMPT (GAME MASTER REGELN)",
      style = MaterialTheme.typography.labelSmall,
      color = AmberGoldPrimary,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = "Jede Geschichte kann einen eigenen Prompt besitzen. Wenn das Feld leer ist, gilt der globale Standard-Prompt.",
      style = MaterialTheme.typography.bodySmall,
      color = TextParchmentFaint
    )
    Spacer(modifier = Modifier.height(8.dp))

    TabRow(
      selectedTabIndex = promptTabSelected,
      containerColor = SlateDark800,
      contentColor = AmberGoldPrimary,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[promptTabSelected]),
          color = AmberGoldPrimary
        )
      }
    ) {
      Tab(
        selected = promptTabSelected == 0,
        onClick = { promptTabSelected = 0 },
        text = {
          Text(
            text = "Dieser Chat (Individuell)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (promptTabSelected == 0) FontWeight.Bold else FontWeight.Normal
          )
        }
      )
      Tab(
        selected = promptTabSelected == 1,
        onClick = { promptTabSelected = 1 },
        text = {
          Text(
            text = "Globaler Standard",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (promptTabSelected == 1) FontWeight.Bold else FontWeight.Normal
          )
        }
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (promptTabSelected == 0) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (storySystemPrompt.isBlank()) "Aktuell: Standard-Prompt wird genutzt" else "Individueller Story-Prompt aktiv",
          style = MaterialTheme.typography.labelSmall,
          color = if (storySystemPrompt.isBlank()) TextParchmentMuted else AmberGoldPrimary
        )
        if (storySystemPrompt.isNotBlank()) {
          TextButton(onClick = { storySystemPrompt = "" }) {
            Text("Auf Standard zurücksetzen", fontSize = 11.sp, color = AmberGoldPrimary)
          }
        }
      }

      OutlinedTextField(
        value = storySystemPrompt,
        onValueChange = { storySystemPrompt = it },
        placeholder = { Text("Hier individuellen Prompt für diesen Chat eintragen (oder leer lassen für Standard)...", color = TextParchmentFaint) },
        modifier = Modifier
          .fillMaxWidth()
          .height(200.dp)
          .testTag("story_prompt_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = SlateDark800,
          unfocusedContainerColor = SlateDark800,
          focusedBorderColor = AmberGoldPrimary,
          unfocusedBorderColor = SlateDark600,
          focusedTextColor = TextParchment,
          unfocusedTextColor = TextParchment
        ),
        shape = RoundedCornerShape(10.dp),
        textStyle = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp)
      )
    } else {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Standard-Prompt für alle Chats ohne eigenen Prompt",
          style = MaterialTheme.typography.labelSmall,
          color = TextParchmentMuted
        )
        TextButton(onClick = { globalPrompt = GeminiClient.DEFAULT_SYSTEM_PROMPT }) {
          Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("Werkseinstellung", fontSize = 11.sp, color = AmberGoldPrimary)
        }
      }

      OutlinedTextField(
        value = globalPrompt,
        onValueChange = { globalPrompt = it },
        modifier = Modifier
          .fillMaxWidth()
          .height(200.dp)
          .testTag("global_prompt_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = SlateDark800,
          unfocusedContainerColor = SlateDark800,
          focusedBorderColor = AmberGoldPrimary,
          unfocusedBorderColor = SlateDark600,
          focusedTextColor = TextParchment,
          unfocusedTextColor = TextParchment
        ),
        shape = RoundedCornerShape(10.dp),
        textStyle = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp)
      )
    }

    Spacer(modifier = Modifier.height(28.dp))

    // Save All Button
    Button(
      onClick = {
        onSaveStorySettings(title, storySystemPrompt, selectedModel, temperature, thinkingBudget, adultContent)
        onSaveGlobalDefaultPrompt(globalPrompt)
        onSaveApiKey(apiKeyInput)
        onClose()
      },
      colors = ButtonDefaults.buttonColors(
        containerColor = AmberGoldPrimary,
        contentColor = SlateDark900
      ),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("save_settings_button")
    ) {
      Text(
        text = "Einstellungen anwenden & speichern",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold
      )
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}
