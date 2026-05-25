# Word Sync and Shofar Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add low-CPU word synchronization for lectern playback and replace the shofar with stronger visual and audio assets.

**Architecture:** Session playback time remains the source of truth. The server-side lectern display loads optional timestamp sidecars for exact verse/word timing. When exact sidecars are missing, the existing visual display can still advance with lightweight estimated positions, but exact word sync must come from provider timings or an offline forced-alignment pipeline that emits the same sidecar JSON format. The shofar remains a normal item with bundled lightweight assets.

**Tech Stack:** Java 21, NeoForge 1.21.1, Gradle/JUnit, Minecraft `Component`, PNG item texture, OGG Vorbis sound.

---

### Task 1: Timestamp Word Model

**Files:**
- Modify: `src/main/java/com/livingword/audio/VerseTimestampMap.java`
- Modify: `src/main/java/com/livingword/audio/VerseTimestampParser.java`
- Test: `src/test/java/com/livingword/audio/VerseTimestampParserTest.java`

**Steps:**
1. Add a failing parser test for nested `verses` and `words` timestamp JSON.
2. Add a timed word record and word lookup helpers.
3. Keep the old flat verse timestamp JSON format working.
4. Run the parser tests.

### Task 2: Lectern Word Highlighting

**Files:**
- Modify: `src/main/java/com/livingword/lectern/LecternVerseDisplay.java`
- Modify: `src/main/java/com/livingword/lectern/LecternEvents.java`
- Test: `src/test/java/com/livingword/lectern/LecternVerseDisplayTest.java`

**Steps:**
1. Add failing tests for exact word lookup and fallback estimated word lookup.
2. Add display-line token helpers so the active word can be colored separately.
3. Load timestamp sidecars from classpath resources under `data/livingword/audio/<translation>/<manifest>/<book>/<book_###.timestamps.json`.
4. Render the active word in a stronger color while keeping the verse readable.
5. Use forced-aligned sidecars for exact results; do not treat generated estimates as production timing data.

### Task 3: Shofar Assets

**Files:**
- Modify: `src/main/resources/assets/livingword/textures/item/shofar.png`
- Modify: `src/main/resources/assets/livingword/sounds/shofar_blow.ogg`

**Steps:**
1. Use the Freesound `Sjofar.wav` source from `jpors` with attribution.
2. Convert a transformed long blast to mono OGG Vorbis for Minecraft playback.
3. Flip the held-item transforms so the player holds the mouthpiece end.
4. Confirm the generated assets are packaged into the jar.

### Task 4: Verification

**Commands:**
- `.\gradlew test --tests com.livingword.audio.VerseTimestampParserTest --tests com.livingword.lectern.LecternVerseDisplayTest --tests com.livingword.items.LivingWordItemsTest`
- `.\gradlew build`

**Steps:**
1. Run targeted tests.
2. Run the full build.
3. Copy the jar into the client and server mod folders.
