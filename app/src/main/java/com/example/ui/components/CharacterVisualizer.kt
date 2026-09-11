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
  val displayName = if (isPlayer) "Du (Hauptcharakter)" else (currentNpc?.name ?: "NPC")

  val currentGender = characterGenders.getOrPut(characterName) {
    // Heuristik fuer weibliche Namen
    val nameLower = characterName.lowercase()
    if (nameLower.endsWith("a") || nameLower.endsWith("e") || nameLower.contains("frau") || nameLower.contains("elena") || nameLower.contains("sarah")) {
      CharacterGender.FEMALE
    } else {
      CharacterGender.MALE
    }
  }

  val characterOutfit = if (isPlayer) {
    checkpoint?.playerOutfit?.ifBlank { "Standard-Reisekleidung" } ?: "Standard-Reisekleidung"
  } else {
    currentNpc?.outfit?.ifBlank { "Passende Zivilkleidung" } ?: "Passende Zivilkleidung"
  }

  val characterCondition = if (isPlayer) {
    checkpoint?.playerCondition?.ifBlank { "Unverletzt" } ?: "Unverletzt"
  } else {
    currentNpc?.currentMood ?: "Ruhig"
  }

  // Verletzungen fuer den aktuell gewaehlten Charakter
  val characterInjuries = remember(checkpoint?.rawStateJson, checkpoint?.playerCondition, characterName) {
    checkpoint?.getCharacterInjuries(characterName) ?: emptyList()
  }

  var selectedBodyPart by remember { mutableStateOf<BodyPart?>(null) }
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
            text = "Charakter & Anatomie",
            fontFamily = DnaTypography.ManropeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = DnaColors.OnSurface
          )
        }

        if (adultContentEnabled && isAdultOutfit) {
          DnaBadge(
            text = "Adult / Freizügig",
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
        text = "AKTIVER CHARAKTER",
        fontFamily = DnaTypography.InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp,
        color = DnaColors.OnSurfaceVariant
      )

      Spacer(modifier = Modifier.height(6.dp))

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
          injuryCount = checkpoint?.getCharacterInjuries("Du")?.size ?: 0,
          onClick = {
            selectedCharacterIndex = 0
            selectedBodyPart = null
          }
        )

        // Alle NPCs der Story
        npcs.forEachIndexed { index, npc ->
          val npcInjuries = checkpoint?.getCharacterInjuries(npc.name)?.size ?: 0
          CharacterSelectorChip(
            title = npc.name,
            isSelected = selectedCharacterIndex == index + 1,
            injuryCount = npcInjuries,
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

      // Vitalitätsberechnung anhand aktiver Verletzungen
      val totalPenalty = characterInjuries.sumOf {
        when (it.severity) {
          InjurySeverity.LIGHT -> 5
          InjurySeverity.MEDIUM -> 15
          InjurySeverity.SEVERE -> 25
          InjurySeverity.CRITICAL -> 40
        }
      }
      val vitalityPercent = (100 - totalPenalty).coerceIn(5, 100)
      val vitalityColor = when {
        vitalityPercent >= 80 -> DnaColors.MintAccent
        vitalityPercent >= 50 -> DnaColors.StatusWarning
        else -> DnaColors.StatusDestructive
      }
      val conditionTitle = when {
        vitalityPercent == 100 -> "Vollständig einsatzbereit"
        vitalityPercent >= 80 -> "Leicht angeschlagen ($vitalityPercent%)"
        vitalityPercent >= 50 -> "Verwundet ($vitalityPercent%)"
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
                text = "Unverletzt",
                tone = DnaBadgeTone.MINT,
                showDot = false
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

      // 2. Anatomische Körperregionen-Matrix (Touch-freundliche Kacheln statt verzerrtem Canvas)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "KÖRPERREGIONEN",
          fontFamily = DnaTypography.InterFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 11.sp,
          color = DnaColors.OnSurfaceVariant
        )

        if (selectedBodyPart != null) {
          Text(
            text = "Filter aufheben",
            fontFamily = DnaTypography.InterFamily,
            fontSize = 11.sp,
            color = DnaColors.Primary,
            modifier = Modifier
              .clickable { selectedBodyPart = null }
              .padding(4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Kacheln aller Regionen in 3 Spalten
      val regions = listOf(
        Pair(BodyPart.HEAD, "Kopf & Gesicht"),
        Pair(BodyPart.NECK, "Hals & Kehle"),
        Pair(BodyPart.CHEST, "Brust & Rippen"),
        Pair(BodyPart.ABDOMEN, "Bauch & Unterleib"),
        Pair(BodyPart.LEFT_ARM, "Linker Arm"),
        Pair(BodyPart.RIGHT_ARM, "Rechter Arm"),
        Pair(BodyPart.LEFT_LEG, "Linkes Bein"),
        Pair(BodyPart.RIGHT_LEG, "Rechtes Bein"),
        Pair(BodyPart.FEET, "Füße & Knöchel")
      )

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        regions.chunked(3).forEach { rowParts ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            rowParts.forEach { (part, label) ->
              val isSelected = selectedBodyPart == part
              val partInjuries = characterInjuries.filter { it.bodyPart == part }
              val hasInjuries = partInjuries.isNotEmpty()

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                  isSelected -> DnaColors.PrimaryContainer
                  hasInjuries -> DnaColors.StatusDestructive.copy(alpha = 0.12f)
                  else -> DnaColors.SurfaceContainerHigh
                },
                border = BorderStroke(
                  1.dp,
                  when {
                    isSelected -> DnaColors.Primary
                    hasInjuries -> DnaColors.StatusDestructive.copy(alpha = 0.6f)
                    else -> DnaColors.Border
                  }
                ),
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .clickable {
                    selectedBodyPart = if (isSelected) null else part
                  }
              ) {
                Column(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text(
                    text = label,
                    fontFamily = DnaTypography.InterFamily,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp,
                    color = when {
                      isSelected -> DnaColors.Primary
                      hasInjuries -> DnaColors.StatusDestructive
                      else -> DnaColors.OnSurface
                    },
                    maxLines = 1
                  )

                  Spacer(modifier = Modifier.height(2.dp))

                  if (hasInjuries) {
                    Text(
                      text = "${partInjuries.size} Wunde(n)",
                      fontFamily = DnaTypography.InterFamily,
                      fontSize = 9.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = DnaColors.StatusDestructive
                    )
                  } else {
                    Text(
                      text = "Unversehrt",
                      fontFamily = DnaTypography.InterFamily,
                      fontSize = 9.sp,
                      color = DnaColors.OnSurfaceVariant
                    )
                  }
                }
              }
            }
          }
        }
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
          val displayedInjuries = if (selectedBodyPart != null) {
            characterInjuries.filter { it.bodyPart == selectedBodyPart }
          } else {
            characterInjuries
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (selectedBodyPart != null) {
                "WUNDEN: ${selectedBodyPart!!.displayName.uppercase()}"
              } else {
                "ALLE VERLETZUNGEN (${characterInjuries.size})"
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
                  text = if (selectedBodyPart != null) {
                    "Keine Verletzungen an ${selectedBodyPart!!.displayName}."
                  } else {
                    "Keine aktiven Verletzungen. Vollständig einsatzbereit."
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
              "+ Wunde an ${selectedBodyPart!!.displayName} melden"
            } else {
              "+ Verletzung eintragen"
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
            text = "AKTUELLES OUTFIT / RÜSTUNG",
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
            text = "GESAMTZUSTAND",
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
            color = if (characterInjuries.isNotEmpty()) DnaColors.StatusDestructive else DnaColors.Secondary
          )
        }
      }
    }
  }

  // Dialog zum Eintragen einer neuen Verletzung
  if (showAddInjuryDialog) {
    AddInjuryDialog(
      initialBodyPart = selectedBodyPart ?: BodyPart.CHEST,
      onDismiss = { showAddInjuryDialog = false },
      onConfirm = { part, description, severity ->
        val newInjury = CharacterInjury(
          characterName = characterName,
          bodyPart = part,
          description = description,
          severity = severity
        )
        val updated = characterInjuries + newInjury
        onUpdateInjuries?.invoke(characterName, updated)
        selectedBodyPart = part
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
  onDismiss: () -> Unit,
  onConfirm: (bodyPart: BodyPart, description: String, severity: InjurySeverity) -> Unit
) {
  var selectedPart by remember { mutableStateOf(initialBodyPart) }
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
            text = "Neue Verletzung eintragen",
            fontFamily = DnaTypography.ManropeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = DnaColors.OnSurface
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Koerperteil Dropdown
        DnaDropdown(
          label = "KÖRPERREGION",
          options = bodyPartOptions,
          selectedValue = selectedPart,
          onOptionSelected = { selectedPart = it },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

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
          text = "BESCHREIBUNG DER WUNDE",
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
              text = "z. B. Tiefe Schnittwunde, Brandblasen, Pfeilspitze...",
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
            text = "Wunde speichern",
            onClick = {
              val desc = description.ifBlank { "Wunde an ${selectedPart.displayName}" }
              onConfirm(selectedPart, desc, selectedSeverity)
            },
            variant = DnaButtonVariant.PRIMARY
          )
        }
      }
    }
  }
}
