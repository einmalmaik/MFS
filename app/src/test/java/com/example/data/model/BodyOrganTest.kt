package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Sichert die Organkarte ab: Zuordnung aus Freitext, Geschlechtsbindung und die Lage der
 * Koordinaten innerhalb der Silhouette.
 *
 * Robolectric, weil [CharacterInjury.toJson] echtes org.json braucht.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BodyOrganTest {

  // --- Zuordnung aus dem Text der Extraktion ---

  @Test
  fun `erkennt Organe aus deutschem Freitext`() {
    assertEquals(BodyOrgan.STOMACH, BodyOrgan.fromString("Magen"))
    assertEquals(BodyOrgan.SPLEEN, BodyOrgan.fromString("Milz"))
    assertEquals(BodyOrgan.INTESTINES, BodyOrgan.fromString("Darm"))
    assertEquals(BodyOrgan.HEART, BodyOrgan.fromString("Herz"))
    assertEquals(BodyOrgan.BRAIN, BodyOrgan.fromString("Gehirn"))
    assertEquals(BodyOrgan.UTERUS, BodyOrgan.fromString("Gebärmutter"))
    assertEquals(BodyOrgan.UTERUS, BodyOrgan.fromString("Uterus"))
  }

  @Test
  fun `erkennt Organe aus einer Wundbeschreibung`() {
    assertEquals(BodyOrgan.LIVER, BodyOrgan.fromString("Tiefe Stichwunde in die Leber"))
    assertEquals(BodyOrgan.LEFT_LUNG, BodyOrgan.fromString("Kollabierte linke Lunge"))
  }

  @Test
  fun `unterscheidet Seiten am Wort links`() {
    assertEquals(BodyOrgan.LEFT_KIDNEY, BodyOrgan.fromString("linke Niere"))
    assertEquals(BodyOrgan.RIGHT_KIDNEY, BodyOrgan.fromString("rechte Niere"))
    assertEquals(BodyOrgan.LEFT_TESTICLE, BodyOrgan.fromString("linker Hoden"))
  }

  @Test
  fun `erkennt die Ids aus dem Extraktionsschema`() {
    BodyOrgan.entries.forEach { organ ->
      assertEquals("Id ${organ.id} wurde nicht erkannt", organ, BodyOrgan.fromString(organ.id))
    }
  }

  @Test
  fun `liefert null statt zu raten`() {
    assertNull(BodyOrgan.fromString(""))
    assertNull(BodyOrgan.fromString("Schnittwunde am Unterarm"))
  }

  // --- Geschlechtsbindung: Grundlage der Darstellung ---

  @Test
  fun `Gebaermutter und Eierstoecke gibt es nur bei weiblicher Anatomie`() {
    val male = BodyOrgan.forGender(CharacterGender.MALE)
    assertFalse(male.contains(BodyOrgan.UTERUS))
    assertFalse(male.contains(BodyOrgan.LEFT_OVARY))
    assertFalse(male.contains(BodyOrgan.RIGHT_OVARY))

    val female = BodyOrgan.forGender(CharacterGender.FEMALE)
    assertTrue(female.contains(BodyOrgan.UTERUS))
    assertTrue(female.contains(BodyOrgan.LEFT_OVARY))
  }

  @Test
  fun `Hoden gibt es nur bei maennlicher Anatomie`() {
    assertFalse(BodyOrgan.forGender(CharacterGender.FEMALE).contains(BodyOrgan.LEFT_TESTICLE))
    assertTrue(BodyOrgan.forGender(CharacterGender.MALE).contains(BodyOrgan.RIGHT_TESTICLE))
  }

  @Test
  fun `geschlechtsneutrale Organe gibt es bei beiden`() {
    listOf(BodyOrgan.HEART, BodyOrgan.STOMACH, BodyOrgan.INTESTINES, BodyOrgan.BRAIN, BodyOrgan.SPLEEN)
      .forEach {
        assertTrue("${it.displayName} fehlt bei MALE", it.existsFor(CharacterGender.MALE))
        assertTrue("${it.displayName} fehlt bei FEMALE", it.existsFor(CharacterGender.FEMALE))
      }
  }

  // --- Lage ---

  @Test
  fun `alle Koordinaten liegen innerhalb der Zeichenflaeche`() {
    BodyOrgan.entries.forEach {
      assertTrue("${it.displayName} liegt quer daneben", it.xPercent in 0.05f..0.95f)
      assertTrue("${it.displayName} liegt hoch daneben", it.yPercent in 0.02f..0.98f)
      assertTrue("${it.displayName} hat keine Ausdehnung", it.widthPercent > 0f && it.heightPercent > 0f)
    }
  }

  @Test
  fun `jedes Organ liegt in der zu seiner Region passenden Hoehe`() {
    // Die Region verbindet Organe mit dem bestehenden Verletzungssystem; stimmt die Höhe nicht,
    // erscheint die Wunde im Bild an einer anderen Stelle als in der Liste.
    //
    // BodyPart kennt nur einen Mittelpunkt, keine Ausdehnung. Der Bauch reicht auf der
    // Silhouette von 0.39 bis 0.545, ein Organ darf also rund 0.08 über seinem Regionsmittel-
    // punkt liegen (Harnblase) — 0.15 ist die Grenze, ab der es in die Nachbarregion rutschte.
    BodyOrgan.entries.forEach {
      val abstand = kotlin.math.abs(it.yPercent - it.region.yPercent)
      assertTrue(
        "${it.displayName} liegt $abstand von seiner Region ${it.region.displayName} entfernt",
        abstand < 0.15f
      )
    }
  }

  @Test
  fun `die Leber liegt auf der anderen Seite als Magen und Milz`() {
    // Sicht von vorn: Die Leber des Charakters liegt auf seiner rechten Seite, also links im Bild.
    assertTrue(BodyOrgan.LIVER.xPercent < 0.5f)
    assertTrue(BodyOrgan.STOMACH.xPercent > 0.5f)
    assertTrue(BodyOrgan.SPLEEN.xPercent > 0.5f)
  }

  @Test
  fun `die Reihenfolge legt hintere Organe zuerst`() {
    val sorted = BodyOrgan.forGender(CharacterGender.MALE)
    val lunge = sorted.indexOf(BodyOrgan.LEFT_LUNG)
    val herz = sorted.indexOf(BodyOrgan.HEART)
    assertTrue("Das Herz muss vor der Lunge gezeichnet werden", lunge < herz)
  }

  // --- Verbindung zur Verletzung ---

  @Test
  fun `eine Verletzung uebernimmt das Organ aus ihrer Beschreibung`() {
    val injury = CharacterInjury(
      bodyPart = BodyPart.ABDOMEN,
      description = "Durchschuss der linken Niere"
    ).let { original ->
      CharacterInjury.fromJson(original.toJson().put("description", original.description))
    }

    assertNotNull(injury.organ)
    assertEquals(BodyOrgan.LEFT_KIDNEY, injury.organ)
  }

  @Test
  fun `ein ausdruecklich genanntes Organ schlaegt die Beschreibung`() {
    val json = CharacterInjury(bodyPart = BodyPart.CHEST, description = "Trauma")
      .toJson()
      .put("organ", "HEART")

    assertEquals(BodyOrgan.HEART, CharacterInjury.fromJson(json).organ)
  }

  @Test
  fun `eine aeussere Wunde bekommt kein Organ untergeschoben`() {
    val json = CharacterInjury(
      bodyPart = BodyPart.LEFT_ARM,
      description = "Schürfwunde am Ellenbogen"
    ).toJson()

    assertNull(CharacterInjury.fromJson(json).organ)
  }
}
