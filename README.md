# Maunting Story Fable (MSF)

Ein interaktives Text-Rollenspiel für Android. Die KI spielt den Spielleiter, du spielst die
Hauptfigur — und die Welt vergisst nicht, was gestern passiert ist.

MSF läuft vollständig auf deinem Gerät. Kein Konto, keine Anmeldung, kein Server von uns. Die
einzige Gegenstelle ist Googles Gemini-API, und die sprichst du mit deinem eigenen Schlüssel an.

> **Die Oberfläche ist auf Deutsch.** Eine Übersetzung gibt es derzeit nicht.

---

## Herunterladen

**[MauntingStoryFable.apk – neueste Fassung](https://github.com/einmalmaik/MFS/releases/latest/download/MauntingStoryFable.apk)**

Direkt vom Handy aus laden und öffnen. Android fragt einmal, ob du Installationen aus dieser
Quelle erlauben willst — das ist normal bei einer App außerhalb des Play Store.

**Voraussetzungen**

- Android 7.0 oder neuer
- Ein eigener Gemini-API-Schlüssel, kostenlos unter [aistudio.google.com/apikey](https://aistudio.google.com/apikey)

Ohne Schlüssel startet die App, kann aber nichts erzählen. Du trägst ihn einmalig unter
Einstellungen ein; er bleibt auf dem Gerät.

> **Zum kostenlosen Kontingent:** Google deckelt die Gratis-Nutzung pro Modell und Tag recht
> eng, und ein einziger Spielzug verbraucht mehrere Anfragen (Erzählung, Zustands-Auswertung,
> Erinnerungen). Wer länger am Stück spielt, stößt daran. Die App sagt dir, wenn es soweit ist.

---

## Was die App kann

**Erzählen.** Du gibst einer Geschichte ihren Prompt, tippst Handlungen, die KI erzählt weiter.
Eine globale „Standard-Regie" legt den Ton für alle Geschichten fest.

**Sich erinnern — der eigentliche Kern.** Ein Gedächtnis aus vier Schichten: die letzten Züge
wörtlich, Tageszusammenfassungen, ein episodisches Vektorgedächtnis mit semantischer Suche und
Identitätsprofile der Figuren. Deshalb ist ein prägender Moment von Tag 1 auch an Tag 100 noch
auffindbar — statt im Kontextfenster zu verschwinden.

**Den Weltzustand führen.** Nach jedem Zug wertet ein zweiter Aufruf aus, was sich geändert hat:
Uhrzeit, Ort, Wetter, Kleidung, Zustand, Inventar, anwesende Figuren, Meilensteine. Das Ergebnis
ist ein Checkpoint — eine Momentaufnahme, zu der du jederzeit zurückkehren kannst. Die Spielzeit
läuft deterministisch weiter, auch wenn das Modell einen angeforderten Zeitsprung verschläft.

**Körper und Wunden.** Verletzungen mit Körperregion, betroffenem Organ und Schweregrad, sichtbar
auf einem antippbaren Mannequin mit Scan-Ansicht. Wunden heilen über Spieltage — behandelte
doppelt so schnell, lebensbedrohliche nie von selbst. Schwere Verletzungen hinterlassen Narben,
und die Erzählung weiß davon.

**Figuren mit eigenem Gedächtnis.** NPCs werden über Namen und Aliasse zusammengeführt und
behalten Aussehen, Persönlichkeit, Beziehung zu dir, Laune, Kleidung und Zustand — samt dem, was
sie selbst erlebt haben und du nicht.

**Zeitlinien.** Ab jeder Nachricht verzweigen, zu jeder Nachricht zurückspulen, eine eigene
Handlung nachträglich ändern und neu erzählen lassen. Der Ursprungsstrang bleibt dabei unberührt.

**Außerdem:** Spracheingabe mit Transkription, Notizbuch, freie Modellwahl aus Googles
Live-Katalog, einstellbare Denkstufe und Kreativität, Selbstaktualisierung über GitHub.

---

## Datenschutz in drei Sätzen

Geschichten, Checkpoints, Erinnerungen und Figuren liegen ausschließlich in einer lokalen
Datenbank auf deinem Gerät. Was hinausgeht, geht an Googles Gemini-API — und zwar das, was die
Erzählung braucht: Weltzustand, Erinnerungen, deine Eingabe. Es gibt keine Telemetrie, keine
Werbung, kein Tracking und kein Konto.

Die vollständige Erklärung steht **in der App** unter Einstellungen → Datenschutz. Sie benennt
auch das Unbequeme: die abgeschalteten Sicherheitsfilter, den Schlüssel im Klartext, und was
Google sich im kostenlosen Kontingent bei Ein- und Ausgaben vorbehält.

Zwei Punkte, die du vor dem Installieren wissen solltest:

- **Die Sicherheitsfilter sind aus.** MSF setzt Googles vier einstellbare Filter bewusst auf OFF.
  Die Erzählung kann Gewalt, Sexualität und Grausamkeit ungefiltert schildern. Das ist eine
  Entscheidung für kompromissloses Erzählen, keine Nebenwirkung.
- **Die ausgelieferte APK ist ein Debug-Build** und damit `debuggable`. Wer dein Gerät mit
  eingeschaltetem USB-Debugging an einen Rechner anschließt, kommt über `adb` an die Datenbank
  und den Schlüssel, ohne das Gerät zu rooten. Lass USB-Debugging aus, wenn du es nicht brauchst.

---

## Technik

Kotlin · Jetpack Compose · Material 3 · Room · Google Gemini API

Drei Schriften mit klarer Aufgabenteilung: **Manrope** für Titel und Überschriften, **Inter** für
Fließtext, **JetBrains Mono** für Zeitstempel, Werte und IDs.

### Selbst bauen

```bash
git clone https://github.com/einmalmaik/MFS.git
cd MFS
./gradlew assembleDebug        # APK unter app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # Testlauf
```

Ein API-Schlüssel wird zum Bauen nicht gebraucht — er wird zur Laufzeit in der App eingetragen.

### Wenn du das Projekt forkst

Die Veröffentlichung läuft über eine GitHub Action: Sobald ein Release veröffentlicht wird, baut
sie die APK, erzeugt eine `latest.json` für den Updater und hängt beides an den Release. Damit das
in deinem Fork funktioniert:

1. **Signaturschlüssel als Secret hinterlegen.** `signingConfigs.debugConfig` zeigt auf
   `debug.keystore`, und das steht in `.gitignore` — im CI-Checkout fehlt die Datei also. Lege
   dein Keystore base64-kodiert als Repository-Secret `ANDROID_KEYSTORE_BASE64` ab
   (Settings → Secrets and variables → **Actions**, nicht Environments).

   ```bash
   gh secret set ANDROID_KEYSTORE_BASE64 < <(base64 -w0 debug.keystore)
   ```

2. **Es muss für immer derselbe Schlüssel bleiben.** Ein späterer Wechsel wirkt für jedes Gerät,
   auf dem schon eine Version läuft, wie ein zufälliger Schlüssel: Android lehnt die
   Aktualisierung mit `INSTALL_FAILED_UPDATE_INCOMPATIBLE` ab. Der naheliegende Ausweg —
   deinstallieren — löscht alle Geschichten. Die App warnt davor, heilen kann sie es nicht.

3. **Der Release-Tag muss dem `versionName` entsprechen**, also `v1.0.1` zu `versionName = "1.0.1"`.
   Die Action prüft das und bricht sonst ab, damit kein Release entsteht, dessen APK innen eine
   andere Version trägt — sonst böte der Updater dieselbe Aktualisierung endlos erneut an.

4. **Das Repository muss öffentlich sein**, damit der Updater greift. GitHub antwortet auf private
   Repositories mit 404, und die App kann „kein Release" und „kein Zugriff" nicht unterscheiden.

---

## Verwandtes Projekt

**[Maunting Server Manager](https://github.com/einmalmaik/maunting-server-manager)** — ein
selbstgehostetes Server-Panel aus demselben Haus.
