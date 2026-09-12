package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.api.GeminiClient
import com.example.data.model.GeminiModelInfo
import com.example.data.model.StoryEntity
import com.example.ui.dna.DnaColors
import com.example.ui.dna.DnaTypography
import com.example.ui.dna.DnaFloatNumberStepper
import com.example.ui.dna.DnaNumberStepper

data class StoryPreset(
  val title: String,
  val genre: String,
  val perspective: String,
  val location: String,
  val outfit: String,
  val inventory: List<String>,
  val npcName: String,
  val npcOutfit: String,
  val npcRelation: String,
  val openingText: String
)

val STORY_PRESETS = listOf(
  StoryPreset(
    title = "Schatten über der Hafenstadt",
    genre = "Dark Noir & Mystery",
    perspective = "Zweite Person (Du)",
    location = "Alte Lagerhalle am Nordhafen",
    outfit = "Durchnässte dunkle Lederjacke, grauer Hoodie, robuste Stiefel",
    inventory = listOf("Taschenlampe", "Dietrich-Set", "altes Notizbuch"),
    npcName = "Elena",
    npcOutfit = "Dunkelblauer Wollmantel, Lederstiefel, hochgeschlagener Kragen",
    npcRelation = "Misstrauisch, hält Abstand wegen der Ereignisse am Vortag",
    openingText = "Der Regen schlägt unbarmherzig gegen die verrosteten Wellblechwände der alten Lagerhalle am Nordhafen. Der Geruch von feuchtem Beton, Motorenöl und salziger Meeresluft hängt schwer im Raum.\n\nElena steht ein paar Schritte entfernt an einer umgestürzten Holzkiste. Sie hat die Hände tief in den Taschen ihres dunkelblauen Wollmantels vergraben. Ihr Blick wandert zur schweren Schiebetür, die nur einen Spalt breit offen steht. Im trüben Schein deiner schwachen Taschenlampe wirkt ihr Gesicht angespannt.\n\n\"Sie wissen, dass wir hier sind\", sagt sie leise, ohne dich direkt anzusehen. \"Die Frage ist nur, wie viel Zeit wir noch haben.\""
  ),
  StoryPreset(
    title = "Neon & Schatten: Sektor 7",
    genre = "Cyberpunk / Dystopie",
    perspective = "Zweite Person (Du)",
    location = "Hinterzimmer eines Ramen-Ladens im Untergrund",
    outfit = "Abgewetzte Tech-Jacke mit isolierten Ärmeln, Cyber-Brille, Cargo-Hose",
    inventory = listOf("Kompaktes Cyberdeck", "Verschlüsselter Datenchip", "Neuro-Injektor"),
    npcName = "Kael",
    npcOutfit = "Schwarzer Synthetik-Mantel mit LED-Säumen, Fingerlose Handschuhe",
    npcRelation = "Früherer Partner, schuldet dir noch einen Gefallen aus Neon-City",
    openingText = "Flackerndes Neonlicht in Giftgrün und Magenta dringt durch die Dunstabzugshaube des Ramen-Ladens. Draußen dröhnt der magnetische Schwebezug über Sektor 7, dass die Gläser im Regal klirren.\n\nKael sitzt dir gegenüber an dem ölverschmierten Edelstahltisch. Er schiebt sich die Cyber-Brille auf die Stirn und schiebt dir einen zerkratzten Glasfaser-Chip zu.\n\n\"Das ist der Code für den Arasaka-Sublevel\", flüstert er rauchig. \"Wenn sie merken, dass der weg ist, brennt ganz Sektor 7.\""
  ),
  StoryPreset(
    title = "Die verfluchte Feste von Alden",
    genre = "Dark Fantasy & Horror",
    perspective = "Zweite Person (Du)",
    location = "Verfallener Thronsaal der Bergfeste",
    outfit = "Brünierte Kettenrüstung, zerfetzter Wollumhang, schwere Lederhandschuhe",
    inventory = listOf("Breitschwert mit Silberrunen", "Fackel", "Weihwasser-Fläschchen"),
    npcName = "Priesterin Vespera",
    npcOutfit = "Aschgraue Robe mit gesticktem Sonnensymbol, silbernes Amulett",
    npcRelation = "Begleitet dich zur Reinigung der Feste, verbirgt jedoch ein eigenes Geheimnis",
    openingText = "Der Wind heult durch die zerschlagenen Buntglasfenster des Thronsaals. Auf dem eiskalten Steinboden zeichnen Schatten groteske Fratzen, während deine Fackel zischend das Dunkel zurückdrängt.\n\nVespera kniet vor den zerbrochenen Stufen des Throns und murmelt ein Schutzgebet auf Alt-Imperial. Als sie den Kopf hebt, spiegelt sich im Fackelschein nacktes Entsetzen in ihren Augen.\n\n\"Hörst du das?\", haucht sie. \"Es kratzt... von innen an den Mauern.\""
  ),
  StoryPreset(
    title = "Station Erebos: Stiller Orbit",
    genre = "Sci-Fi / Space Survival",
    perspective = "Zweite Person (Du)",
    location = "Druckschleuse B der verlassenen Orbitalstation",
    outfit = "Schwerer EVA-Raumanzug mit HUD-Helm, magnetische Arbeitsstiefel",
    inventory = listOf("Plasmaschneider", "Diagnose-Padd", "Notfallsauerstoff-Patrone"),
    npcName = "Dr. Marcus Chen",
    npcOutfit = "Verknitterter Stationskittel über dem Schutzanzug, Notfall-Headset",
    npcRelation = "Leitender Ingenieur, sichtlich traumatisiert vom Systemkollaps",
    openingText = "Mit einem metallischen Zischen schließt die Druckschleuse hinter euch. Die Statusanzeige schlägt von Not-Rot auf flackerndes Notstrom-Bernstein um. Schwerelosigkeit zieht an deinen Eingeweiden.\n\nDr. Chen stützt sich mit zitternden Händen an der Wandkonsole ab. Seine Atemmaske beschlägt bei jedem hektischen Atemzug.\n\n\"Der Hauptreaktor ist noch stabil\", keucht er über das Funkgerät. \"Aber die interne Biosphäre... etwas ist dort drin gewachsen, nachdem der Funkkontakt abbrach.\""
  )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorySelectorDialog(
  currentStoryId: Long?,
  stories: List<StoryEntity>,
  availableModels: List<GeminiModelInfo>,
  isFetchingModels: Boolean,
  onRefreshModels: () -> Unit,
  onSelectStory: (Long) -> Unit,
  onDeleteStory: (Long) -> Unit,
  onBranchStory: (sourceStoryId: Long, branchTitle: String) -> Unit,
  onCreateNewStory: (
    title: String,
    genre: String,
    perspective: String,
    systemPrompt: String,
    model: String,
    embeddingModel: String,
    temperature: Float,
    supportsTemperature: Boolean,
    thinkingLevel: String,
    thinkingBudget: Int,
    adultContent: Boolean,
    location: String,
    outfit: String,
    inventory: List<String>,
    npcName: String,
    npcOutfit: String,
    npcRelation: String,
    openingText: String
  ) -> Unit,
  initialCreateMode: Boolean = false,
  onDismiss: () -> Unit
) {
  var isCreatingNew by remember { mutableStateOf(initialCreateMode) }
  var storyToDelete by remember { mutableStateOf<StoryEntity?>(null) }
  var storyToBranch by remember { mutableStateOf<StoryEntity?>(null) }
  var branchTitleInput by remember { mutableStateOf("") }

  // New story form state
  var selectedPresetIndex by remember { mutableStateOf(0) }
  var customTitle by remember { mutableStateOf("") }
  var customGenre by remember { mutableStateOf("") }
  var customLocation by remember { mutableStateOf("") }
  var customOutfit by remember { mutableStateOf("") }
  var customInventory by remember { mutableStateOf("") }
  var customNpcName by remember { mutableStateOf("") }
  var customNpcOutfit by remember { mutableStateOf("") }
  var customNpcRelation by remember { mutableStateOf("") }
  var customOpening by remember { mutableStateOf("") }

  // Model and thinking configuration
  val defaultModelId = availableModels.firstOrNull { it.id == "gemini-3.8-flash" }?.id
    ?: availableModels.firstOrNull()?.id
    ?: "gemini-3.8-flash"

  var selectedModelId by remember { mutableStateOf(defaultModelId) }
  var selectedEmbeddingModel by remember { mutableStateOf("text-embedding-004") }
  val currentModelInfo = availableModels.firstOrNull { it.id == selectedModelId }
    ?: GeminiModelInfo(
      id = selectedModelId,
      displayName = selectedModelId,
      description = "",
      supportsTemperature = true,
      defaultTemperature = 0.85f,
      isThinkingModel = true,
      usesThinkingLevel = true
    )

  var thinkingLevel by remember { mutableStateOf("MEDIUM") }
  var thinkingBudget by remember { mutableIntStateOf(2048) }
  var temperature by remember { mutableFloatStateOf(0.85f) }
  var adultContent by remember { mutableStateOf(true) }

  var modelDropdownExpanded by remember { mutableStateOf(false) }
  var thinkingDropdownExpanded by remember { mutableStateOf(false) }

  fun loadPreset(preset: StoryPreset) {
    customTitle = preset.title
    customGenre = preset.genre
    customLocation = preset.location
    customOutfit = preset.outfit
    customInventory = preset.inventory.joinToString(", ")
    customNpcName = preset.npcName
    customNpcOutfit = preset.npcOutfit
    customNpcRelation = preset.npcRelation
    customOpening = preset.openingText
  }

  // Delete confirmation dialog
  if (storyToDelete != null) {
    val target = storyToDelete!!
    com.example.ui.dna.DnaActionConfirmDialog(
      title = "Geschichte löschen?",
      description = "Möchtest du '${target.title}' wirklich unwiderruflich aus der lokalen Datenbank entfernen? Alle Nachrichten, Checkpoints und NPCs dieser Geschichte gehen verloren.",
      confirmButtonText = "Endgültig löschen",
      cancelButtonText = "Abbrechen",
      isDestructive = true,
      onConfirm = {
        onDeleteStory(target.id)
        storyToDelete = null
      },
      onDismiss = { storyToDelete = null }
    )
  }

  // Branch dialog
  if (storyToBranch != null) {
    val target = storyToBranch!!
    AlertDialog(
      onDismissRequest = { storyToBranch = null },
      title = { Text("Zweig erstellen (Branching)", color = DnaColors.OnSurface) },
      text = {
        Column {
          Text(
            "Erstelle eine unabhängige Kopie von '${target.title}', um ab jetzt alternative Entscheidungen zu erkunden:",
            color = DnaColors.OnSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = branchTitleInput,
            onValueChange = { branchTitleInput = it },
            placeholder = { Text("${target.title} (Alternativer Pfad)", color = DnaColors.MutedForeground) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = DnaColors.Surface,
              unfocusedContainerColor = DnaColors.Surface,
              focusedTextColor = DnaColors.OnSurface,
              unfocusedTextColor = DnaColors.OnSurface,
              focusedBorderColor = DnaColors.Primary,
              unfocusedBorderColor = DnaColors.Border
            )
          )
        }
      },
      confirmButton = {
        DnaButton(
          text = "Zweig starten",
          onClick = {
            onBranchStory(target.id, branchTitleInput)
            storyToBranch = null
            onDismiss()
          },
          variant = DnaButtonVariant.PRIMARY,
          testTag = "confirm_branch_story_button"
        )
      },
      dismissButton = {
        DnaButton(
          text = "Abbrechen",
          onClick = { storyToBranch = null },
          variant = DnaButtonVariant.GHOST,
          testTag = "cancel_branch_story_button"
        )
      },
      containerColor = DnaColors.SurfaceContainer
    )
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .padding(16.dp),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = DnaColors.Surface),
      border = androidx.compose.foundation.BorderStroke(1.dp, DnaColors.Border)
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (isCreatingNew) {
              IconButton(onClick = { isCreatingNew = false }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = DnaColors.Primary)
              }
            } else {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = DnaColors.Primary
              )
              Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
              text = if (isCreatingNew) "Neues Abenteuer erschaffen" else "Deine Geschichten",
              style = MaterialTheme.typography.titleMedium,
              color = DnaColors.OnSurface,
              fontFamily = DnaTypography.ManropeFamily,
              fontWeight = FontWeight.Bold
            )
          }

          IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_story_selector")) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Schließen", tint = DnaColors.OnSurfaceVariant)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = DnaColors.SurfaceContainerHigh)
        Spacer(modifier = Modifier.height(14.dp))

        if (!isCreatingNew) {
          // List existing stories
          Text(
            text = "GESPEICHERTE GESCHICHTEN (${stories.size})",
            style = MaterialTheme.typography.labelSmall,
            color = DnaColors.Primary,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(8.dp))

          stories.forEach { story ->
            val isCurrent = story.id == currentStoryId
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clickable {
                  onSelectStory(story.id)
                  onDismiss()
                },
              shape = RoundedCornerShape(10.dp),
              color = if (isCurrent) DnaColors.PrimaryContainer else DnaColors.SurfaceContainer,
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isCurrent) DnaColors.Primary else DnaColors.Border
              )
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = story.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCurrent) DnaColors.Primary else DnaColors.OnSurface,
                    fontWeight = FontWeight.Bold
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "${story.genre} • ${story.selectedModel} (${story.thinkingLevel})",
                    style = MaterialTheme.typography.bodySmall,
                    color = DnaColors.OnSurfaceVariant
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  val hours = story.playTimeSeconds / 3600
                  val minutes = (story.playTimeSeconds % 3600) / 60
                  val timeString = if (hours > 0) {
                    "${hours}h ${minutes}m"
                  } else if (minutes > 0) {
                    "${minutes}m"
                  } else {
                    "< 1m"
                  }
                  Text(
                    text = "Spielzeit: $timeString",
                    style = MaterialTheme.typography.labelSmall,
                    color = DnaColors.OnSurfaceVariant
                  )
                }

                Row {
                  IconButton(
                    onClick = {
                      branchTitleInput = "${story.title} (Zweig)"
                      storyToBranch = story
                    }
                  ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.AltRoute, contentDescription = "Zweig erstellen", tint = DnaColors.Secondary)
                  }

                  if (stories.size > 1) {
                    IconButton(
                      onClick = { storyToDelete = story }
                    ) {
                      Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Löschen", tint = DnaColors.StatusDestructive)
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          DnaButton(
            text = "Völlig neue Geschichte beginnen",
            onClick = {
              loadPreset(STORY_PRESETS.first())
              isCreatingNew = true
            },
            icon = Icons.Default.Add,
            variant = DnaButtonVariant.PRIMARY,
            fullWidth = true,
            testTag = "create_new_story_button"
          )
        } else {
          // --- CREATE NEW STORY VIEW ---
          Text(
            text = "SCHNELLE VORLAGEN (PRESETS)",
            style = MaterialTheme.typography.labelSmall,
            color = DnaColors.Primary,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(6.dp))

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            STORY_PRESETS.forEachIndexed { index, preset ->
              val isSelected = selectedPresetIndex == index
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    selectedPresetIndex = index
                    loadPreset(preset)
                  },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) DnaColors.PrimaryContainer else DnaColors.SurfaceContainer,
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (isSelected) DnaColors.Primary else DnaColors.SurfaceContainerHigh
                )
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (isSelected) DnaColors.Primary else DnaColors.OnSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(
                      text = preset.title,
                      style = MaterialTheme.typography.bodyMedium,
                      color = if (isSelected) DnaColors.Primary else DnaColors.OnSurface,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = preset.genre,
                      style = MaterialTheme.typography.bodySmall,
                      color = DnaColors.OnSurfaceVariant
                    )
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
          HorizontalDivider(color = DnaColors.SurfaceContainerHigh)
          Spacer(modifier = Modifier.height(16.dp))

          // Model selection for new story
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "GEMINI-MODELL FÜR DIESE GESCHICHTE",
              style = MaterialTheme.typography.labelSmall,
              color = DnaColors.Primary,
              fontWeight = FontWeight.Bold
            )

            TextButton(
              onClick = onRefreshModels,
              enabled = !isFetchingModels
            ) {
              if (isFetchingModels) {
                CircularProgressIndicator(color = DnaColors.Primary, modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
              } else {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = DnaColors.Secondary, modifier = Modifier.size(14.dp))
              }
              Spacer(modifier = Modifier.width(4.dp))
              Text("Aktualisieren", style = MaterialTheme.typography.labelSmall, color = DnaColors.Secondary)
            }
          }

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
            selectedValue = selectedModelId,
            onOptionSelected = { newId ->
              selectedModelId = newId
              availableModels.find { it.id == newId }?.let {
                temperature = it.defaultTemperature
              }
            },
            modifier = Modifier.fillMaxWidth().testTag("story_model_dropdown")
          )

          Spacer(modifier = Modifier.height(16.dp))

          val embeddingModelOptions = listOf(
            com.example.ui.dna.DnaDropdownOption("text-embedding-004", "Text Embedding 004", "Neuestes & bestes Modell"),
            com.example.ui.dna.DnaDropdownOption("embedding-001", "Embedding 001", "Legacy Modell")
          )
          com.example.ui.dna.DnaDropdown(
            label = "EMBEDDING MODELL",
            options = embeddingModelOptions,
            selectedValue = selectedEmbeddingModel,
            onOptionSelected = { selectedEmbeddingModel = it },
            modifier = Modifier.fillMaxWidth().testTag("story_embedding_model_dropdown")
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Thinking level or thinking budget
          if (currentModelInfo.usesThinkingLevel) {
            val thinkingOptions = remember {
              listOf(
                com.example.ui.dna.DnaDropdownOption("MINIMAL", "Minimal", "Höchste Geschwindigkeit"),
                com.example.ui.dna.DnaDropdownOption("LOW", "Niedrig", "Schnell"),
                com.example.ui.dna.DnaDropdownOption("MEDIUM", "Mittel (Standard)", "Ausgewogene Psychologie"),
                com.example.ui.dna.DnaDropdownOption("HIGH", "Hoch", "Tiefgründige Reflexion & Konsistenz")
              )
            }

            com.example.ui.dna.DnaDropdown(
              label = "DENKSTUFE (REASONING EFFORT)",
              options = thinkingOptions,
              selectedValue = thinkingLevel,
              onOptionSelected = { thinkingLevel = it },
              icon = Icons.Default.Psychology,
              modifier = Modifier.fillMaxWidth().testTag("story_thinking_dropdown")
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Temperature slider or notice
          if (currentModelInfo.supportsTemperature) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(text = "TEMPERATUR (KREATIVITÄT)", style = MaterialTheme.typography.labelSmall, color = DnaColors.Primary, fontWeight = FontWeight.Bold)
              Text(String.format("%.2f", temperature), style = MaterialTheme.typography.labelMedium, color = DnaColors.OnSurface, fontWeight = FontWeight.Bold)
            }
            Slider(
              value = temperature,
              onValueChange = { temperature = it },
              valueRange = 0.2f..1.5f,
              steps = 26,
              colors = SliderDefaults.colors(
                thumbColor = DnaColors.Primary,
                activeTrackColor = DnaColors.Primary,
                inactiveTrackColor = DnaColors.SurfaceContainerHigh
              )
            )
          } else {
            Card(
              colors = CardDefaults.cardColors(containerColor = DnaColors.SurfaceContainer),
              shape = RoundedCornerShape(8.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, DnaColors.SurfaceContainerHigh)
            ) {
              Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = DnaColors.Secondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Temperatur wird von $selectedModelId fest verwaltet.", style = MaterialTheme.typography.bodySmall, color = DnaColors.OnSurfaceVariant)
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Adult content filter switch
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(DnaColors.SurfaceContainer, RoundedCornerShape(8.dp))
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = DnaColors.Primary, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Adult Content Filter (BLOCK_NONE)", style = MaterialTheme.typography.bodyMedium, color = DnaColors.OnSurface)
            }
            Switch(
              checked = adultContent,
              onCheckedChange = { adultContent = it },
              colors = SwitchDefaults.colors(
                checkedThumbColor = DnaColors.Primary,
                checkedTrackColor = DnaColors.PrimaryContainer,
                uncheckedThumbColor = DnaColors.OnSurfaceVariant,
                uncheckedTrackColor = DnaColors.SurfaceContainerHigh
              )
            )
          }

          Spacer(modifier = Modifier.height(16.dp))
          HorizontalDivider(color = DnaColors.SurfaceContainerHigh)
          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = "DETAILS & CHARAKTER-SETTING",
            style = MaterialTheme.typography.labelSmall,
            color = DnaColors.Primary,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(8.dp))

          Text(text = "TITEL DER GESCHICHTE", style = MaterialTheme.typography.labelSmall, color = DnaColors.Primary, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = customTitle,
            onValueChange = { customTitle = it },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = DnaColors.SurfaceContainer,
              unfocusedContainerColor = DnaColors.SurfaceContainer,
              focusedTextColor = DnaColors.OnSurface,
              unfocusedTextColor = DnaColors.OnSurface,
              focusedBorderColor = DnaColors.Primary,
              unfocusedBorderColor = DnaColors.Border
            ),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(10.dp))
          Text(text = "GENRE & SETTING", style = MaterialTheme.typography.labelSmall, color = DnaColors.Primary, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = customGenre,
            onValueChange = { customGenre = it },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = DnaColors.SurfaceContainer,
              unfocusedContainerColor = DnaColors.SurfaceContainer,
              focusedTextColor = DnaColors.OnSurface,
              unfocusedTextColor = DnaColors.OnSurface,
              focusedBorderColor = DnaColors.Primary,
              unfocusedBorderColor = DnaColors.Border
            ),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(10.dp))
          Text(text = "STARTORT", style = MaterialTheme.typography.labelSmall, color = DnaColors.Primary, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = customLocation,
            onValueChange = { customLocation = it },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = DnaColors.SurfaceContainer,
              unfocusedContainerColor = DnaColors.SurfaceContainer,
              focusedTextColor = DnaColors.OnSurface,
              unfocusedTextColor = DnaColors.OnSurface,
              focusedBorderColor = DnaColors.Primary,
              unfocusedBorderColor = DnaColors.Border
            ),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(10.dp))
          Text(text = "DEIN OUTFIT ZU BEGINN", style = MaterialTheme.typography.labelSmall, color = DnaColors.Primary, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = customOutfit,
            onValueChange = { customOutfit = it },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = DnaColors.SurfaceContainer,
              unfocusedContainerColor = DnaColors.SurfaceContainer,
              focusedTextColor = DnaColors.OnSurface,
              unfocusedTextColor = DnaColors.OnSurface,
              focusedBorderColor = DnaColors.Primary,
              unfocusedBorderColor = DnaColors.Border
            ),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(10.dp))
          Text(text = "START-INVENTAR (KOMMAGETRENNT)", style = MaterialTheme.typography.labelSmall, color = DnaColors.Primary, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = customInventory,
            onValueChange = { customInventory = it },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = DnaColors.SurfaceContainer,
              unfocusedContainerColor = DnaColors.SurfaceContainer,
              focusedTextColor = DnaColors.OnSurface,
              unfocusedTextColor = DnaColors.OnSurface,
              focusedBorderColor = DnaColors.Primary,
              unfocusedBorderColor = DnaColors.Border
            ),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(10.dp))
          Text(text = "ERSTER BEGLEITER / NPC NAME (OPTIONAL)", style = MaterialTheme.typography.labelSmall, color = DnaColors.Primary, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = customNpcName,
            onValueChange = { customNpcName = it },
            placeholder = { Text("z. B. Elena", color = DnaColors.MutedForeground) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = DnaColors.SurfaceContainer,
              unfocusedContainerColor = DnaColors.SurfaceContainer,
              focusedTextColor = DnaColors.OnSurface,
              unfocusedTextColor = DnaColors.OnSurface,
              focusedBorderColor = DnaColors.Primary,
              unfocusedBorderColor = DnaColors.Border
            ),
            shape = RoundedCornerShape(8.dp)
          )

          if (customNpcName.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "OUTFIT VON $customNpcName", style = MaterialTheme.typography.labelSmall, color = DnaColors.Primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
              value = customNpcOutfit,
              onValueChange = { customNpcOutfit = it },
              modifier = Modifier.fillMaxWidth(),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DnaColors.SurfaceContainer,
                unfocusedContainerColor = DnaColors.SurfaceContainer,
                focusedTextColor = DnaColors.OnSurface,
                unfocusedTextColor = DnaColors.OnSurface,
                focusedBorderColor = DnaColors.Primary,
                unfocusedBorderColor = DnaColors.Border
              ),
              shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "BEZIEHUNG ZU DIR", style = MaterialTheme.typography.labelSmall, color = DnaColors.Primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
              value = customNpcRelation,
              onValueChange = { customNpcRelation = it },
              modifier = Modifier.fillMaxWidth(),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DnaColors.SurfaceContainer,
                unfocusedContainerColor = DnaColors.SurfaceContainer,
                focusedTextColor = DnaColors.OnSurface,
                unfocusedTextColor = DnaColors.OnSurface,
                focusedBorderColor = DnaColors.Primary,
                unfocusedBorderColor = DnaColors.Border
              ),
              shape = RoundedCornerShape(8.dp)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))
          Text(text = "ERÖFFNUNGSTEXT / PROLOG", style = MaterialTheme.typography.labelSmall, color = DnaColors.Primary, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = customOpening,
            onValueChange = { customOpening = it },
            modifier = Modifier
              .fillMaxWidth()
              .height(150.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = DnaColors.SurfaceContainer,
              unfocusedContainerColor = DnaColors.SurfaceContainer,
              focusedTextColor = DnaColors.OnSurface,
              unfocusedTextColor = DnaColors.OnSurface,
              focusedBorderColor = DnaColors.Primary,
              unfocusedBorderColor = DnaColors.Border
            ),
            shape = RoundedCornerShape(8.dp),
            textStyle = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp)
          )

          Spacer(modifier = Modifier.height(20.dp))

          DnaButton(
            text = "Geschichte jetzt starten",
            onClick = {
              val invList = customInventory.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

              onCreateNewStory(
                customTitle.ifBlank { "Unbenanntes Abenteuer" },
                customGenre.ifBlank { "Freies Abenteuer" },
                "Zweite Person (Du)",
                "", // Use global default prompt
                selectedModelId,
                selectedEmbeddingModel,
                temperature,
                currentModelInfo.supportsTemperature,
                thinkingLevel,
                thinkingBudget,
                adultContent,
                customLocation.ifBlank { "Startort" },
                customOutfit.ifBlank { "Alltagskleidung" },
                invList,
                customNpcName,
                customNpcOutfit,
                customNpcRelation,
                customOpening
              )
              onDismiss()
            },
            variant = DnaButtonVariant.PRIMARY,
            fullWidth = true,
            testTag = "confirm_create_story_button"
          )
        }
      }
    }
  }
}
