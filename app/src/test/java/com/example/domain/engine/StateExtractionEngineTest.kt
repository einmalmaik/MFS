package com.example.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.data.api.GeminiClient
import com.example.data.db.StoryDao

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StateExtractionEngineTest {
  
  // We'll use simple reflection to just test the method directly instead of relying on Mockito 
  // since Mockito is not in the dependencies.
  @Test
  fun `test deterministic time progression without skips`() {
    val result = invokeTimeProgression(
      "Tag 1, 20:00 Uhr",
      "Tag 1, 20:00 Uhr",
      "Ich gehe durch die Tür.",
      "Du gehst durch die Tür in den nächsten Raum."
    )
    assertEquals("Tag 1, 20:00 Uhr", result)
  }

  @Test
  fun `test deterministic time progression with explicit days skip in action`() {
    val result = invokeTimeProgression(
      "Tag 1, 20:00 Uhr",
      "Tag 1, 20:00 Uhr", // Simulation: Model failed to advance time
      "Es vergehen zwei Tage.",
      "Zwei Tage verstreichen ohne weitere Vorkommnisse."
    )
    // The engine should auto-correct to target day (Day 3) and default to 20:00
    assertEquals("Tag 3, 20:00 Uhr", result)
  }
  
  @Test
  fun `test deterministic time progression with next morning skip`() {
    val result = invokeTimeProgression(
      "Tag 1, 22:00 Uhr",
      "Tag 1, 08:00 Uhr", // Model got the time right but missed the day switch
      "Wir schlafen bis zum nächsten Morgen.",
      "Die Nacht vergeht ruhig. Am nächsten Morgen wacht ihr auf."
    )
    assertEquals("Tag 2, 08:00 Uhr", result)
  }

  @Test
  fun `test model correctly advances time itself`() {
    val result = invokeTimeProgression(
      "Tag 2, 14:00 Uhr",
      "Tag 5, 12:00 Uhr", // Model correctly skipped 3 days
      "Drei Tage später treffen wir uns wieder.",
      "Nach drei Tagen am vereinbarten Ort..."
    )
    assertEquals("Tag 5, 12:00 Uhr", result)
  }

  // Bis hierhin prüft jeder Test dieselbe Richtung: Die Uhr geht zu langsam vor. Die Gegenrichtung
  // war ungeprüft -- und ungeprüft ging sie ungebremst in den Spielstand.

  @Test
  fun `ein Rueckschritt am Tag wird nicht uebernommen`() {
    val result = invokeTimeProgression(
      "Tag 5, 14:00 Uhr",
      "Tag 3, 09:00 Uhr", // Rückblende, verlesene Zahl -- so oder so: die Uhr lief rückwärts
      "Ich erinnere mich an den Überfall.",
      "In deiner Erinnerung stehst du wieder am Hafen."
    )
    assertEquals("Tag 5, 09:00 Uhr", result)
  }

  @Test
  fun `der Tag bleibt stehen, die Tageszeit laeuft weiter`() {
    // Ein reiner Rückfall auf previousTime würde die Uhr anhalten, sobald das Modell einmal irrt.
    val result = invokeTimeProgression(
      "Tag 4, 08:00 Uhr",
      "Tag 1, 21:30 Uhr",
      "Ich gehe weiter.",
      "Der Abend bricht herein."
    )
    assertEquals("Tag 4, 21:30 Uhr", result)
  }

  @Test
  fun `ohne erkennbare Tageszeit gilt die vorherige`() {
    val result = invokeTimeProgression(
      "Tag 6, 17:45 Uhr",
      "Tag 2",
      "Ich sehe mich um.",
      "Der Platz liegt still."
    )
    assertEquals("Tag 6, 17:45 Uhr", result)
  }

  @Test
  fun `ein angeforderter Sprung schlaegt den Rueckwaerts-Schutz`() {
    // Reihenfolge zählt: Erst die Sprung-Korrektur, dann der Schutz. Sonst bliebe der Spieler
    // auf Tag 2 stehen, obwohl er ausdrücklich zwei Tage verstreichen lassen wollte.
    val result = invokeTimeProgression(
      "Tag 2, 10:00 Uhr",
      "Tag 1, 10:00 Uhr",
      "Es vergehen zwei Tage.",
      "Zwei Tage ziehen ins Land."
    )
    assertEquals("Tag 4, 10:00 Uhr", result)
  }

  @Test
  fun `innerhalb eines Tages bleibt die Tageszeit unangetastet`() {
    // Bewusste Grenze: Innerhalb desselben Tages kann eine frühere Uhrzeit eine gleichzeitig
    // erzählte Szene sein. Geschützt wird der Tag, nicht die Minute.
    val result = invokeTimeProgression(
      "Tag 3, 18:00 Uhr",
      "Tag 3, 16:00 Uhr",
      "Was geschah währenddessen bei Lena?",
      "Zur selben Stunde, zwei Straßen weiter..."
    )
    assertEquals("Tag 3, 16:00 Uhr", result)
  }

  // --- ERSTER ZUG ---
  //
  // Eine frisch angelegte Geschichte hat keinen Zeitstempel. Früher stand dort "Tag 1, 20:00 Uhr",
  // und jede Geschichte begann abends um acht -- auch die, deren Prompt von einem Morgen erzählte.

  @Test
  fun `der erste Zug uebernimmt die Uhrzeit aus der Erzaehlung`() {
    val result = invokeTimeProgression(
      "", // frisch angelegt: noch keine Spielzeit
      "Tag 1, 02:00 Uhr",
      "Sonntag, zwei Uhr nachts. Ich schließe die Kneipe ab.",
      "Der Riegel rastet ein. Die Straße liegt leer."
    )
    assertEquals("Tag 1, 02:00 Uhr", result)
  }

  @Test
  fun `Vorgeschichte verschiebt den ersten Tag nicht`() {
    // Der Kern der Sache: "vor einer Woche hergezogen" ist Prolog, keine verstrichene Spielzeit.
    // Ohne diese Klammer begann die Geschichte an Tag 8 -- in ihrer eigenen Zukunft.
    val result = invokeTimeProgression(
      "",
      "Tag 8, 19:00 Uhr", // das Modell hat die Woche mitgerechnet
      "Ich bin vor einer Woche in diese Stadt gezogen und packe die letzte Kiste aus.",
      "Der Karton gibt nach, das Packband löst sich."
    )
    assertEquals("Tag 1, 19:00 Uhr", result)
  }

  @Test
  fun `auch ein Zeitsprung im ersten Zug beginnt an Tag 1`() {
    // Eine Geschichte kann nicht vor ihrem eigenen Anfang beginnen. Die Sprung-Korrektur darf
    // hier deshalb nicht greifen, obwohl "zwei Tage" in der Eingabe steht.
    val result = invokeTimeProgression(
      "",
      "Tag 3, 08:00 Uhr",
      "Es vergehen zwei Tage, dann breche ich auf.",
      "Zwei Tage später stehst du am Tor."
    )
    assertEquals("Tag 1, 08:00 Uhr", result)
  }

  @Test
  fun `der erste Zug uebernimmt die Uhrzeit aus der Eingabe wenn Extraktion leer ist`() {
    val result = invokeTimeProgression(
      "",
      "", // Modell hat in_game_time leer gelassen
      "Sonntag, zwei Uhr nachts. Ich schließe die Kneipe ab.",
      "Der Riegel rastet ein. Die Straße liegt leer."
    )
    assertEquals("Tag 1, 02:00 Uhr", result)
  }

  @Test
  fun `nennt der erste Zug keine Uhrzeit, bleibt es bei Tag 1 ohne Uhrzeit`() {
    // Keine erfundene Ersatzzeit: Lieber gar keine Uhrzeit als eine, die niemand gesagt hat.
    val result = invokeTimeProgression(
      "",
      "",
      "Ich sehe mich um.",
      "Staub liegt auf allem."
    )
    assertEquals("Tag 1", result)
  }

  @Test
  fun `ab dem zweiten Zug gilt wieder die normale Rechnung`() {
    // Die Klammer greift ausschließlich bei leerem previousTime -- danach zählt der Sprung.
    // Die Tageszeit stammt dabei aus der Extraktion (02:00 Uhr) und nicht aus dem 20-Uhr-Ersatz;
    // der gilt nur, wenn gar keine Uhrzeit erkennbar ist.
    val result = invokeTimeProgression(
      "Tag 1, 02:00 Uhr",
      "Tag 1, 02:00 Uhr",
      "Es vergehen zwei Tage.",
      "Zwei Tage ziehen vorbei."
    )
    assertEquals("Tag 3, 02:00 Uhr", result)
  }

  private fun invokeTimeProgression(
    previousTime: String,
    extractedTime: String,
    userAction: String,
    modelResponse: String
  ): String {
    return StateExtractionEngine.computeDeterministicTimeProgression(
      previousTime, extractedTime, userAction, modelResponse
    )
  }
}

