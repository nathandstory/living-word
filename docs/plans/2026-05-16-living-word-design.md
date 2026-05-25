# Living Word First-Build Design

Date: 2026-05-16

## Purpose

Living Word is a NeoForge 1.21.1 Minecraft Java mod focused on peaceful, respectful, multiplayer-friendly Bible reading and synchronized scripture listening. The initial implementation produces a compile-ready foundation with one usable player path while keeping audio, lectern, disc, and synchronization systems architecturally ready for later production hardening.

## Scope

The initial implementation prioritizes a stable foundation over attempting every feature at full depth immediately. It includes:

- A craftable Bible item that opens a custom Bible GUI.
- Data-driven Bible translation loading with bundled KJV sample/default content.
- A translation selector and stable references for future translations.
- Book, chapter, and verse navigation in the GUI.
- Search, bookmark, recent-history, and copy-to-chat service scaffolding.
- Network registration and packet models for future synchronized listening sessions.
- Audio cache, CDN manifest, download, playback, and sync service interfaces/skeletons.
- Scripture disc and lectern integration skeletons that compile and expose clear extension points.
- Config entries for CDN URLs, cache policy, subtitles, sync tolerance, daily verse, and narration volume.

Low-level streamed OGG playback and production CDN download behavior will be implemented in a later focused pass because they need careful testing against the Minecraft client sound engine and real audio assets.

## Architecture

The code will be organized under `com.livingword` with these main packages:

- `core`: mod constants, shared bootstrap, logging, utility types.
- `bible`: translation manifests, book/chapter/verse references, Bible data manager, search index abstractions.
- `client`: client setup and screen registration.
- `client.gui`: Bible screen and reusable GUI widgets.
- `audio`: audio manifest models, cache manager, chapter resolver, async download/playback interfaces.
- `sync`: listening sessions, membership, source positions, server-side session manager.
- `network`: custom payload registration and play/pause/seek/join/leave/sync packets.
- `items`: Bible item and scripture disc items.
- `lectern`: lectern hooks and listening-station abstractions.
- `discs`: scripture disc metadata and jukebox integration scaffolding.
- `config`: NeoForge config specs.

Shared/common code will never reference client-only Minecraft classes. GUI, audio playback, and client cache behavior will live behind client-only setup classes.

## Translation System

KJV will be bundled because it is public domain, but the mod will not be KJV-only. Translations will be data-driven and loadable from datapacks or other mod resources.

Example layout:

```text
data/livingword/bible/kjv/translation.json
data/livingword/bible/kjv/books/john/003.json
```

A translation manifest includes:

- translation id
- display name
- language code
- license
- attribution
- text direction
- book order
- optional audio manifest id

Verse references will store:

```text
translation_id + book_id + chapter + verse
```

This keeps bookmarks, history, search, and audio synchronization compatible with future translations such as WEB, ASV, modpack-provided translations, or licensed translations distributed separately by users with permission.

## Multiplayer Listening Model

The server will coordinate listening sessions but will not relay audio data. A session records:

- translation id
- audio manifest or narrator id
- book id and chapter
- playback state
- server start time or paused timestamp
- source type and optional world position
- participant list
- content hash/version metadata

Each client independently streams or downloads the declared chapter audio from the configured source, plays it locally, and follows periodic timestamp correction packets from the server. If a chapter is already cached locally, the client can play offline. If it is not cached and the internet is unavailable, the client will show that first playback requires internet.

This allows several players near the same lectern, jukebox, or Bible session to hear the same passage from the same declared source at the same time without burdening the Minecraft server with audio bandwidth.

## Audio Cache Foundation

Audio will be modeled as one OGG Vorbis file per chapter plus optional verse timestamps.

Example cache path:

```text
.minecraft/livingword/cache/audio/kjv/john/john_003.ogg
.minecraft/livingword/cache/audio/kjv/john/john_003.timestamps.json
```

The initial implementation defines the cache manager, URL resolver, download task model, progress states, hash validation hooks, corrupted-file recovery behavior, and async execution boundaries. Full streaming playback is implemented after the foundation compiles.

## GUI Direction

The Bible GUI should feel modern, calm, and vanilla-adjacent rather than like a basic inventory screen. It will use a dark parchment style, compact navigation, readable text, smooth scroll-ready layout, keyboard search entry, verse highlighting hooks, and minimal clutter.

The initial implementation makes the GUI structurally usable and ready for polish. It avoids embedding copyrighted text beyond public-domain sample KJV content.

## Config

The mod will provide common and client config specs for:

- audio CDN base URL
- cache size limits
- narration volume
- subtitle display
- synchronization tolerance
- autoplay behavior
- daily verse enablement
- verse display style
- debug logging

## Error Handling

Bible data loading should tolerate missing optional translations and report clear errors for malformed manifests. Audio requests should expose states for cached, downloading, unavailable, failed hash validation, and offline-first-playback. Network packets should validate that players are authorized participants before mutating sessions.

## Testing Strategy

Initial verification will focus on compilation and unit-testable pure Java logic where practical:

- translation manifest parsing
- verse reference serialization
- cache path generation
- listening session timestamp math
- packet encode/decode shape where feasible

Manual client verification will check that the Bible item exists, the GUI opens, KJV sample data displays, and client/server launch tasks start.

## Future Expansion

The architecture intentionally leaves room for:

- full KJV chapter data generation/import
- WEB or ASV translation packs
- real streamed OGG playback
- narrator/audio packs
- lectern listening stations
- scripture discs in jukeboxes
- daily verse scheduling
- notes and highlights
- chapel or library structures
- optional Simple Voice Chat integration hooks
