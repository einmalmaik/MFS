package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiSettings
import com.example.data.model.GeminiModelInfo
import com.example.ui.dna.DnaBadge
import com.example.ui.dna.DnaBadgeTone
import com.example.ui.dna.DnaButton
import com.example.ui.dna.DnaButtonVariant
import com.example.ui.dna.DnaColors
import com.example.ui.dna.DnaDropdown
import com.example.ui.dna.DnaDropdownOption
import com.example.ui.dna.DnaFloatNumberStepper
import com.example.ui.dna.DnaNumberStepper
import com.example.ui.dna.DnaTypography
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
  aiSettings: AiSettings,
  globalDefaultPrompt: String,
  customApiKey: String,
  availableModels: List<GeminiModelInfo>,
  availableEmbeddingModels: List<GeminiModelInfo> = emptyList(),
  availableTranscriptionModels: List<GeminiModelInfo> = emptyList(),
  isFetchingModels: Boolean,
  modelCatalogIsLive: Boolean = false,
  onRefreshModels: () -> Unit,
  onSaveAiSettings: (AiSettings) -> Unit,
  onSaveGlobalDefaultPrompt: (String) -> Unit,
  onSaveApiKey: (String) -> Unit,
  onTestApiKey: suspend () -> Pair<Boolean, String>,
  updateCheckEnabled: Boolean,
  installedVersion: String,
  onSetUpdateCheck: (Boolean) -> Unit,
  onCheckForUpdate: () -> Unit,
  onOpenPrivacy: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var globalPrompt by remember(globalDefaultPrompt) { mutableStateOf(globalDefaultPrompt) }

  var selectedModel by remember(aiSettings) { mutableStateOf(aiSettings.chatModel) }
  var selectedEmbeddingModel by remember(aiSettings) { mutableStateOf(aiSettings.embeddingModel) }
  var selectedTranscriptionModel by remember(aiSettings) { mutableStateOf(aiSettings.transcriptionModel) }
  val currentModelInfo = remember(selectedModel, availableModels) {
    availableModels.find { it.id == selectedModel } ?: GeminiModelInfo(
      id = selectedModel,
      displayName = selectedModel,
      description = "",
      supportsTemperature = true,
      usesThinkingLevel = true
    )
  }

  var supportsTemperature by remember(aiSettings, currentModelInfo) {
    mutableStateOf(currentModelInfo.supportsTemperature)
  }
  var temperature by remember(aiSettings) { mutableFloatStateOf(aiSettings.temperature) }
  var thinkingLevel by remember(aiSettings) { mutableStateOf(aiSettings.thinkingLevel) }
  var thinkingBudget by remember(aiSettings) {
    mutableIntStateOf(if (aiSettings.thinkingBudget > 0) aiSettings.thinkingBudget else 2048)
  }
  var adultContent by remember(aiSettings) { mutableStateOf(aiSettings.adultContentEnabled) }

  var apiKeyInput by remember(customApiKey) { mutableStateOf(customApiKey) }
  var showApiKey by remember { mutableStateOf(false) }
  var isTestingKey by remember { mutableStateOf(false) }
  var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(DnaColors.Surface)
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
        .background(DnaColors.Border, RoundedCornerShape(2.dp))
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
          tint = DnaColors.Primary,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Einstellungen",
          style = MaterialTheme.typography.titleMedium,
          fontFamily = DnaTypography.ManropeFamily,
          fontWeight = FontWeight.Bold,
          color = DnaColors.OnSurface
        )
      }

      IconButton(
        onClick = onClose,
        modifier = Modifier.testTag("close_settings_button")
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Schließen",
          tint = DnaColors.OnSurfaceVariant
        )
      }
    }

    Text(
      text = "Gilt für alle Geschichten.",
      style = MaterialTheme.typography.bodySmall,
      fontFamily = DnaTypography.InterFamily,
      color = DnaColors.OnSurfaceVariant
    )

    Spacer(modifier = Modifier.height(20.dp))

    // 1. Google Gemini API-Schlüssel
    Card(
      colors = CardDefaults.cardColors(containerColor = DnaColors.SurfaceContainer),
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, DnaColors.Border)
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
              tint = DnaColors.Primary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "API-Key",
              style = MaterialTheme.typography.labelLarge,
              fontFamily = DnaTypography.ManropeFamily,
              fontWeight = FontWeight.Bold,
              color = DnaColors.OnSurface
            )
          }

          TextButton(
            onClick = {
              val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
              context.startActivity(intent)
            }
          ) {
            Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp), tint = DnaColors.Secondary)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Key holen", style = MaterialTheme.typography.labelSmall, fontFamily = DnaTypography.InterFamily, color = DnaColors.Secondary)
          }
        }

        Text(
          text = "Bleibt lokal auf diesem Gerät.",
          style = MaterialTheme.typography.bodySmall,
          fontFamily = DnaTypography.InterFamily,
          color = DnaColors.OnSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = apiKeyInput,
          onValueChange = { apiKeyInput = it },
          placeholder = { Text("AIzaSy…", color = DnaColors.MutedForeground) },
          visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
          trailingIcon = {
            IconButton(onClick = { showApiKey = !showApiKey }) {
              Icon(
                imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = null,
                tint = DnaColors.OnSurfaceVariant
              )
            }
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("api_key_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DnaColors.SurfaceContainerLow,
            unfocusedContainerColor = DnaColors.SurfaceContainerLow,
            focusedBorderColor = DnaColors.Primary,
            unfocusedBorderColor = DnaColors.Border,
            focusedTextColor = DnaColors.OnSurface,
            unfocusedTextColor = DnaColors.OnSurface
          ),
          shape = RoundedCornerShape(10.dp)
        )

        // Nur warnen, wenn das Format wirklich zu keinem bekannten Schlüssel passt.
        // Der frühere Absatz zu 'AQ.'-Schlüsseln stand bei jedem Öffnen da, obwohl diese
        // Schlüssel funktionieren — eine Dauerwarnung, die man irgendwann überliest.
        val trimmedKey = apiKeyInput.trim()
        val keyHint: String? = when {
          trimmedKey.isBlank() -> null
          trimmedKey.startsWith("AQ.") || trimmedKey.startsWith("AIzaSy") -> null
          else -> "Unbekanntes Schlüsselformat. Erwartet wird 'AIzaSy…' oder 'AQ.…'."
        }
        if (keyHint != null) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = keyHint,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = DnaTypography.InterFamily,
            color = DnaColors.StatusWarning,
            fontSize = 11.sp
          )
        }

        if (!modelCatalogIsLive) {
          Spacer(modifier = Modifier.height(8.dp))
          DnaBadge(
            text = "Modellliste offline",
            tone = DnaBadgeTone.AMBER
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

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
            color = if (success) DnaColors.StatusSuccess.copy(alpha = 0.15f) else DnaColors.StatusDestructive.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, if (success) DnaColors.StatusSuccess else DnaColors.StatusDestructive),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = msg,
              style = MaterialTheme.typography.bodySmall,
              fontFamily = DnaTypography.InterFamily,
              color = if (success) DnaColors.StatusSuccess else DnaColors.StatusDestructive,
              modifier = Modifier.padding(10.dp)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 2. Aktuelle Gemini-Modelle
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "ERZÄHLMODELL",
        style = MaterialTheme.typography.labelSmall,
        fontFamily = DnaTypography.InterFamily,
        color = DnaColors.Primary,
        fontWeight = FontWeight.Bold
      )

      TextButton(
        onClick = onRefreshModels,
        enabled = !isFetchingModels,
        modifier = Modifier.testTag("refresh_models_button")
      ) {
        if (isFetchingModels) {
          CircularProgressIndicator(color = DnaColors.Primary, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
        } else {
          Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = DnaColors.Secondary, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text("Von Google abrufen", style = MaterialTheme.typography.labelSmall, fontFamily = DnaTypography.InterFamily, color = DnaColors.Secondary)
      }
    }
    Text(
      text = "Live aus deinem Schlüssel geladen.",
      style = MaterialTheme.typography.bodySmall,
      fontFamily = DnaTypography.InterFamily,
      color = DnaColors.MutedForeground
    )
    Spacer(modifier = Modifier.height(6.dp))

    val modelOptions = remember(availableModels) {
      availableModels.map { modelInfo ->
        DnaDropdownOption(
          value = modelInfo.id,
          label = modelInfo.displayName,
          hint = "${modelInfo.id} • ${if (modelInfo.usesThinkingLevel) "Denkstufen" else "Token-Budget"}"
        )
      }
    }

    DnaDropdown(
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

    Text(
      text = "GEDÄCHTNIS",
      style = MaterialTheme.typography.labelSmall,
      fontFamily = DnaTypography.InterFamily,
      color = DnaColors.Primary,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = "Ein Wechsel macht bisherige Erinnerungen unlesbar.",
      style = MaterialTheme.typography.bodySmall,
      fontFamily = DnaTypography.InterFamily,
      color = DnaColors.MutedForeground
    )
    Spacer(modifier = Modifier.height(6.dp))

    // Ausschließlich die Modelle, die der Schlüssel wirklich erreichen kann. Eine fest
    // verdrahtete Ersatzliste hätte hier jahrelang tote Ids angeboten.
    val embeddingModelOptions = remember(availableEmbeddingModels) {
      availableEmbeddingModels.map {
        DnaDropdownOption(value = it.id, label = it.displayName, hint = it.id)
      }
    }
    DnaDropdown(
      options = embeddingModelOptions,
      selectedValue = selectedEmbeddingModel,
      onOptionSelected = { selectedEmbeddingModel = it },
      modifier = Modifier.fillMaxWidth().testTag("embedding_model_selector")
    )

    Spacer(modifier = Modifier.height(20.dp))

    Text(
      text = "SPRACHEINGABE",
      style = MaterialTheme.typography.labelSmall,
      fontFamily = DnaTypography.InterFamily,
      color = DnaColors.Primary,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = "Wandelt deine Sprachaufnahmen in Text um.",
      style = MaterialTheme.typography.bodySmall,
      fontFamily = DnaTypography.InterFamily,
      color = DnaColors.MutedForeground
    )
    Spacer(modifier = Modifier.height(6.dp))

    val transcriptionModelOptions = remember(availableTranscriptionModels) {
      availableTranscriptionModels.map {
        DnaDropdownOption(value = it.id, label = it.displayName, hint = it.id)
      }
    }
    DnaDropdown(
      options = transcriptionModelOptions,
      selectedValue = selectedTranscriptionModel,
      onOptionSelected = { selectedTranscriptionModel = it },
      modifier = Modifier.fillMaxWidth().testTag("transcription_model_selector")
    )

    Spacer(modifier = Modifier.height(20.dp))

    // 3. Denkstufe (Thinking Level) oder Token-Budget je nach Modelltyp
    if (!currentModelInfo.isThinkingModel) {
      Card(
        colors = CardDefaults.cardColors(containerColor = DnaColors.SurfaceContainer),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, DnaColors.Border)
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
            tint = DnaColors.Secondary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Denkstufen: Nicht unterstützt",
              style = MaterialTheme.typography.labelMedium,
              fontFamily = DnaTypography.InterFamily,
              fontWeight = FontWeight.Bold,
              color = DnaColors.OnSurface
            )
            Text(
              text = "$selectedModel antwortet direkt, ohne Denkschritte.",
              style = MaterialTheme.typography.bodySmall,
              fontFamily = DnaTypography.InterFamily,
              color = DnaColors.OnSurfaceVariant
            )
          }
        }
      }
    } else if (currentModelInfo.usesThinkingLevel) {
      Text(
        text = "DENKSTUFE",
        style = MaterialTheme.typography.labelSmall,
        fontFamily = DnaTypography.InterFamily,
        color = DnaColors.Primary,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "Wie gründlich das Modell vor der Antwort nachdenkt.",
        style = MaterialTheme.typography.bodySmall,
        fontFamily = DnaTypography.InterFamily,
        color = DnaColors.MutedForeground
      )
      Spacer(modifier = Modifier.height(8.dp))

      val thinkingOptions = remember(currentModelInfo.supportedThinkingLevels) {
        val levels = if (currentModelInfo.supportedThinkingLevels.isNotEmpty()) {
          currentModelInfo.supportedThinkingLevels
        } else {
          listOf("LOW", "MEDIUM", "HIGH")
        }
        levels.map { lvl ->
          when (lvl.uppercase()) {
            "LOW" -> DnaDropdownOption("LOW", "Niedrig", "Schnelle Reflexion, minimale Latenz")
            "HIGH" -> DnaDropdownOption("HIGH", "Hoch", "Tiefgründige Reflexion & Konsistenz")
            else -> DnaDropdownOption("MEDIUM", "Mittel (Standard)", "Ausgewogene Psychologie")
          }
        } + listOf(DnaDropdownOption("OFF", "Deaktiviert", "Keine Denkphase"))
      }

      DnaDropdown(
        options = thinkingOptions,
        selectedValue = thinkingLevel,
        onOptionSelected = { thinkingLevel = it },
        icon = Icons.Default.Psychology,
        modifier = Modifier.fillMaxWidth().testTag("thinking_level_selector")
      )
    } else {
      DnaNumberStepper(
        value = thinkingBudget,
        onValueChange = { thinkingBudget = it },
        min = 0,
        max = 8192,
        step = 512,
        label = "DENKBUDGET",
        unit = "Tokens",
        modifier = Modifier.fillMaxWidth().testTag("thinking_budget_stepper")
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 4. Kreativität (Temperature) mit DnaFloatNumberStepper
    if (supportsTemperature) {
      DnaFloatNumberStepper(
        value = temperature,
        onValueChange = { temperature = it },
        min = 0.2f,
        max = 1.5f,
        step = 0.1f,
        label = "KREATIVITÄT",
        modifier = Modifier.fillMaxWidth().testTag("temperature_stepper")
      )
      Spacer(modifier = Modifier.height(4.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text("0.2: Präzise & linientreu", style = MaterialTheme.typography.labelSmall, fontFamily = DnaTypography.InterFamily, color = DnaColors.MutedForeground)
        Text("1.5: Kreativ & überraschend", style = MaterialTheme.typography.labelSmall, fontFamily = DnaTypography.InterFamily, color = DnaColors.MutedForeground)
      }
    } else {
      Card(
        colors = CardDefaults.cardColors(containerColor = DnaColors.SurfaceContainer),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, DnaColors.Border)
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
            tint = DnaColors.Primary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Temperatur: Fest vorgegeben",
              style = MaterialTheme.typography.labelMedium,
              fontFamily = DnaTypography.InterFamily,
              fontWeight = FontWeight.Bold,
              color = DnaColors.OnSurface
            )
            Text(
              text = "$selectedModel hat eine feste Kreativität.",
              style = MaterialTheme.typography.bodySmall,
              fontFamily = DnaTypography.InterFamily,
              color = DnaColors.OnSurfaceVariant
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 5. Mature / Adult Content
    Card(
      colors = CardDefaults.cardColors(containerColor = DnaColors.SurfaceContainer),
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, DnaColors.Border)
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
              tint = if (adultContent) DnaColors.Primary else DnaColors.OnSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Adult Content",
              style = MaterialTheme.typography.labelLarge,
              fontFamily = DnaTypography.ManropeFamily,
              fontWeight = FontWeight.Bold,
              color = DnaColors.OnSurface
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = if (adultContent) {
              "Nichts wird ausgeblendet."
            } else {
              "Googles Standardfilter sind aktiv."
            },
            style = MaterialTheme.typography.bodySmall,
            fontFamily = DnaTypography.InterFamily,
            color = if (adultContent) DnaColors.OnPrimaryContainer else DnaColors.OnSurfaceVariant
          )
        }

        Switch(
          checked = adultContent,
          onCheckedChange = { adultContent = it },
          colors = SwitchDefaults.colors(
            checkedThumbColor = DnaColors.Primary,
            checkedTrackColor = DnaColors.PrimaryContainer,
            uncheckedThumbColor = DnaColors.TextDisabled,
            uncheckedTrackColor = DnaColors.SurfaceContainerLow
          ),
          modifier = Modifier.testTag("adult_content_switch")
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // 5b. Aktualisierung
    Text(
      text = "AKTUALISIERUNG",
      style = MaterialTheme.typography.labelSmall,
      fontFamily = DnaTypography.InterFamily,
      color = DnaColors.Primary,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))

    Surface(
      shape = RoundedCornerShape(12.dp),
      color = DnaColors.SurfaceContainer,
      border = BorderStroke(1.dp, DnaColors.Border),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.fillMaxWidth(0.78f)) {
            Text(
              text = "Beim Start nach Aktualisierungen suchen",
              style = MaterialTheme.typography.labelLarge,
              fontFamily = DnaTypography.ManropeFamily,
              fontWeight = FontWeight.Bold,
              color = DnaColors.OnSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              // Die einzige zweite Gegenstelle neben Google — das gehört benannt, nicht
              // versteckt. Vorgabe ist aus.
              text = "Fragt höchstens einmal am Tag bei GitHub nach. GitHub erfährt dabei " +
                "deine IP-Adresse. Geladen und installiert wird nur auf deinen Tastendruck.",
              style = MaterialTheme.typography.bodySmall,
              fontFamily = DnaTypography.InterFamily,
              color = DnaColors.OnSurfaceVariant
            )
          }

          Switch(
            checked = updateCheckEnabled,
            onCheckedChange = { onSetUpdateCheck(it) },
            colors = SwitchDefaults.colors(
              checkedThumbColor = DnaColors.Primary,
              checkedTrackColor = DnaColors.PrimaryContainer,
              uncheckedThumbColor = DnaColors.TextDisabled,
              uncheckedTrackColor = DnaColors.SurfaceContainerLow
            ),
            modifier = Modifier.testTag("update_check_switch")
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(text = "Installiert: $installedVersion", style = DnaTypography.MonoSmall)

        Spacer(modifier = Modifier.height(12.dp))

        // Der einzige Weg, bei dem auch das Nicht-Ergebnis gemeldet wird. Die automatische
        // Prüfung schweigt immer, die ausdrücklich ausgelöste antwortet immer.
        DnaButton(
          text = "Jetzt prüfen",
          onClick = onCheckForUpdate,
          variant = DnaButtonVariant.SECONDARY,
          fullWidth = true,
          testTag = "check_update_button"
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // 6. Standard-Regie
    //
    // Frueher standen hier zwei Reiter: der Prompt dieser Geschichte und ein globaler, von dem
    // der erste den zweiten verdraengte. Jetzt gehoert der Geschichten-Prompt in die Geschichte
    // (Stift in der Kopfleiste), und dieser Text hier ist die generische Qualitaetsgrundlage,
    // die zusaetzlich mitgeschickt wird.
    Text(
      text = "STANDARD-REGIE",
      style = MaterialTheme.typography.labelSmall,
      fontFamily = DnaTypography.InterFamily,
      color = DnaColors.Primary,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = "Erzählhaltung für alle Geschichten. Wird zusätzlich zum Prompt der " +
        "jeweiligen Geschichte geschickt — leeren ist erlaubt.",
      style = MaterialTheme.typography.bodySmall,
      fontFamily = DnaTypography.InterFamily,
      color = DnaColors.OnSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
      value = globalPrompt,
      onValueChange = { globalPrompt = it },
      placeholder = {
        Text(
          "Leer: Es gilt nur der Prompt der jeweiligen Geschichte.",
          color = DnaColors.MutedForeground
        )
      },
      modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
        .testTag("global_prompt_input"),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = DnaColors.SurfaceContainer,
        unfocusedContainerColor = DnaColors.SurfaceContainer,
        focusedBorderColor = DnaColors.Primary,
        unfocusedBorderColor = DnaColors.Border,
        focusedTextColor = DnaColors.OnSurface,
        unfocusedTextColor = DnaColors.OnSurface
      ),
      shape = RoundedCornerShape(10.dp),
      textStyle = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp, fontFamily = DnaTypography.InterFamily)
    )

    Spacer(modifier = Modifier.height(28.dp))

    DnaButton(
      text = "Speichern",
      onClick = {
        onSaveAiSettings(
          AiSettings(
            chatModel = selectedModel,
            embeddingModel = selectedEmbeddingModel,
            transcriptionModel = selectedTranscriptionModel,
            thinkingLevel = thinkingLevel,
            thinkingBudget = thinkingBudget,
            temperature = temperature,
            supportsTemperature = supportsTemperature,
            adultContentEnabled = adultContent
          )
        )
        onSaveGlobalDefaultPrompt(globalPrompt)
        onSaveApiKey(apiKeyInput)
        onClose()
      },
      variant = DnaButtonVariant.PRIMARY,
      fullWidth = true,
      testTag = "save_settings_button"
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Die Erklärung steht in der App, nicht hinter einem Link. Eine App, die zusagt, dass
    // nichts unbemerkt hinausgeht, kann ihre eigene Datenschutzerklärung schlecht über einen
    // Aufruf nachladen, der wieder etwas hinausschickt.
    DnaButton(
      text = "Datenschutz",
      onClick = onOpenPrivacy,
      variant = DnaButtonVariant.GHOST,
      fullWidth = true,
      testTag = "open_privacy_button"
    )

    Spacer(modifier = Modifier.height(24.dp))
  }
}
