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
import androidx.compose.material.icons.filled.Info
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
import com.example.ui.dna.DnaButton
import com.example.ui.dna.DnaButtonVariant
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
import com.example.data.model.GeminiModelInfo
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
  availableModels: List<GeminiModelInfo>,
  isFetchingModels: Boolean,
  onRefreshModels: () -> Unit,
  onSaveStorySettings: (
    title: String,
    systemPrompt: String,
    model: String,
    temperature: Float,
    supportsTemperature: Boolean,
    thinkingLevel: String,
    thinkingBudget: Int,
    adultContent: Boolean
  ) -> Unit,
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
  val currentModelInfo = availableModels.firstOrNull { it.id == selectedModel }
    ?: GeminiModelInfo(
      id = selectedModel,
      displayName = selectedModel,
      description = "",
      supportsTemperature = story.supportsTemperature,
      defaultTemperature = story.temperature,
      isThinkingModel = true,
      usesThinkingLevel = selectedModel.contains("3.") || selectedModel.contains("gemini-3")
    )

  var thinkingLevel by remember(story) { mutableStateOf(story.thinkingLevel) }
  var thinkingBudget by remember(story) { mutableIntStateOf(story.thinkingBudget) }
  var temperature by remember(story) { mutableFloatStateOf(story.temperature) }
  var supportsTemperature by remember(story, currentModelInfo) {
    mutableStateOf(currentModelInfo.supportsTemperature)
  }
  var adultContent by remember(story) { mutableStateOf(story.adultContentEnabled) }

  var apiKeyInput by remember(customApiKey) { mutableStateOf(customApiKey) }
  var showApiKey by remember { mutableStateOf(false) }
  var isTestingKey by remember { mutableStateOf(false) }
  var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

  var modelDropdownExpanded by remember { mutableStateOf(false) }
  var thinkingDropdownExpanded by remember { mutableStateOf(false) }

  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(SlateDark900)
      .padding(horizontal = 20.dp)
      .verticalScroll(scrollState)
      .testTag("settings_sheet")
  ) {
    Spacer(modifier = Modifier.height(12.dp))

    // Handle indicator
    Box(
      modifier = Modifier
        .align(Alignment.CenterHorizontally)
        .width(40.dp)
        .height(4.dp)
        .background(SlateDark600, RoundedCornerShape(2.dp))
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Header
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
          text = "Spieleinstellungen & KI-Regeln",
          style = MaterialTheme.typography.titleMedium,
          fontFamily = FontFamily.Serif,
          fontWeight = FontWeight.Bold,
          color = TextParchment
        )
      }

      IconButton(
        onClick = onClose,
        modifier = Modifier.testTag("close_settings_button")
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Schließen",
          tint = TextParchmentMuted
        )
      }
    }

    Text(
      text = "Passe das Google Gemini Modell, die Denkstufen und die Regieanweisungen für dieses Abenteuer an.",
      style = MaterialTheme.typography.bodySmall,
      color = TextParchmentMuted
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Story Title
    Text(
      text = "TITEL DER GESCHICHTE",
      style = MaterialTheme.typography.labelSmall,
      color = AmberGoldPrimary,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
      value = title,
      onValueChange = { title = it },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("story_title_input"),
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

    Spacer(modifier = Modifier.height(20.dp))

    // 1. Google Gemini API-Schlüssel
    Card(
      colors = CardDefaults.cardColors(containerColor = SlateDark800),
      shape = RoundedCornerShape(12.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, SlateDark600)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Key,
              contentDescription = null,
              tint = AmberGoldPrimary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Google Gemini API-Key",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = TextParchment
            )
          }

          TextButton(
            onClick = {
              val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
              context.startActivity(intent)
            }
          ) {
            Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp), tint = AmberGoldLight)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Key holen", style = MaterialTheme.typography.labelSmall, color = AmberGoldLight)
          }
        }

        Text(
          text = "Direkter Zugriff auf die offiziellen Google Gemini Modelle (inkl. 3.8 Flash, 3.1 Pro). Dein Key verbleibt sicher lokal auf diesem Gerät.",
          style = MaterialTheme.typography.bodySmall,
          color = TextParchmentMuted
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = apiKeyInput,
          onValueChange = { apiKeyInput = it },
          placeholder = { Text("AIzaSy...", color = TextParchmentFaint) },
          visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
          trailingIcon = {
            IconButton(onClick = { showApiKey = !showApiKey }) {
              Icon(
                imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = null,
                tint = TextParchmentMuted
              )
            }
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("api_key_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SlateDark900,
            unfocusedContainerColor = SlateDark900,
            focusedBorderColor = AmberGoldPrimary,
            unfocusedBorderColor = SlateDark700,
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
          DnaButton(
            text = "Speichern",
            onClick = { onSaveApiKey(apiKeyInput) },
            icon = Icons.Default.Key,
            variant = DnaButtonVariant.SECONDARY,
            modifier = Modifier.weight(1f),
            testTag = "save_api_key_btn"
          )

          DnaButton(
            text = "Testen",
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
            loading = isTestingKey,
            icon = Icons.Default.Check,
            variant = DnaButtonVariant.PRIMARY,
            modifier = Modifier.weight(1f),
            testTag = "test_api_key_btn"
          )
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

    // 2. Aktuelle Gemini-Modelle (Dynamisch von Google abrufen)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "AKTUELLES GOOGLE GEMINI MODELL",
        style = MaterialTheme.typography.labelSmall,
        color = AmberGoldPrimary,
        fontWeight = FontWeight.Bold
      )

      TextButton(
        onClick = onRefreshModels,
        enabled = !isFetchingModels,
        modifier = Modifier.testTag("refresh_models_button")
      ) {
        if (isFetchingModels) {
          CircularProgressIndicator(color = AmberGoldPrimary, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
        } else {
          Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = AmberGoldLight, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text("Von Google abrufen", style = MaterialTheme.typography.labelSmall, color = AmberGoldLight)
      }
    }
    Text(
      text = "Aktuelle Modelle werden live aus der Google Gemini API geladen. Neue Modelle wie Gemini 3.8 Flash stehen sofort bereit.",
      style = MaterialTheme.typography.bodySmall,
      color = TextParchmentFaint
    )
    Spacer(modifier = Modifier.height(6.dp))

    val modelOptions = remember(availableModels) {
      availableModels.map { modelInfo ->
        com.example.ui.dna.DnaDropdownOption(
          value = modelInfo.id,
          label = modelInfo.displayName,
          hint = "${modelInfo.id} • ${if (modelInfo.usesThinkingLevel) "Denkstufen" else "Token-Budget"}"
        )
      }
    }

    com.example.ui.dna.DnaDropdown(
      options = modelOptions,
      selectedValue = selectedModel,
      onOptionSelected = { modelId ->
        selectedModel = modelId
        availableModels.find { it.id == modelId }?.let { info ->
          supportsTemperature = info.supportsTemperature
          if (info.supportsTemperature) {
            temperature = info.defaultTemperature
          }
        }
      },
      modifier = Modifier.fillMaxWidth().testTag("model_selector")
    )

    Spacer(modifier = Modifier.height(20.dp))

    // 3. Denkstufe (Thinking Level) oder Token-Budget je nach Modelltyp
    if (currentModelInfo.usesThinkingLevel) {
      // Gemini 3.x+ Modelle: Denkstufen (Minimal, Niedrig, Mittel, Hoch)
      Text(
        text = "DENKSTUFE (REASONING EFFORT)",
        style = MaterialTheme.typography.labelSmall,
        color = AmberGoldPrimary,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "Moderne Modelle (wie Gemini 3.8 Flash) nutzen abgestufte Denkstufen statt starrer Token-Budgets für NPC-Logik & Konsistenzprüfung.",
        style = MaterialTheme.typography.bodySmall,
        color = TextParchmentFaint
      )
      Spacer(modifier = Modifier.height(8.dp))

      val thinkingOptions = remember {
        listOf(
          com.example.ui.dna.DnaDropdownOption("MINIMAL", "Minimal", "Blitzschnell, minimale Denkzeit"),
          com.example.ui.dna.DnaDropdownOption("LOW", "Niedrig", "Schnelle Reflexion, geringe Latenz"),
          com.example.ui.dna.DnaDropdownOption("MEDIUM", "Mittel (Standard)", "Ausgewogene Psychologie"),
          com.example.ui.dna.DnaDropdownOption("HIGH", "Hoch", "Tiefgründige Reflexion & maximale Konsistenz")
        )
      }

      com.example.ui.dna.DnaDropdown(
        options = thinkingOptions,
        selectedValue = thinkingLevel,
        onOptionSelected = { thinkingLevel = it },
        icon = Icons.Default.Psychology,
        modifier = Modifier.fillMaxWidth().testTag("thinking_level_selector")
      )
    } else {
      // Legacy Token-Budget für ältere Modelle (z. B. Gemini 2.5 Flash)
      Text(
        text = "DENKINTENSITÄT (THINKING BUDGET IN TOKEN)",
        style = MaterialTheme.typography.labelSmall,
        color = AmberGoldPrimary,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "Tokenbasiertes Denkzeit-Budget für ältere Modellserien.",
        style = MaterialTheme.typography.bodySmall,
        color = TextParchmentFaint
      )
      Spacer(modifier = Modifier.height(6.dp))

      val thinkingPresets = remember {
        GeminiClient.THINKING_BUDGET_PRESETS.map { (budget, label) ->
          com.example.ui.dna.DnaDropdownOption(
            value = budget.toString(),
            label = label,
            hint = "$budget Token"
          )
        }
      }

      com.example.ui.dna.DnaDropdown(
        options = thinkingPresets,
        selectedValue = thinkingBudget.toString(),
        onOptionSelected = { budgetStr ->
          thinkingBudget = budgetStr.toIntOrNull() ?: 2048
        },
        icon = Icons.Default.Psychology,
        modifier = Modifier.fillMaxWidth().testTag("thinking_budget_selector")
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 4. Kreativität (Temperature) oder Hinweis bei nicht-unterstützten Modellen
    if (supportsTemperature) {
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
    } else {
      Card(
        colors = CardDefaults.cardColors(containerColor = SlateDark800),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateDark700)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = AmberGoldLight,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Temperatur: Fest vorgegeben",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = TextParchment
            )
            Text(
              text = "Das Modell $selectedModel arbeitet mit fest verankerten Sampling-Parametern. Manuelle Temperatur-Steuerung wird nicht unterstützt.",
              style = MaterialTheme.typography.bodySmall,
              color = TextParchmentMuted
            )
          }
        }
      }
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
              text = "Mature / Adult Content Filter",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = TextParchment
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = if (adultContent) {
              "BLOCK_NONE aktiv (Sexuelle Inhalte, Gewalt, dunkle Szenen werden nicht ausgeblendet)."
            } else {
              "Standard-Sicherheitsfilter von Google aktiv."
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (adultContent) AmberGoldLight else TextParchmentMuted
          )
        }

        Switch(
          checked = adultContent,
          onCheckedChange = { adultContent = it },
          colors = SwitchDefaults.colors(
            checkedThumbColor = AmberGoldPrimary,
            checkedTrackColor = AmberGoldDark,
            uncheckedThumbColor = TextParchmentMuted,
            uncheckedTrackColor = SlateDark700
          ),
          modifier = Modifier.testTag("adult_content_switch")
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // 6. System-Prompt Regieanweisungen
    Text(
      text = "REGIEANWEISUNG / SYSTEM-PROMPT",
      style = MaterialTheme.typography.labelSmall,
      color = AmberGoldPrimary,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = "Definiere das Fundament der Spielwelt, Sprachregeln, Verbot von Kosenamen und das autonome Handeln der Charaktere.",
      style = MaterialTheme.typography.bodySmall,
      color = TextParchmentMuted
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
            text = "Diese Geschichte",
            style = MaterialTheme.typography.labelMedium,
            color = if (promptTabSelected == 0) AmberGoldPrimary else TextParchmentMuted
          )
        },
        modifier = Modifier.testTag("tab_story_prompt")
      )
      Tab(
        selected = promptTabSelected == 1,
        onClick = { promptTabSelected = 1 },
        text = {
          Text(
            text = "Globaler Standard",
            style = MaterialTheme.typography.labelMedium,
            color = if (promptTabSelected == 1) AmberGoldPrimary else TextParchmentMuted
          )
        },
        modifier = Modifier.testTag("tab_global_prompt")
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (promptTabSelected == 0) {
      Text(
        text = "Individueller Prompt für diese Geschichte (überschreibt den globalen Standard):",
        style = MaterialTheme.typography.bodySmall,
        color = TextParchmentFaint
      )
      Spacer(modifier = Modifier.height(6.dp))
      OutlinedTextField(
        value = storySystemPrompt,
        onValueChange = { storySystemPrompt = it },
        placeholder = { Text("Leer lassen, um den globalen Standard-Prompt zu verwenden...", color = TextParchmentFaint) },
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
      Text(
        text = "Globaler Standard-Prompt (gilt für alle neuen Geschichten):",
        style = MaterialTheme.typography.bodySmall,
        color = TextParchmentFaint
      )
      Spacer(modifier = Modifier.height(6.dp))
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
    DnaButton(
      text = "Einstellungen anwenden & speichern",
      onClick = {
        onSaveStorySettings(
          title,
          storySystemPrompt,
          selectedModel,
          temperature,
          supportsTemperature,
          thinkingLevel,
          thinkingBudget,
          adultContent
        )
        onSaveGlobalDefaultPrompt(globalPrompt)
        onSaveApiKey(apiKeyInput)
        onClose()
      },
      variant = DnaButtonVariant.PRIMARY,
      fullWidth = true,
      testTag = "save_settings_button"
    )

    Spacer(modifier = Modifier.height(24.dp))
  }
}
