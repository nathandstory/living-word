# Living Word

Living Word is a NeoForge 1.21.1 Minecraft mod for peaceful in-game Bible reading and synchronized scripture listening.

This repository contains the first-build foundation for the mod:

- a craftable Bible item
- a first-pass Bible reading screen
- data-driven Bible translation metadata and chapter samples
- multiplayer listening session models and packet payloads
- audio cache/download/playback service foundations
- lectern and Scripture Disc extension points

## Development

- Minecraft: 1.21.1
- NeoForge: 21.1.200
- Java: 21
- Mod ID: `livingword`
- Root package: `com.livingword`

Run Gradle through the checked-in wrapper:

```powershell
.\gradlew.bat --version
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat compileJava
.\gradlew.bat runClient
```

## Content Notes

KJV sample text is bundled because it is public domain. The code is not KJV-only: translations are data-driven and can be supplied by datapacks or other mods. Only distribute translations or audio recordings that are public domain or that you have permission to redistribute.

See:

- `docs/translation-packs.md`
- `docs/audio-packs.md`
- `docs/testing.md`
