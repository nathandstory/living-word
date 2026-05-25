# Release Checklist

Use this before uploading Living Word to Modrinth, CurseForge, or making the repository public.

## Required Before Upload

- Run `.\gradlew.bat test`.
- Run `.\gradlew.bat build`.
- Test the built jar in a clean client profile.
- Test the built jar on a dedicated NeoForge server.
- Confirm the Bible, Scripture Disc, lectern playback, shofar, and advancements work in-game.
- Confirm `NOTICE.md` is current for Bible text, audio providers, generated timing data, sound effects, textures, and logo assets.
- Confirm no private files, API keys, local caches, downloaded audio, crash logs, or personal notes are tracked.
- Confirm the final jar is the only file uploaded as the mod artifact.

## Modrinth Metadata

- Project name: Living Word
- Slug: `living-word`
- Loader: NeoForge
- Game version: 1.21.1
- Environment: client and server
- License: MIT for code; see bundled `NOTICE.md` for data and media notices
- Categories: utility, social, adventure, decoration, or library/API only if it later exposes a public integration API
- Required dependencies: NeoForge
- Description: use the README summary and feature list
- Changelog: use `CHANGELOG.md`
- Gallery: include actual in-game screenshots of the Bible item, Bible GUI, Scripture Disc, lectern station, and shofar

## CurseForge Metadata

- Project type: Minecraft mod
- Mod loader: NeoForge
- Game version: 1.21.1
- Java version: 21
- License: MIT, with source notices in `NOTICE.md`
- Logo: `src/main/resources/logo.png`
- Summary: Peaceful in-game Bible reading and synchronized scripture listening for NeoForge.
- Description: use the README summary and feature list
- Changelog: use `CHANGELOG.md`
- Relations: NeoForge is required
- Mark the file as both client and server when the platform asks for supported environments

## Public GitHub Review

- Confirm `README.md`, `NOTICE.md`, `CHANGELOG.md`, `CONTRIBUTING.md`, `SECURITY.md`, and `SUPPORT.md` are present.
- Confirm issue templates exist under `.github/ISSUE_TEMPLATE`.
- Confirm repository description, website, topics, and license are set in GitHub settings.
- Confirm private vulnerability reporting is enabled if available.
- Confirm Actions/Dependabot settings are acceptable.
- Confirm branch protections before accepting outside contributions.
