package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Anatomische Koerperregionen fuer das Verletzungs- und Ausruestungssystem.
 * Jede Region besitzt genaue normalisierte Koordinaten (xPercent, yPercent)
 * auf der menschlichen Silhouette.
 */
enum class BodyPart(
  val id: String,
  val displayName: String,
  val xPercent: Float,
  val yPercent: Float
) {
  HEAD("HEAD", "Kopf & Gesicht", 0.50f, 0.12f),
  NECK("NECK", "Hals & Kehle", 0.50f, 0.19f),
  CHEST("CHEST", "Brust & Rippen", 0.50f, 0.28f),
  ABDOMEN("ABDOMEN", "Bauch & Unterleib", 0.50f, 0.40f),
  GENITALS("GENITALS", "Intimbereich", 0.50f, 0.50f),
  LEFT_ARM("LEFT_ARM", "Linker Arm", 0.27f, 0.33f),
  RIGHT_ARM("RIGHT_ARM", "Rechter Arm", 0.73f, 0.33f),
  LEFT_HAND("LEFT_HAND", "Linke Hand", 0.20f, 0.48f),
  RIGHT_HAND("RIGHT_HAND", "Rechte Hand", 0.80f, 0.48f),
  LEFT_LEG("LEFT_LEG", "Linkes Bein", 0.41f, 0.65f),
  RIGHT_LEG("RIGHT_LEG", "Rechtes Bein", 0.59f, 0.65f),
  FEET("FEET", "Füße & Knöchel", 0.50f, 0.90f);

  companion object {
    /**
     * Findet die passende Koerperregion anhand einer ID oder eines Freitextes.
     */
    fun fromString(value: String): BodyPart {
      val clean = value.uppercase().trim()
      entries.find { it.name == clean || it.id == clean }?.let { return it }

      return when {
        clean.contains("KOPF") || clean.contains("GESICHT") || clean.contains("AUGE") || clean.contains("STIRN") -> HEAD
        clean.contains("HALS") || clean.contains("NACKEN") || clean.contains("KEHLE") -> NECK
        clean.contains("BRUST") || clean.contains("RIPPE") || clean.contains("HERZ") -> CHEST
        clean.contains("BAUCH") || clean.contains("MAGEN") || clean.contains("HÜFTE") || clean.contains("LEIB") -> ABDOMEN
        clean.contains("INTIM") || clean.contains("GENITAL") || clean.contains("PENIS") || clean.contains("SCHRITT") || clean.contains("HODEN") -> GENITALS
        clean.contains("HAND") && (clean.contains("LINKS") || clean.contains("LINK")) -> LEFT_HAND
        clean.contains("HAND") -> RIGHT_HAND
        clean.contains("ARM") && (clean.contains("LINKS") || clean.contains("LINK")) -> LEFT_ARM
        clean.contains("ARM") || clean.contains("SCHULTER") -> RIGHT_ARM
        clean.contains("BEIN") && (clean.contains("LINKS") || clean.contains("LINK")) -> LEFT_LEG
        clean.contains("BEIN") || clean.contains("SCHENKEL") || clean.contains("KNIE") -> RIGHT_LEG
        clean.contains("FUSS") || clean.contains("FÜSS") || clean.contains("KNÖCHEL") -> FEET
        else -> CHEST
      }
    }
  }
}

/**
 * Schweregrad einer koerperlichen Verletzung.
 */
enum class InjurySeverity(
  val displayName: String,
  val impactText: String
) {
  LIGHT("Leicht", "Oberflächlich, leichte Schmerzen"),
  MEDIUM("Mittelschwer", "Behindert Bewegung oder Handeln spürbar"),
  SEVERE("Schwer", "Starke Schmerzen, Blutung, deutliche Schwächung"),
  CRITICAL("Kritisch", "Lebensbedrohlich, erfordert sofortige Versorgung");

  companion object {
    fun fromString(value: String): InjurySeverity {
      val clean = value.uppercase().trim()
      return entries.find { it.name == clean } ?: when {
        clean.contains("KRITISCH") || clean.contains("TÖDLICH") -> CRITICAL
        clean.contains("SCHWER") || clean.contains("TIEF") -> SEVERE
        clean.contains("MITTEL") -> MEDIUM
        else -> LIGHT
      }
    }
  }
}

/**
 * Geschlecht des Charakters zur anatomischen Silhouetten-Darstellung.
 */
enum class CharacterGender(val displayName: String) {
  MALE("Männlich"),
  FEMALE("Weiblich");

  companion object {
    fun fromString(value: String): CharacterGender {
      val clean = value.lowercase().trim()
      return if (clean.contains("w") || clean.contains("female") || clean.contains("frau") || clean.contains("weiblich")) {
        FEMALE
      } else {
        MALE
      }
    }
  }
}

/**
 * Konkrete Verletzung an einem anatomischen Koerperteil.
 */
data class CharacterInjury(
  val id: String = UUID.randomUUID().toString(),
  val characterName: String = "Du",
  val bodyPart: BodyPart,
  val description: String,
  val severity: InjurySeverity = InjurySeverity.MEDIUM,
  val isTreated: Boolean = false
) {
  /**
   * Konvertiert die Verletzung in ein JSON-Objekt.
   */
  fun toJson(): JSONObject {
    val obj = JSONObject()
    obj.put("id", id)
    obj.put("character", characterName)
    obj.put("body_part", bodyPart.name)
    obj.put("description", description)
    obj.put("severity", severity.name)
    obj.put("is_treated", isTreated)
    return obj
  }

  companion object {
    /**
     * Parst eine Verletzung aus einem JSON-Objekt.
     */
    fun fromJson(obj: JSONObject): CharacterInjury {
      return CharacterInjury(
        id = obj.optString("id", UUID.randomUUID().toString()),
        characterName = obj.optString("character", "Du"),
        bodyPart = BodyPart.fromString(obj.optString("body_part", "CHEST")),
        description = obj.optString("description", "Verletzung"),
        severity = InjurySeverity.fromString(obj.optString("severity", "MEDIUM")),
        isTreated = obj.optBoolean("is_treated", false)
      )
    }

    /**
     * Parst eine Liste von Verletzungen aus einem JSON-Array oder Freitext.
     */
    fun parseListFromJson(jsonString: String): List<CharacterInjury> {
      val list = mutableListOf<CharacterInjury>()
      if (jsonString.isBlank()) return list

      try {
        if (jsonString.trim().startsWith("[")) {
          val array = JSONArray(jsonString)
          for (i in 0 until array.length()) {
            val item = array.optJSONObject(i)
            if (item != null) {
              list.add(fromJson(item))
            } else {
              val text = array.optString(i)
              if (text.isNotBlank()) {
                list.add(fromNaturalText(text, "Du"))
              }
            }
          }
        } else if (jsonString.trim().startsWith("{")) {
          val obj = JSONObject(jsonString)
          val injuriesArray = obj.optJSONArray("injuries")
          if (injuriesArray != null) {
            for (i in 0 until injuriesArray.length()) {
              injuriesArray.optJSONObject(i)?.let { list.add(fromJson(it)) }
            }
          }
        }
      } catch (_: Exception) { }

      return list
    }

    /**
     * Erzeugt eine Verletzung aus einer natuerlichen Zustandsbeschreibung wie
     * "Tiefe Schnittwunde an der rechten Schulter".
     */
    fun fromNaturalText(text: String, characterName: String): CharacterInjury {
      val part = BodyPart.fromString(text)
      val severity = when {
        text.contains("kritisch", ignoreCase = true) || text.contains("schwer", ignoreCase = true) || text.contains("tief", ignoreCase = true) -> InjurySeverity.SEVERE
        text.contains("leicht", ignoreCase = true) || text.contains("kratzer", ignoreCase = true) || text.contains("schürfwunde", ignoreCase = true) -> InjurySeverity.LIGHT
        else -> InjurySeverity.MEDIUM
      }
      return CharacterInjury(
        characterName = characterName,
        bodyPart = part,
        description = text.trim(),
        severity = severity,
        isTreated = text.contains("verbunden", ignoreCase = true) || text.contains("bandagiert", ignoreCase = true)
      )
    }
  }
}
