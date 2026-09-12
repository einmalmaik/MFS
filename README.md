# Maunting Story Fable (MSF)

Maunting Story Fable ist ein interaktives Text-RPG und eine immersive Storytelling-App mit einem KI-gesteuerten Game-Master (angetrieben durch die Google Gemini API) im Stil der **MauntingStudios Design-DNA**. Die App bietet ein immersives Checkpoint-Gedächtnissystem, dynamisches Tracking von Charakterzuständen, Wunden (inkl. detailliertem 3D-Mannequin) und ein komplettes RPG-Erlebnis direkt auf dem Smartphone.

## ✨ Features

- **Atmosphärische Design-DNA:** Dark-First Elevation-Oberflächen, Ice-Cyan Akzente, gemütliche Bernstein-Akzente für gesprochene Dialoge (`StoryAmberCampfire`), Manrope-Typografie für epische Titel und Inter für ermüdungsfreies Lesen.
- **Zentrale Design-Komponenten:** DnaDropdown, DnaNumberStepper, DnaButton, DnaCard und DnaBadge.
- **KI Game-Master:** Erlebe dynamische und interaktive Abenteuer, in denen die KI als Spielleiter fungiert.
- **Vier-Schichten-Gedächtnis:** Arbeitsfenster, Tageszusammenfassungen, episodische Vektorsuche (`gemini-embedding-001`) und Identitätsgedächtnis der Figuren. Damit bleibt ein prägender Moment von Tag 1 auch an Tag 100 auffindbar. Die Modellwahl steht in den Einstellungen und gilt für alle Geschichten.
- **Selbstaktualisierung:** MSF kann bei GitHub nach einer neueren Fassung sehen — ab Werk abgeschaltet, und nichts wird ohne ausdrückliche Zustimmung geladen oder installiert.
- **Datenschutzerklärung in der App:** Erreichbar über die Einstellungen. Sie benennt auch, was unbequem ist — abgeschaltete Sicherheitsfilter, den Schlüssel im Klartext und was Google im kostenlosen Kontingent mit den Daten tun darf.
- **Charakter & Anatomie-Tracking:** Behalte deine Spielfigur und Begleiter (NPCs) im Auge. Inklusive visuellem Mannequin, Körperteil-Filterung und einem detaillierten Verletzungs- und Wundsystem, auf das die KI zugreifen und das sie beeinflussen kann.
- **Offline-Gedächtnis-Speicher:** Alle Episoden, Charaktere und Weltdetails werden über eine lokale Room-Datenbank sicher und performant gespeichert.
- **Sicheres Secrets-Management:** Konzipiert für einen sauberen und sicheren Umgang mit API-Schlüsseln (Google Gemini AI).

## 🚀 Installation via GitHub Release (APK)

Sobald ein neuer **Release (Tag)** auf GitHub veröffentlicht wird, baut eine **GitHub Action** im Hintergrund automatisch die aktuelle Android APK der App.

Du kannst diese APK dann direkt von deinem Smartphone aus unter dem Reiter **Releases** auf dieser GitHub-Seite herunterladen und installieren – ganz ohne USB-Kabel oder Android Studio. Die stabile Adresse lautet:

```
https://github.com/einmalmaik/MFS/releases/latest/download/MauntingStoryFable.apk
```

**Vor dem ersten Release zu erledigen:**

1. **Signaturschlüssel als Secret hinterlegen.** `signingConfigs.debugConfig` zeigt auf `debug.keystore`, das in `.gitignore` steht — ohne Secret kann die Action gar nicht bauen. `base64 -w0 debug.keystore` und das Ergebnis als Repository-Secret `ANDROID_KEYSTORE_BASE64` eintragen.
2. **Debug- oder Release-Schlüssel entscheiden — dauerhaft.** Ein späterer Wechsel wirkt für jedes Gerät, auf dem schon eine Version läuft, wie ein zufälliger Schlüssel: Android lehnt die Aktualisierung ab, und der naheliegende Ausweg (deinstallieren) löscht alle Geschichten. Die App warnt davor, heilen kann sie es nicht.
3. **Repo öffentlich schalten**, wenn der Updater und der Hinweis im Maunting Server Manager greifen sollen. GitHub antwortet auf private Repositories mit 404 — die App kann „kein Release" und „kein Zugriff" nicht unterscheiden und schweigt in beiden Fällen.

*(Hinweis: Die GitHub Action baut aktuell eine `Debug-APK`, damit sie sich ohne eingerichteten Release-Keystore per Sideloading installieren lässt. Ein Debug-Build ist `debuggable` — auf einem entsperrten Gerät ist sein Datenbestand damit über `adb` zugänglich.)*

## 🛠️ Entwickelt mit
- Kotlin
- Jetpack Compose & MauntingStudios Design-DNA
- Room Database (Local SQLite)
- Google Gemini API (AI Studio)

