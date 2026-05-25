# Contributing

Living Word is a peaceful scripture reading and listening mod. Contributions should preserve the tone of the project: respectful, non-combat, multiplayer-friendly, and vanilla-adjacent.

## Development Setup

Requirements:

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.x

Common commands:

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runClient
```

## Contribution Guidelines

- Keep client and server logic separated.
- Do not bundle large audio files in the jar.
- Only add Bible translations or audio providers when redistribution or streaming terms are clear.
- Add focused tests for parser, data, synchronization, and cache behavior.
- Keep UI changes readable at common Minecraft GUI scales.
- Avoid combat, spellcasting, power, mana, or weaponized scripture mechanics.

## Reporting Issues

Use the issue templates for bugs, content/audio problems, and feature requests. Include Minecraft version, NeoForge version, mod version, and logs when relevant.
