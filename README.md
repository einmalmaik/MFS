# StoryForge

StoryForge ist ein interaktives Text-RPG und eine Storytelling-App mit einem KI-gesteuerten Game-Master (angetrieben durch die Gemini API). Die App bietet ein immersives Checkpoint-Gedächtnissystem, dynamisches Tracking von Charakterzuständen, Wunden (inkl. detailliertem 3D-Mannequin) und ein komplettes RPG-Erlebnis direkt auf dem Smartphone.

## ✨ Features

- **KI Game-Master:** Erlebe dynamische und interaktive Abenteuer, in denen die KI als Spielleiter fungiert.
- **Vektor-basiertes Gedächtnis:** Nutzt Embedding-Modelle (wie `text-embedding-004` oder `embedding-001`), um die Vergangenheit der Geschichte nahtlos in aktuelle Ereignisse einzuflechten und sich Charaktere und Entscheidungen exakt zu merken.
- **Charakter & Anatomie-Tracking:** Behalte deine Spielfigur und Begleiter (NPCs) im Auge. Inklusive visuellem Mannequin, Körperteil-Filterung und einem detaillierten Verletzungs- und Wundsystem, auf das die KI zugreifen und das sie beeinflussen kann.
- **Offline-Gedächtnis-Speicher:** Alle Episoden, Charaktere und Weltdetails werden über eine lokale Room-Datenbank sicher und performant gespeichert.
- **Sicheres Secrets-Management:** Konzipiert für einen sauberen und sicheren Umgang mit API-Schlüsseln (Gemini AI).

## 🚀 Installation via GitHub Release (APK)

Sobald ein neuer **Release (Tag)** auf GitHub veröffentlicht wird, baut eine **GitHub Action** im Hintergrund automatisch die aktuelle Android APK der App.

Du kannst diese APK dann direkt von deinem Smartphone aus unter dem Reiter **Releases** auf dieser GitHub-Seite herunterladen und installieren – ganz ohne USB-Kabel oder Android Studio!

*(Hinweis: Die GitHub Action baut aktuell eine `Debug-APK`, damit du sie ohne das komplizierte Einrichten von kryptografischen Release-Signaturschlüsseln (Keystores) einfach und unkompliziert per Sideloading installieren kannst.)*

## 🛠️ Entwickelt mit
- Kotlin
- Jetpack Compose (Material Design 3)
- Room Database (Local SQLite)
- Gemini API (AI Studio)
