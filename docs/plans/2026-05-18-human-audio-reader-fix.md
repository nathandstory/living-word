# Human Audio And Reader Fix Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make Living Word use real human Bible narration by default and repair the Bible reader so verses are readable at normal and small GUI scales.

**Architecture:** Audio manifests will describe the actual remote chapter path and file extension instead of assuming generated OGG names. The cache will store the downloaded format, and playback will choose an OGG or MP3 stream based on the cached file. The reader will separate controls from text and wrap verses into stable visual lines.

**Tech Stack:** Java 21, NeoForge 1.21.1, Minecraft client sound streams, JUnit 5, NeoForge Jar-in-Jar for MP3 decoding.

---

### Task 1: Manifest And Cache Formats

**Files:**
- Modify: `src/test/java/com/livingword/audio/AudioManifestParserTest.java`
- Modify: `src/test/java/com/livingword/audio/AudioCacheManagerTest.java`
- Modify: `src/main/java/com/livingword/audio/AudioManifest.java`
- Modify: `src/main/java/com/livingword/audio/AudioManifestParser.java`
- Modify: `src/main/java/com/livingword/audio/AudioCacheManager.java`
- Modify: `src/main/java/com/livingword/audio/CachedAudioDownloadService.java`

**Steps:**
1. Write failing tests for `fileExtension: "mp3"` and explicit `chapterPaths`.
2. Run `.\gradlew.bat test --tests com.livingword.audio.AudioManifestParserTest --tests com.livingword.audio.AudioCacheManagerTest`.
3. Add extension-aware paths and explicit remote chapter path support.
4. Re-run the focused tests.

### Task 2: Human WEB Audio Default

**Files:**
- Create: `src/main/resources/data/livingword/audio/webp/default.json`
- Modify: `src/main/java/com/livingword/config/LivingWordConfig.java`
- Modify: `src/main/java/com/livingword/client/LivingWordClient.java`

**Steps:**
1. Add a bundled manifest for eBible WEB human MP3 chapters, starting with exact URL support and provider metadata.
2. Ensure `manifestOrFallback` prefers bundled manifests before the configurable CDN fallback.
3. Run audio manifest repository tests.

### Task 3: MP3 Playback

**Files:**
- Modify: `build.gradle`
- Modify: `src/main/java/com/livingword/client/MinecraftAudioPlaybackService.java`
- Create: `src/main/java/com/livingword/client/Mp3AudioStream.java`

**Steps:**
1. Add a failing testable stream selection helper if feasible outside Minecraft runtime.
2. Add MP3 decoding through a bundled JavaSound MP3 SPI dependency.
3. Keep OGG playback unchanged for existing audio packs.
4. Run `.\gradlew.bat build`.

### Task 4: Bible Reader Layout

**Files:**
- Modify: `src/test/java/com/livingword/client/gui/widgets/VerseListWidgetTest.java`
- Create: `src/main/java/com/livingword/client/gui/widgets/WrappedVerseLayout.java`
- Modify: `src/main/java/com/livingword/client/gui/widgets/VerseListWidget.java`
- Modify: `src/main/java/com/livingword/client/gui/BibleScreen.java`

**Steps:**
1. Write failing tests proving long verse text wraps instead of truncating.
2. Add wrapped verse layout logic independent of Minecraft rendering.
3. Move the reader controls out of the verse text area and add an obvious close button.
4. Run focused GUI tests and a full build.
