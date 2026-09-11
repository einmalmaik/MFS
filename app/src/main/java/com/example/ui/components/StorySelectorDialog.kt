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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.StoryEntity
import com.example.ui.theme.AmberGoldContainer
import com.example.ui.theme.AmberGoldPrimary
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.SlateDark600
import com.example.ui.theme.SlateDark700
import com.example.ui.theme.SlateDark800
import com.example.ui.theme.SlateDark900
import com.example.ui.theme.TextParchment
import com.example.ui.theme.TextParchmentFaint
import com.example.ui.theme.TextParchmentMuted

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
    npcOutfit = "Schwarzer Mantel mit optischer Tarnfaser, mechanischer linker Arm",
    npcRelation = "Geschäftspartner, pragmatisch, schuldet dir noch einen Gefallen",
    openingText = "Flackerndes Neonlicht taucht die feuchte Gasse in grelles Violett. Der Dunst der Straßenküchen mischt sich mit dem Ozon aus freiliegenden Starkstromleitungen.\n\nKael sitzt dir an einem schmierigen Holztisch gegenüber. Das schwache Surren seiner kybernetischen Servos ist kaum lauter als das Summen der Leuchtreklame draußen. Er schiebt einen winzigen Datenchip über den Tisch.\n\n\"Das Syndikat durchsucht die oberen Ebenen bereits\", murmelt er und nippt an seinem Tee. \"Wenn du das Ding entpackst, gibt es kein Zurück mehr.\""
  ),
  StoryPreset(
    title = "Flüsternde Asche",
    genre = "Dark Fantasy",
    perspective = "Zweite Person (Du)",
    location = "Verfallenes Kloster auf dem Nebelberg",
    outfit = "Schwerer Reisemantel aus Leinen, Kettenhemd, abgewetzte Lederhandschuhe",
    inventory = listOf("Silberklinge", "Trank der Klarsicht", "Siegelring des Ordens"),
    npcName = "Vera",
    npcOutfit = "Kapuzengewand aus grobem Wollstoff, Amulett aus Knochen",
    npcRelation = "Gefährten wider Willen, teilt das gleiche Schicksal",
    openingText = "Der Wind heult durch die zerbrochenen Spitzbogenfenster des verlassenen Sanktuariums. Auf dem Altar glimmt die letzte Glut eines Feuers, das vor Stunden erloschen ist.\n\nVera kniet am Rand des Steinkreises und zieht mit einem Stück Kreide die verblassten Runen nach. Ihre Finger zittern leicht vor Kälte.\n\n\"Das Ritual hält nicht bis zum Morgengrauen\", sagt sie, ohne den Kopf zu heben. \"Wir müssen entscheiden, wer von uns den Schwellenwächter ablenkt.\""
  ),
  StoryPreset(
    title = "Mitternacht im Grand Hotel",
    genre = "Romantisches Drama & Erotik (Adult)",
    perspective = "Zweite Person (Du)",
    location = "Penthouse-Suite mit Blick auf die beleuchtete Skyline",
    outfit = "Maßgeschneidertes dunkles Sakko, offenes weißes Hemd",
    inventory = listOf("Schlüsselkarte zu Zimmer 402", "Altes Foto", "Zigarettenetui"),
    npcName = "Sophie",
    npcOutfit = "Elegantes seidenes Abendkleid in Bordeauxrot, zarter Goldschmuck",
    npcRelation = "Verflossene Geliebte, unausgesprochene Spannungen und Vertrautheit",
    openingText = "Die Geräusche der abendlichen Gala im Ballsaal sind hier oben im 30. Stock nur noch ein fernes, dumpfes Echo. Durch die raumhohen Panoramafenster glitzert das endlose Lichtermeer der Metropole.\n\nSophie steht am Fenster, ein halbvolles Champagnerglas in der Hand. Die kühle Nachtluft weht durch die leicht geöffnete Balkontür und bewegt sanft eine Strähne ihres Haares.\n\nAls sie deine Schritte auf dem Parkett hört, dreht sie sich langsam um. In ihren Augen liegt dieser Blick, den du seit drei Jahren nicht mehr gesehen hast.\n\n\"Ich wusste, dass du heute Abend auftauchen würdest\", sagt sie ruhig. Ihre Stimme hat dieses vertraute, dunkle Timbre."
  ),
  StoryPreset(
    title = "Völlig freies Abenteuer",
    genre = "Eigene Welt / Sandbox",
    perspective = "Zweite Person (Du)",
    location = "Dein gewählter Startort",
    outfit = "Passende Kleidung für dein Setting",
    inventory = listOf("Grundausrüstung"),
    npcName = "",
    npcOutfit = "",
    npcRelation = "",
    openingText = "Die Geschichte beginnt genau so, wie du sie dir vorstellst."
  )
)

@Composable
fun StorySelectorDialog(
  currentStoryId: Long?,
  stories: List<StoryEntity>,
  onSelectStory: (Long) -> Unit,
  onDeleteStory: (Long) -> Unit,
  onBranchStory: (sourceStoryId: Long, branchTitle: String) -> Unit,
  onCreateNewStory: (
    title: String,
    genre: String,
    perspective: String,
    systemPrompt: String,
    model: String,
    temperature: Float,
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
  onDismiss: () -> Unit
) {
  var isCreatingNew by remember { mutableStateOf(false) }
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
    AlertDialog(
      onDismissRequest = { storyToDelete = null },
      title = { Text("Geschichte löschen?", color = TextParchment) },
      text = {
        Text(
          "Möchtest du '${target.title}' wirklich unwiderruflich aus der Datenbank löschen? Alle Nachrichten, Checkpoints und Inventareinträge gehen verloren.",
          color = TextParchmentMuted
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteStory(target.id)
            storyToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger)
        ) {
          Text("Endgültig löschen")
        }
      },
      dismissButton = {
        TextButton(onClick = { storyToDelete = null }) {
          Text("Abbrechen", color = TextParchment)
        }
      },
      containerColor = SlateDark800
    )
  }

  // Branch dialog
  if (storyToBranch != null) {
    val target = storyToBranch!!
    AlertDialog(
      onDismissRequest = { storyToBranch = null },
      title = { Text("Zweig erstellen (Branching)", color = TextParchment) },
      text = {
        Column {
          Text(
            "Erstelle eine isolierte Kopie von '${target.title}', um ab jetzt andere Entscheidungen zu treffen:",
            color = TextParchmentMuted,
            style = MaterialTheme.typography.bodySmall
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = branchTitleInput,
            onValueChange = { branchTitleInput = it },
            placeholder = { Text("Titel des neuen Zweigs...") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = SlateDark900,
              unfocusedContainerColor = SlateDark900,
              focusedTextColor = TextParchment,
              unfocusedTextColor = TextParchment,
              focusedBorderColor = AmberGoldPrimary,
              unfocusedBorderColor = SlateDark600
            )
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onBranchStory(target.id, branchTitleInput)
            storyToBranch = null
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = AmberGoldPrimary, contentColor = SlateDark900)
        ) {
          Text("Zweig starten", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { storyToBranch = null }) {
          Text("Abbrechen", color = TextParchment)
        }
      },
      containerColor = SlateDark800
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
      colors = CardDefaults.cardColors(containerColor = SlateDark900),
      border = androidx.compose.foundation.BorderStroke(1.dp, SlateDark600)
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
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Zurück", tint = AmberGoldPrimary)
              }
            } else {
              Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = null,
                tint = AmberGoldPrimary
              )
              Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
              text = if (isCreatingNew) "Neues Abenteuer erschaffen" else "Deine Geschichten",
              style = MaterialTheme.typography.titleMedium,
              color = TextParchment,
              fontFamily = FontFamily.Serif
            )
          }

          IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_story_selector")) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Schließen", tint = TextParchmentMuted)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = SlateDark700)
        Spacer(modifier = Modifier.height(14.dp))

        if (!isCreatingNew) {
          // List existing stories
          Text(
            text = "GESPEICHERTE GESCHICHTEN (${stories.size})",
            style = MaterialTheme.typography.labelSmall,
            color = AmberGoldPrimary,
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
              color = if (isCurrent) AmberGoldContainer else SlateDark800,
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isCurrent) AmberGoldPrimary else SlateDark600
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
                    color = if (isCurrent) AmberGoldPrimary else TextParchment,
                    fontWeight = FontWeight.Bold
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "${story.genre} • ${story.selectedModel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextParchmentMuted
                  )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                  // Branch button
                  IconButton(
                    onClick = {
                      branchTitleInput = "${story.title} (Zweig)"
                      storyToBranch = story
                    },
                    modifier = Modifier.size(32.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.AltRoute,
                      contentDescription = "Zweig erstellen",
                      tint = AmberGoldPrimary,
                      modifier = Modifier.size(16.dp)
                    )
                  }

                  // Delete button (allowed if > 1 story)
                  if (stories.size > 1) {
                    IconButton(
                      onClick = { storyToDelete = story },
                      modifier = Modifier.size(32.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Löschen",
                        tint = CrimsonDanger,
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          Button(
            onClick = {
              loadPreset(STORY_PRESETS[0])
              isCreatingNew = true
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("create_new_story_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = AmberGoldPrimary,
              contentColor = SlateDark900
            ),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Neue Geschichte anlegen", fontWeight = FontWeight.Bold)
          }
        } else {
          // New Story Creator with Presets
          Text(
            text = "WÄHLE EIN VORLAGE-GENRE ODER FREIES ABENTEUER",
            style = MaterialTheme.typography.labelSmall,
            color = AmberGoldPrimary,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(8.dp))

          STORY_PRESETS.forEachIndexed { index, preset ->
            val isSelected = selectedPresetIndex == index
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .clickable {
                  selectedPresetIndex = index
                  loadPreset(preset)
                },
              shape = RoundedCornerShape(8.dp),
              color = if (isSelected) AmberGoldContainer else SlateDark800,
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isSelected) AmberGoldPrimary else SlateDark600
              )
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = null,
                  tint = if (isSelected) AmberGoldPrimary else TextParchmentMuted,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(text = preset.title, style = MaterialTheme.typography.bodyMedium, color = TextParchment, fontWeight = FontWeight.SemiBold)
                  Text(text = preset.genre, style = MaterialTheme.typography.labelSmall, color = TextParchmentFaint)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          HorizontalDivider(color = SlateDark700)
          Spacer(modifier = Modifier.height(14.dp))

          // Detail Inputs
          Text(text = "TITEL DER GESCHICHTE", style = MaterialTheme.typography.labelSmall, color = AmberGoldPrimary, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = customTitle,
            onValueChange = { customTitle = it },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = SlateDark800,
              unfocusedContainerColor = SlateDark800,
              focusedTextColor = TextParchment,
              unfocusedTextColor = TextParchment,
              focusedBorderColor = AmberGoldPrimary,
              unfocusedBorderColor = SlateDark600
            ),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(10.dp))
          Text(text = "GENRE & SETTING", style = MaterialTheme.typography.labelSmall, color = AmberGoldPrimary, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = customGenre,
            onValueChange = { customGenre = it },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = SlateDark800,
              unfocusedContainerColor = SlateDark800,
              focusedTextColor = TextParchment,
              unfocusedTextColor = TextParchment,
              focusedBorderColor = AmberGoldPrimary,
              unfocusedBorderColor = SlateDark600
            ),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(10.dp))
          Text(text = "STARTORT", style = MaterialTheme.typography.labelSmall, color = AmberGoldPrimary, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = customLocation,
            onValueChange = { customLocation = it },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = SlateDark800,
              unfocusedContainerColor = SlateDark800,
              focusedTextColor = TextParchment,
              unfocusedTextColor = TextParchment,
              focusedBorderColor = AmberGoldPrimary,
              unfocusedBorderColor = SlateDark600
            ),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(10.dp))
          Text(text = "DEIN OUTFIT ZU BEGINN", style = MaterialTheme.typography.labelSmall, color = AmberGoldPrimary, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = customOutfit,
            onValueChange = { customOutfit = it },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = SlateDark800,
              unfocusedContainerColor = SlateDark800,
              focusedTextColor = TextParchment,
              unfocusedTextColor = TextParchment,
              focusedBorderColor = AmberGoldPrimary,
              unfocusedBorderColor = SlateDark600
            ),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(10.dp))
          Text(text = "START-INVENTAR (KOMMAGETRENNT)", style = MaterialTheme.typography.labelSmall, color = AmberGoldPrimary, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = customInventory,
            onValueChange = { customInventory = it },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = SlateDark800,
              unfocusedContainerColor = SlateDark800,
              focusedTextColor = TextParchment,
              unfocusedTextColor = TextParchment,
              focusedBorderColor = AmberGoldPrimary,
              unfocusedBorderColor = SlateDark600
            ),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(10.dp))
          Text(text = "ERSTER BEGLEITER / NPC NAME (OPTIONAL)", style = MaterialTheme.typography.labelSmall, color = AmberGoldPrimary, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = customNpcName,
            onValueChange = { customNpcName = it },
            placeholder = { Text("z. B. Elena", color = TextParchmentFaint) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = SlateDark800,
              unfocusedContainerColor = SlateDark800,
              focusedTextColor = TextParchment,
              unfocusedTextColor = TextParchment,
              focusedBorderColor = AmberGoldPrimary,
              unfocusedBorderColor = SlateDark600
            ),
            shape = RoundedCornerShape(8.dp)
          )

          if (customNpcName.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "OUTFIT VON $customNpcName", style = MaterialTheme.typography.labelSmall, color = AmberGoldPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
              value = customNpcOutfit,
              onValueChange = { customNpcOutfit = it },
              modifier = Modifier.fillMaxWidth(),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SlateDark800,
                unfocusedContainerColor = SlateDark800,
                focusedTextColor = TextParchment,
                unfocusedTextColor = TextParchment,
                focusedBorderColor = AmberGoldPrimary,
                unfocusedBorderColor = SlateDark600
              ),
              shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "BEZIEHUNG ZU DIR", style = MaterialTheme.typography.labelSmall, color = AmberGoldPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
              value = customNpcRelation,
              onValueChange = { customNpcRelation = it },
              modifier = Modifier.fillMaxWidth(),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SlateDark800,
                unfocusedContainerColor = SlateDark800,
                focusedTextColor = TextParchment,
                unfocusedTextColor = TextParchment,
                focusedBorderColor = AmberGoldPrimary,
                unfocusedBorderColor = SlateDark600
              ),
              shape = RoundedCornerShape(8.dp)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))
          Text(text = "ERÖFFNUNGSTEXT / PROLOG", style = MaterialTheme.typography.labelSmall, color = AmberGoldPrimary, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = customOpening,
            onValueChange = { customOpening = it },
            modifier = Modifier
              .fillMaxWidth()
              .height(150.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = SlateDark800,
              unfocusedContainerColor = SlateDark800,
              focusedTextColor = TextParchment,
              unfocusedTextColor = TextParchment,
              focusedBorderColor = AmberGoldPrimary,
              unfocusedBorderColor = SlateDark600
            ),
            shape = RoundedCornerShape(8.dp),
            textStyle = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp)
          )

          Spacer(modifier = Modifier.height(20.dp))

          Button(
            onClick = {
              val invList = customInventory.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

              onCreateNewStory(
                customTitle.ifBlank { "Unbenanntes Abenteuer" },
                customGenre.ifBlank { "Freies Abenteuer" },
                "Zweite Person (Du)",
                "", // Use global default prompt
                "gemini-2.5-flash",
                0.85f,
                2048,
                true,
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
            modifier = Modifier
              .fillMaxWidth()
              .testTag("confirm_create_story_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = AmberGoldPrimary,
              contentColor = SlateDark900
            ),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("Geschichte jetzt starten", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
