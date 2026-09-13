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
 * Innere Organe mit normalisierten Koordinaten auf derselben Silhouette wie [BodyPart].
 *
 * Die Koordinaten sind Anteile der Zeichenflaeche (0..1) und bewusst unabhaengig von der
 * konkreten Darstellung gehalten: Wird spaeter eine MRT-Bildvorlage untergelegt, bleiben sie
 * unveraendert gueltig.
 *
 * Seitenangaben folgen der Sicht auf die Figur von vorn. Die Leber des Charakters liegt auf
 * seiner rechten Seite und erscheint deshalb links im Bild.
 *
 * @param region Die Koerperregion, zu der dieses Organ gehoert - verbindet Organe mit dem
 *   bestehenden Verletzungssystem.
 * @param restrictedTo Nur bei diesem Geschlecht vorhanden; null = bei beiden.
 * @param widthPercent Ausdehnung quer, als Anteil der Breite.
 * @param heightPercent Ausdehnung hoch, als Anteil der Hoehe.
 * @param depth Zeichenreihenfolge: kleinere Werte liegen weiter hinten.
 */
enum class BodyOrgan(
  val id: String,
  val displayName: String,
  val xPercent: Float,
  val yPercent: Float,
  val widthPercent: Float,
  val heightPercent: Float,
  val region: BodyPart,
  val restrictedTo: CharacterGender? = null,
  val depth: Int = 1
) {
  BRAIN("BRAIN", "Gehirn", 0.500f, 0.098f, 0.090f, 0.052f, BodyPart.HEAD, depth = 0),
  LEFT_EAR("LEFT_EAR", "Linkes Ohr", 0.434f, 0.126f, 0.020f, 0.026f, BodyPart.HEAD, depth = 2),
  RIGHT_EAR("RIGHT_EAR", "Rechtes Ohr", 0.566f, 0.126f, 0.020f, 0.026f, BodyPart.HEAD, depth = 2),
  NOSE("NOSE", "Nase", 0.500f, 0.140f, 0.022f, 0.020f, BodyPart.HEAD, depth = 2),

  LEFT_LUNG("LEFT_LUNG", "Linker Lungenfluegel", 0.432f, 0.302f, 0.062f, 0.078f, BodyPart.CHEST, depth = 0),
  RIGHT_LUNG("RIGHT_LUNG", "Rechter Lungenfluegel", 0.568f, 0.302f, 0.062f, 0.078f, BodyPart.CHEST, depth = 0),
  HEART("HEART", "Herz", 0.470f, 0.316f, 0.054f, 0.058f, BodyPart.CHEST, depth = 2),

  LIVER("LIVER", "Leber", 0.444f, 0.402f, 0.080f, 0.048f, BodyPart.ABDOMEN, depth = 2),
  STOMACH("STOMACH", "Magen", 0.552f, 0.406f, 0.062f, 0.044f, BodyPart.ABDOMEN, depth = 2),
  SPLEEN("SPLEEN", "Milz", 0.590f, 0.396f, 0.032f, 0.030f, BodyPart.ABDOMEN, depth = 1),
  LEFT_KIDNEY("LEFT_KIDNEY", "Linke Niere", 0.452f, 0.448f, 0.030f, 0.038f, BodyPart.ABDOMEN, depth = 0),
  RIGHT_KIDNEY("RIGHT_KIDNEY", "Rechte Niere", 0.548f, 0.448f, 0.030f, 0.038f, BodyPart.ABDOMEN, depth = 0),
  INTESTINES("INTESTINES", "Darm", 0.500f, 0.482f, 0.120f, 0.072f, BodyPart.ABDOMEN, depth = 3),
  BLADDER("BLADDER", "Harnblase", 0.500f, 0.532f, 0.046f, 0.030f, BodyPart.ABDOMEN, depth = 2),

  UTERUS("UTERUS", "Gebaermutter", 0.500f, 0.514f, 0.044f, 0.034f, BodyPart.ABDOMEN, CharacterGender.FEMALE, depth = 1),
  LEFT_OVARY("LEFT_OVARY", "Linker Eierstock", 0.452f, 0.510f, 0.020f, 0.018f, BodyPart.ABDOMEN, CharacterGender.FEMALE, depth = 1),
  RIGHT_OVARY("RIGHT_OVARY", "Rechter Eierstock", 0.548f, 0.510f, 0.020f, 0.018f, BodyPart.ABDOMEN, CharacterGender.FEMALE, depth = 1),

  LEFT_TESTICLE("LEFT_TESTICLE", "Linker Hoden", 0.480f, 0.574f, 0.022f, 0.024f, BodyPart.GENITALS, CharacterGender.MALE, depth = 2),
  RIGHT_TESTICLE("RIGHT_TESTICLE", "Rechter Hoden", 0.520f, 0.574f, 0.022f, 0.024f, BodyPart.GENITALS, CharacterGender.MALE, depth = 2);

  fun existsFor(gender: CharacterGender): Boolean = restrictedTo == null || restrictedTo == gender

  companion object {
    /** Alle Organe, die es bei diesem Geschlecht gibt, von hinten nach vorn sortiert. */
    fun forGender(gender: CharacterGender): List<BodyOrgan> =
      entries.filter { it.existsFor(gender) }.sortedBy { it.depth }

    /**
     * Findet ein Organ anhand einer Id oder eines deutschen Freitextes.
     * Folgt demselben Muster wie [BodyPart.fromString]: exakte Treffer zuerst, dann Synonyme.
     */
    fun fromString(value: String): BodyOrgan? {
      val clean = value.uppercase().trim()
      if (clean.isBlank()) return null
      entries.find { it.name == clean || it.id == clean }?.let { return it }

      val links = clean.contains("LINK")
      return when {
        clean.contains("GEHIRN") || clean.contains("HIRN") -> BRAIN
        clean.contains("OHR") -> if (links) LEFT_EAR else RIGHT_EAR
        clean.contains("NASE") -> NOSE
        clean.contains("HERZ") -> HEART
        clean.contains("LUNGE") || clean.contains("LUNGEN") -> if (links) LEFT_LUNG else RIGHT_LUNG
        clean.contains("MAGEN") -> STOMACH
        clean.contains("LEBER") -> LIVER
        clean.contains("MILZ") -> SPLEEN
        clean.contains("NIERE") -> if (links) LEFT_KIDNEY else RIGHT_KIDNEY
        clean.contains("DARM") || clean.contains("GEDAERM") -> INTESTINES
        clean.contains("BLASE") -> BLADDER
        clean.contains("GEBAERMUTTER") || clean.contains("GEBÄRMUTTER") || clean.contains("UTERUS") -> UTERUS
        clean.contains("EIERSTOCK") || clean.contains("OVAR") -> if (links) LEFT_OVARY else RIGHT_OVARY
        clean.contains("HODEN") || clean.contains("TESTIKEL") -> if (links) LEFT_TESTICLE else RIGHT_TESTICLE
        else -> null
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
  val isTreated: Boolean = false,
  /** Betroffenes inneres Organ, sofern die Verletzung eines trifft. */
  val organ: BodyOrgan? = null,
  /**
   * Wie viele Spieltage diese Wunde schon alt ist. Die Heilung in der StateExtractionEngine
   * rechnet damit, und nur sie zählt hoch -- die KI wird nie danach gefragt.
   *
   * Das Feld gehört zum Modell und nicht nur ins JSON, weil jede Runde über `fromJson`/`toJson`
   * läuft. Fehlt es hier, ist die Wunde nach jedem Durchlauf wieder null Tage alt und heilt nie,
   * egal wie viele Tage der Spieler verstreichen lässt.
   */
  val daysElapsed: Int = 0
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
    obj.put("days_elapsed", daysElapsed)
    if (organ != null) obj.put("organ", organ.id)
    return obj
  }

  companion object {
    /**
     * Parst eine Verletzung aus einem JSON-Objekt.
     */
    fun fromJson(obj: JSONObject): CharacterInjury {
      val description = obj.optString("description", "Verletzung")
      return CharacterInjury(
        id = obj.optString("id", UUID.randomUUID().toString()),
        characterName = obj.optString("character", "Du"),
        bodyPart = BodyPart.fromString(obj.optString("body_part", "CHEST")),
        description = description,
        severity = InjurySeverity.fromString(obj.optString("severity", "MEDIUM")),
        isTreated = obj.optBoolean("is_treated", false),
        // Nennt die Extraktion kein Organ, wird es aus der Wundbeschreibung abgeleitet -
        // "Stichwunde in die Leber" landet so trotzdem an der richtigen Stelle.
        organ = BodyOrgan.fromString(obj.optString("organ"))
          ?: BodyOrgan.fromString(description),
        daysElapsed = obj.optInt("days_elapsed", 0)
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
