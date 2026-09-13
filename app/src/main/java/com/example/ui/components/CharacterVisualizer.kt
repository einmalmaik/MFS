package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.BodyOrgan
import com.example.data.model.BodyPart
import com.example.data.model.CharacterGender
import com.example.data.model.CharacterInjury
import com.example.data.model.CheckpointEntity
import com.example.data.model.InjurySeverity
import com.example.data.model.NpcInfo
import com.example.ui.dna.DnaBadge
import com.example.ui.dna.DnaBadgeTone
import com.example.ui.dna.DnaButton
import com.example.ui.dna.DnaButtonVariant
import com.example.ui.dna.DnaCard
import com.example.ui.dna.DnaColors
import com.example.ui.dna.DnaDropdown
import com.example.ui.dna.DnaDropdownOption
import com.example.ui.dna.DnaTypography

/** Kennung für "kein Organ betroffen" im Organ-Dropdown. */
private const val NO_ORGAN = "NONE"

/**
 * Schlagworte der Zustandsbeschreibung und ihr Abzug auf die Vitalität.
 *
 * Bewusst eine Heuristik über Text: Die Extraktion liefert eine freie Beschreibung, keinen
 * Zahlenwert. Ein zusätzliches Zahlenfeld im Schema wäre mehr Angriffsfläche für Drift als
 * Nutzen — die Abstufung hier muss nur grob stimmen, damit die Anzeige nicht mehr behauptet,
 * eine ausgezehrte Figur sei "vollständig einsatzbereit".
 */
private val CONDITION_PENALTIES = listOf(
  "sterbend" to 55,
  "bewusstlos" to 50,
  "lebensgefahr" to 50,
  "zusammenbruch" to 40,
  "ausgezehrt" to 30,
  "abgemagert" to 30,
  "unterernährt" to 30,
  "verhungert" to 35,
  "dehydriert" to 25,
  "verdurstet" to 30,
  "fieber" to 25,
  "vergiftet" to 30,
  "unterkühlt" to 20,
  "atemnot" to 20,
  "schwäche" to 20,
  "entkräftet" to 25,
  "erschöpft" to 15,
  "angeschlagen" to 10,
  "schwindel" to 10
)

/**
 * Summiert die Abzüge aller erkannten Schlagworte, gedeckelt auf 70 — der Rest bleibt den
 * Wunden vorbehalten, damit eine kranke Figur ohne Verletzung nicht als tot dasteht.
 */
internal fun conditionPenaltyOf(condition: String): Int {
  val lower = condition.lowercase()
  if (lower.isBlank() || lower.contains("unverletzt") && lower.length < 16) return 0
  val sum = CONDITION_PENALTIES.filter { lower.contains(it.first) }.sumOf { it.second }
  // Ein "schwer"/"extrem" davor verschärft den Befund spürbar.
  val verstaerkt = lower.contains("schwer") || lower.contains("extrem") || lower.contains("massiv")
  return (if (verstaerkt) (sum * 1.3f).toInt() else sum).coerceAtMost(70)
}

/**
 * Vollstaendiger interaktiver Charakter- und Ausruestungs-Visualisierer.
 *
 * Beinhaltet:
 * - Multi-Charakter-Unterstuetzung: Nahtloser Wechsel zwischen Spieler und allen Story-NPCs
 * - Anatomischer Mannequin-Canvas fuer maennliche und weibliche Silhouetten
 * - Praezise Wund- und Verletzungskartierung mit pulsierenden Beacons
 * - Interaktive Koerperteil-Inspektion und Dialog zum Hinzufuegen/Heilen von Verletzungen
 * - Vollstaendige Rueckbindung an das KI-System (Gemini erhaelt den exakten Verletzungszustand)
 */
@Composable
fun CharacterVisualizer(
  checkpoint: CheckpointEntity?,
  adultContentEnabled: Boolean = true,
  onUpdateInjuries: ((characterName: String, updatedInjuries: List<CharacterInjury>) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val npcs = checkpoint?.getNpcList() ?: emptyList()
  var selectedCharacterIndex by remember { mutableStateOf(0) } // 0 = Spieler, 1..N = NPCs

  // Speichert das Geschlecht pro Charakter lokal (Standard: Spieler maennlich, NPCs nach Heuristik)
  val characterGenders = remember {
    mutableStateMapOf<String, CharacterGender>().apply {
      put("Du", CharacterGender.MALE)
    }
  }

  val isPlayer = selectedCharacterIndex == 0
  val currentNpc = if (!isPlayer && selectedCharacterIndex - 1 < npcs.size) {
    npcs[selectedCharacterIndex - 1]
  } else null

  val characterName = if (isPlayer) "Du" else (currentNpc?.name ?: "NPC")
  val displayName = if (isPlayer) "Du" else (currentNpc?.name ?: "NPC")

  val currentGender = characterGenders.getOrPut(characterName) {
    // Das Geschlecht kommt aus der Extraktion. Die Namensheuristik bleibt nur als Notbehelf für
    // Altbestände ohne dieses Feld — geraten wird, wenn die Geschichte es nicht hergibt.
    val extracted = currentNpc?.gender.orEmpty()
    if (extracted.isNotBlank()) {
      CharacterGender.fromString(extracted)
    } else {
      val nameLower = characterName.lowercase()
      if (nameLower.endsWith("a") || nameLower.endsWith("e") || nameLower.contains("frau")) {
        CharacterGender.FEMALE
      } else {
        CharacterGender.MALE
      }
    }
  }

  val characterOutfit = if (isPlayer) {
    checkpoint?.playerOutfit?.ifBlank { "Reisekleidung" } ?: "Reisekleidung"
  } else {
    currentNpc?.outfit?.ifBlank { "Zivilkleidung" } ?: "Zivilkleidung"
  }

  // Körperliche Verfassung. Für NPCs stand hier früher die Stimmung — dadurch galt eine Figur
  // als "fiebrig, verängstigt", während der Spieler daneben "unterernährt" war, obwohl beide
  // dieselbe Entbehrung teilten.
  val characterCondition = if (isPlayer) {
    checkpoint?.playerCondition?.ifBlank { "Unverletzt" } ?: "Unverletzt"
  } else {
    currentNpc?.condition?.ifBlank { "Keine Angabe" } ?: "Keine Angabe"
  }

  val characterMood = if (isPlayer) "" else currentNpc?.currentMood.orEmpty()

  // Verletzungen fuer den aktuell gewaehlten Charakter
  val characterInjuries = remember(checkpoint?.rawStateJson, checkpoint?.playerCondition, characterName) {
    checkpoint?.getCharacterInjuries(characterName) ?: emptyList()
  }

  var selectedBodyPart by remember { mutableStateOf<BodyPart?>(null) }
  var selectedOrgan by remember { mutableStateOf<BodyOrgan?>(null) }
  var mannequinView by remember { mutableStateOf(MannequinView.SCAN) }
  var showAddInjuryDialog by remember { mutableStateOf(false) }

  val isAdultOutfit = remember(characterOutfit) {
    val lower = characterOutfit.lowercase()
    lower.contains("dessous") || lower.contains("nackt") || lower.contains("entblößt") ||
      lower.contains("freizügig") || lower.contains("zerrissen") || lower.contains("reizwäsche") ||
      lower.contains("knapp") || lower.contains("oben ohne") || lower.contains("transparent")
  }

  DnaCard(
    modifier = modifier.fillMaxWidth(),
    backgroundColor = DnaColors.SurfaceContainer,
    borderColor = if (isAdultOutfit && adultContentEnabled) DnaColors.StatusDestructive.copy(alpha = 0.5f) else DnaColors.Border
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Kopfzeile: Titel und Adult-Modus
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = DnaColors.Primary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Anatomie",
            fontFamily = DnaTypography.ManropeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = DnaColors.OnSurface
          )
        }

        if (adultContentEnabled && isAdultOutfit) {
          DnaBadge(
            text = "Freizügig",
            tone = DnaBadgeTone.DANGER,
            showDot = true
          )
        } else {
          DnaBadge(
            text = checkpoint?.inGameTime ?: "Tag 1",
            tone = DnaBadgeTone.ICE,
            showDot = false
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Charakter-Auswahlleiste (Horizontal scrollbar fuer beliebig viele Charaktere)
      Text(
        text = "FIGUR",
        fontFamily = DnaTypography.InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp,
        color = DnaColors.OnSurfaceVariant
      )

      Spacer(modifier = Modifier.height(6.dp))

      // getCharacterInjuries parst jedes Mal den kompletten Weltzustand. Direkt in der
      // Chip-Reihe aufgerufen hieß das: einmal je Figur, und das bei jeder Neuzeichnung --
      // also auch beim bloßen Antippen einer anderen Figur oder Umschalten des Geschlechts.
      // Neu gerechnet wird jetzt nur, wenn sich Zustand oder Figurenliste ändern.
      val verletzungsZahlen = remember(checkpoint, npcs) {
        buildMap {
          put("Du", checkpoint?.getCharacterInjuries("Du")?.size ?: 0)
          npcs.forEach { npc ->
            put(npc.name, checkpoint?.getCharacterInjuries(npc.name)?.size ?: 0)
          }
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Spieler-Button
        CharacterSelectorChip(
          title = "Du (Spieler)",
          isSelected = selectedCharacterIndex == 0,
          injuryCount = verletzungsZahlen["Du"] ?: 0,
          onClick = {
            selectedCharacterIndex = 0
            selectedBodyPart = null
          }
        )

        // Alle NPCs der Story
        npcs.forEachIndexed { index, npc ->
          CharacterSelectorChip(
            title = npc.name,
            isSelected = selectedCharacterIndex == index + 1,
            injuryCount = verletzungsZahlen[npc.name] ?: 0,
            onClick = {
              selectedCharacterIndex = index + 1
              selectedBodyPart = null
            }
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Geschlechts-Umschalter fuer die anatomische Silhouette
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = displayName,
          fontFamily = DnaTypography.ManropeFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          color = DnaColors.Primary
        )

        // Geschlechts-Toggle
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DnaColors.SurfaceContainerHigh)
            .border(1.dp, DnaColors.Border, RoundedCornerShape(8.dp))
            .padding(2.dp)
        ) {
          GenderToggleButton(
            title = "Männlich",
            icon = Icons.Default.Male,
            isSelected = currentGender == CharacterGender.MALE,
            onClick = { characterGenders[characterName] = CharacterGender.MALE }
          )
          GenderToggleButton(
            title = "Weiblich",
            icon = Icons.Default.Female,
            isSelected = currentGender == CharacterGender.FEMALE,
            onClick = { characterGenders[characterName] = CharacterGender.FEMALE }
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Vitalität aus Wunden UND körperlicher Verfassung.
      //
      // Nur Wunden zu zählen ergab einen sichtbaren Widerspruch: Eine Figur mit "schwer
      // unterernährt, hohes Fieber, extreme Schwäche" stand als "vollständig einsatzbereit" da,
      // solange sie keine offene Wunde hatte. Hunger und Krankheit schwächen aber genauso.
      val injuryPenalty = characterInjuries.sumOf {
        when (it.severity) {
          InjurySeverity.LIGHT -> 5
          InjurySeverity.MEDIUM -> 15
          InjurySeverity.SEVERE -> 25
          InjurySeverity.CRITICAL -> 40
        }
      }
      val conditionPenalty = remember(characterCondition) {
        conditionPenaltyOf(characterCondition)
      }
      val vitalityPercent = (100 - injuryPenalty - conditionPenalty).coerceIn(5, 100)
      val vitalityColor = when {
        vitalityPercent >= 80 -> DnaColors.MintAccent
        vitalityPercent >= 50 -> DnaColors.StatusWarning
        else -> DnaColors.StatusDestructive
      }
      val conditionTitle = when {
        vitalityPercent == 100 -> "Vollständig einsatzbereit"
        vitalityPercent >= 80 -> "Leicht angeschlagen ($vitalityPercent%)"
        // Getrennte Wortwahl: "Verwundet" wäre falsch, wenn die Schwäche von Hunger und
        // Krankheit kommt und keine einzige Wunde vorliegt.
        vitalityPercent >= 50 ->
          if (characterInjuries.isEmpty()) "Geschwächt ($vitalityPercent%)" else "Verwundet ($vitalityPercent%)"
        else -> "Kritischer Zustand ($vitalityPercent%)"
      }

      // 1. Vitalitäts- & Gesundheits-Banner
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = DnaColors.SurfaceContainerHigh,
        border = BorderStroke(1.dp, DnaColors.Border),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = vitalityColor,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = conditionTitle,
                fontFamily = DnaTypography.ManropeFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = DnaColors.OnSurface
              )
            }

            if (characterInjuries.isNotEmpty()) {
              DnaBadge(
                text = "${characterInjuries.size} ${if (characterInjuries.size == 1) "Wunde" else "Wunden"}",
                tone = if (vitalityPercent < 50) DnaBadgeTone.DANGER else DnaBadgeTone.AMBER,
                showDot = true
              )
            } else {
              DnaBadge(
                text = if (conditionPenalty > 0) "Keine Wunden" else "Unverletzt",
                tone = if (conditionPenalty > 0) DnaBadgeTone.AMBER else DnaBadgeTone.MINT,
                showDot = conditionPenalty > 0
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Vitalitäts-Balken
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp))
              .background(DnaColors.SurfaceContainerLowest)
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth(vitalityPercent / 100f)
                .fillMaxHeight()
                .background(vitalityColor)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 2. Anatomische Körperregionen (Interaktiver Mannequin)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (mannequinView == MannequinView.SCAN) "DURCHLEUCHTUNG" else "KÖRPERREGIONEN",
          fontFamily = DnaTypography.InterFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 11.sp,
          color = DnaColors.OnSurfaceVariant
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (selectedBodyPart != null || selectedOrgan != null) {
            Text(
              text = "Zurücksetzen",
              fontFamily = DnaTypography.InterFamily,
              fontSize = 11.sp,
              color = DnaColors.Primary,
              modifier = Modifier
                .clickable {
                  selectedBodyPart = null
                  selectedOrgan = null
                }
                .padding(4.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
          }

          // Umschalter Haut / Scan
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(DnaColors.SurfaceContainerHigh)
              .border(1.dp, DnaColors.Border, RoundedCornerShape(8.dp))
              .padding(2.dp)
          ) {
            GenderToggleButton(
              title = "Haut",
              icon = Icons.Default.Person,
              isSelected = mannequinView == MannequinView.SURFACE,
              onClick = {
                mannequinView = MannequinView.SURFACE
                selectedOrgan = null
              }
            )
            GenderToggleButton(
              title = "Scan",
              icon = Icons.Outlined.MonitorHeart,
              isSelected = mannequinView == MannequinView.SCAN,
              onClick = { mannequinView = MannequinView.SCAN }
            )
          }
        }
      }

      if (selectedOrgan != null) {
        Spacer(modifier = Modifier.height(6.dp))
        DnaBadge(
          text = selectedOrgan!!.displayName,
          tone = if (characterInjuries.any { it.organ == selectedOrgan }) DnaBadgeTone.DANGER else DnaBadgeTone.ICE,
          showDot = true
        )
      }

      Spacer(modifier = Modifier.height(6.dp))
      
      // Hier wurde vorher die Kachel-Matrix gezeichnet. 
      // Jetzt binden wir den CharacterMannequin (3D/Canvas Renderer) ein, wie vom User gewünscht.
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(300.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(DnaColors.SurfaceContainerHighest),
        contentAlignment = Alignment.Center
      ) {
        CharacterMannequin(
          gender = currentGender,
          injuries = characterInjuries,
          selectedBodyPart = selectedBodyPart,
          onBodyPartSelected = { part ->
            selectedOrgan = null
            selectedBodyPart = if (selectedBodyPart == part) null else part
          },
          view = mannequinView,
          selectedOrgan = selectedOrgan,
          onOrganSelected = { organ ->
            if (selectedOrgan == organ) {
              selectedOrgan = null
              selectedBodyPart = null
            } else {
              selectedOrgan = organ
              selectedBodyPart = organ.region
            }
          },
          modifier = Modifier.fillMaxHeight().width(250.dp) // Begrenzte Breite um Streckung/Ovale zu vermeiden
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 3. Wund-Inspektor & Behandlungsliste
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = DnaColors.SurfaceContainerHigh,
        border = BorderStroke(1.dp, DnaColors.Border),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          val displayedInjuries = when {
            selectedOrgan != null -> characterInjuries.filter { it.organ == selectedOrgan }
            selectedBodyPart != null -> characterInjuries.filter { it.bodyPart == selectedBodyPart }
            else -> characterInjuries
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = when {
                selectedOrgan != null -> "WUNDEN: ${selectedOrgan!!.displayName.uppercase()}"
                selectedBodyPart != null -> "WUNDEN: ${selectedBodyPart!!.displayName.uppercase()}"
                else -> "VERLETZUNGEN (${characterInjuries.size})"
              },
              fontFamily = DnaTypography.InterFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 11.sp,
              color = DnaColors.OnSurfaceVariant
            )

            if (displayedInjuries.isNotEmpty()) {
              DnaBadge(
                text = "${displayedInjuries.size} Aktiv",
                tone = DnaBadgeTone.DANGER,
                showDot = true
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          if (displayedInjuries.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = null,
                  tint = DnaColors.MintAccent,
                  modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = when {
                    selectedOrgan != null -> "Keine Verletzungen an ${selectedOrgan!!.displayName}."
                    selectedBodyPart != null -> "Keine Verletzungen an ${selectedBodyPart!!.displayName}."
                    conditionPenalty > 0 ->
                      "Keine Wunden — die Schwäche kommt von der Verfassung."
                    else -> "Keine Verletzungen."
                  },
                  fontFamily = DnaTypography.InterFamily,
                  fontSize = 12.sp,
                  color = DnaColors.OnSurfaceVariant
                )
              }
            }
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              displayedInjuries.forEach { injury ->
                InjuryItemCard(
                  injury = injury,
                  onHeal = {
                    val updated = characterInjuries.filterNot { it.id == injury.id }
                    onUpdateInjuries?.invoke(characterName, updated)
                  }
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          DnaButton(
            text = if (selectedBodyPart != null) {
              "Wunde an ${selectedBodyPart!!.displayName}"
            } else {
              "Verletzung eintragen"
            },
            onClick = { showAddInjuryDialog = true },
            variant = DnaButtonVariant.PRIMARY,
            fullWidth = true
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Aktuelle Ausruestung & Verfassung
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = DnaColors.SurfaceContainerHigh,
        border = BorderStroke(1.dp, DnaColors.Border),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = "OUTFIT",
            fontFamily = DnaTypography.InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = DnaColors.OnSurfaceVariant
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = characterOutfit,
            fontFamily = DnaTypography.InterFamily,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = DnaColors.OnSurface
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "VERFASSUNG",
            fontFamily = DnaTypography.InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = DnaColors.OnSurfaceVariant
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = characterCondition,
            fontFamily = DnaTypography.InterFamily,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = if (characterInjuries.isNotEmpty()) DnaColors.StatusDestructive else DnaColors.Secondary
          )

          if (characterMood.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "STIMMUNG",
              fontFamily = DnaTypography.InterFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 11.sp,
              color = DnaColors.OnSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = characterMood,
              fontFamily = DnaTypography.InterFamily,
              fontSize = 13.sp,
              lineHeight = 18.sp,
              color = DnaColors.OnSurface
            )
          }
        }
      }
    }
  }

  // Dialog zum Eintragen einer neuen Verletzung
  if (showAddInjuryDialog) {
    AddInjuryDialog(
      initialBodyPart = selectedBodyPart ?: BodyPart.CHEST,
      initialOrgan = selectedOrgan,
      gender = currentGender,
      onDismiss = { showAddInjuryDialog = false },
      onConfirm = { part, organ, description, severity ->
        val newInjury = CharacterInjury(
          characterName = characterName,
          bodyPart = part,
          description = description,
          severity = severity,
          organ = organ
        )
        val updated = characterInjuries + newInjury
        onUpdateInjuries?.invoke(characterName, updated)
        selectedBodyPart = part
        selectedOrgan = organ
        showAddInjuryDialog = false
      }
    )
  }
}

/**
 * Chip-Auswahlbutton fuer den aktiven Charakter in der Liste.
 */
@Composable
private fun CharacterSelectorChip(
  title: String,
  isSelected: Boolean,
  injuryCount: Int,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = if (isSelected) DnaColors.PrimaryContainer else DnaColors.SurfaceContainerHigh,
    border = BorderStroke(1.dp, if (isSelected) DnaColors.Primary else DnaColors.Border),
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .clickable { onClick() }
      .testTag("char_select_$title")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
      Text(
        text = title,
        fontFamily = DnaTypography.InterFamily,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        fontSize = 13.sp,
        color = if (isSelected) DnaColors.Primary else DnaColors.OnSurface
      )

      if (injuryCount > 0) {
        Spacer(modifier = Modifier.width(6.dp))
        Box(
          modifier = Modifier
            .size(8.dp)
            .background(DnaColors.StatusDestructive, CircleShape)
        )
      }
    }
  }
}

/**
 * Umschalter fuer das anatomische Geschlecht (Maennlich / Weiblich).
 */
@Composable
private fun GenderToggleButton(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = if (isSelected) DnaColors.SurfaceContainerHighest else Color.Transparent,
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .clickable { onClick() }
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (isSelected) DnaColors.Primary else DnaColors.OnSurfaceVariant,
        modifier = Modifier.size(14.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = title,
        fontFamily = DnaTypography.InterFamily,
        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        fontSize = 11.sp,
        color = if (isSelected) DnaColors.OnSurface else DnaColors.OnSurfaceVariant
      )
    }
  }
}

/**
 * Einzelkarte fuer eine Verletzung innerhalb des Inspektionsfensters.
 */
@Composable
private fun InjuryItemCard(
  injury: CharacterInjury,
  onHeal: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = DnaColors.SurfaceContainerHighest,
    border = BorderStroke(1.dp, DnaColors.StatusDestructive.copy(alpha = 0.35f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = injury.description,
          fontFamily = DnaTypography.InterFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 12.sp,
          color = DnaColors.OnSurface
        )
        Text(
          text = "Schwere: ${injury.severity.displayName}",
          fontFamily = DnaTypography.InterFamily,
          fontSize = 10.sp,
          color = DnaColors.StatusDestructive
        )
      }

      Surface(
        shape = CircleShape,
        color = DnaColors.SurfaceContainerLow,
        border = BorderStroke(1.dp, DnaColors.Border),
        modifier = Modifier
          .size(28.dp)
          .clip(CircleShape)
          .clickable { onHeal() }
      ) {
        Icon(
          imageVector = Icons.Default.Healing,
          contentDescription = "Wunde heilen",
          tint = DnaColors.Secondary,
          modifier = Modifier
            .padding(6.dp)
            .size(16.dp)
        )
      }
    }
  }
}

/**
 * Kompakte Wund-Uebersichtszeile in der Listenansicht.
 */
@Composable
private fun InjurySummaryRow(
  injury: CharacterInjury,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = DnaColors.SurfaceContainerHighest,
    border = BorderStroke(1.dp, DnaColors.Border),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(6.dp))
      .clickable { onClick() }
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(6.dp)
            .background(DnaColors.StatusDestructive, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = injury.bodyPart.displayName,
          fontFamily = DnaTypography.InterFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 11.sp,
          color = DnaColors.OnSurface
        )
      }

      Text(
        text = injury.severity.displayName,
        fontFamily = DnaTypography.InterFamily,
        fontSize = 10.sp,
        color = DnaColors.StatusWarning
      )
    }
  }
}

/**
 * Dialog zum Erfassen einer neuen koerperlichen Verletzung.
 */
@Composable
private fun AddInjuryDialog(
  initialBodyPart: BodyPart,
  initialOrgan: BodyOrgan?,
  gender: CharacterGender,
  onDismiss: () -> Unit,
  onConfirm: (bodyPart: BodyPart, organ: BodyOrgan?, description: String, severity: InjurySeverity) -> Unit
) {
  var selectedPart by remember { mutableStateOf(initialBodyPart) }
  var selectedOrganValue by remember { mutableStateOf(initialOrgan?.id ?: NO_ORGAN) }
  var description by remember { mutableStateOf("") }
  var selectedSeverity by remember { mutableStateOf(InjurySeverity.MEDIUM) }

  val bodyPartOptions = remember {
    BodyPart.entries.map {
      DnaDropdownOption(
        value = it,
        label = it.displayName
      )
    }
  }

  // Nur Organe der gewählten Region und des passenden Geschlechts - eine Gebärmutter unter
  // "Linker Arm" anzubieten wäre schlicht falsch.
  val organOptions = remember(selectedPart, gender) {
    listOf(DnaDropdownOption(value = NO_ORGAN, label = "Keines")) +
      BodyOrgan.forGender(gender)
        .filter { it.region == selectedPart }
        .map { DnaDropdownOption(value = it.id, label = it.displayName) }
  }

  // Wechselt die Region, passt das bisher gewählte Organ meist nicht mehr dazu.
  if (organOptions.none { it.value == selectedOrganValue }) {
    selectedOrganValue = NO_ORGAN
  }

  val severityOptions = remember {
    InjurySeverity.entries.map {
      DnaDropdownOption(
        value = it,
        label = it.displayName,
        hint = it.impactText
      )
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = DnaColors.SurfaceContainerHigh,
      border = BorderStroke(1.dp, DnaColors.Border),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("add_injury_dialog")
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = DnaColors.StatusDestructive,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Neue Verletzung",
            fontFamily = DnaTypography.ManropeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = DnaColors.OnSurface
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Koerperteil Dropdown
        DnaDropdown(
          label = "REGION",
          options = bodyPartOptions,
          selectedValue = selectedPart,
          onOptionSelected = { selectedPart = it },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Organ Dropdown - nur befüllt, wenn die Region überhaupt Organe enthält
        if (organOptions.size > 1) {
          DnaDropdown(
            label = "ORGAN",
            options = organOptions,
            selectedValue = selectedOrganValue,
            onOptionSelected = { selectedOrganValue = it },
            modifier = Modifier.fillMaxWidth().testTag("injury_organ_selector")
          )

          Spacer(modifier = Modifier.height(10.dp))
        }

        // Schweregrad Dropdown
        DnaDropdown(
          label = "SCHWEREGRAD",
          options = severityOptions,
          selectedValue = selectedSeverity,
          onOptionSelected = { selectedSeverity = it },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Beschreibung Textfeld
        Text(
          text = "BESCHREIBUNG",
          fontFamily = DnaTypography.InterFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 12.sp,
          color = DnaColors.OnSurfaceVariant,
          modifier = Modifier.padding(bottom = 6.dp)
        )

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          placeholder = {
            Text(
              text = "z. B. tiefe Schnittwunde",
              fontFamily = DnaTypography.InterFamily,
              fontSize = 13.sp,
              color = DnaColors.OnSurfaceVariant
            )
          },
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = DnaColors.OnSurface,
            unfocusedTextColor = DnaColors.OnSurface,
            focusedBorderColor = DnaColors.Primary,
            unfocusedBorderColor = DnaColors.Border,
            focusedContainerColor = DnaColors.SurfaceContainerLowest,
            unfocusedContainerColor = DnaColors.SurfaceContainerLowest
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          DnaButton(
            text = "Abbrechen",
            onClick = onDismiss,
            variant = DnaButtonVariant.SECONDARY,
            modifier = Modifier.padding(end = 8.dp)
          )

          DnaButton(
            text = "Speichern",
            onClick = {
              val organ = BodyOrgan.fromString(selectedOrganValue)
              val desc = description.ifBlank {
                "Wunde an ${organ?.displayName ?: selectedPart.displayName}"
              }
              onConfirm(selectedPart, organ, desc, selectedSeverity)
            },
            variant = DnaButtonVariant.PRIMARY
          )
        }
      }
    }
  }
}
