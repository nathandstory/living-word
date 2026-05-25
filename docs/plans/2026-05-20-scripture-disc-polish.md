# Scripture Disc Polish Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make the configurable Scripture Disc understandable, polished, and reliable for jukebox listening.

**Architecture:** Keep one configurable disc item and store its selected passage, audio source, and playback mode in item custom data. Extend listening sessions and sync payloads with an audio manifest id so every client downloads the same narrator/source independently. Keep automatic book continuation as metadata-only until audio duration/end callbacks exist; implement reliable play, pause, resume, stop, and clear player feedback now.

**Tech Stack:** Java 21, NeoForge 1.21.1 item custom data, custom payloads, JUnit 5, Minecraft client GUI.

---

### Task 1: Disc Selection Data

**Files:**
- Modify: `src/main/java/com/livingword/discs/ScriptureDiscSelection.java`
- Create: `src/main/java/com/livingword/discs/ScriptureDiscPlaybackMode.java`
- Create: `src/main/java/com/livingword/discs/ScriptureDiscAudioSource.java`
- Test: `src/test/java/com/livingword/discs/ScriptureDiscSelectionTest.java`

**Steps:**
1. Add failing tests for default BSB John 1 / default narrator / single chapter mode and custom metadata round-trip.
2. Add playback mode enum with `SINGLE_CHAPTER`, `CONTINUE_BOOK`, and `LOOP_CHAPTER`.
3. Add known audio source options for BSB David/Hays/Souer, KJV default, and WEBP default.
4. Extend selection read/write to preserve new fields while accepting old discs.

### Task 2: Audio Source Synchronization

**Files:**
- Modify: `src/main/java/com/livingword/sync/ListeningSession.java`
- Modify: `src/main/java/com/livingword/sync/ListeningSessionManager.java`
- Modify: `src/main/java/com/livingword/network/payload/ListeningSessionSyncPayload.java`
- Modify: `src/main/java/com/livingword/network/payload/ConfigureScriptureDiscPayload.java`
- Modify: `src/main/java/com/livingword/network/LivingWordNetwork.java`
- Modify: `src/main/java/com/livingword/client/ClientAudioSessionController.java`
- Modify: `src/main/java/com/livingword/client/LivingWordClient.java`
- Test: existing sync, client, and payload tests

**Steps:**
1. Add failing tests proving sessions/payloads carry `audioManifestId`.
2. Add backward-safe defaults for old call sites.
3. Resolve audio manifests by `translationId + audioManifestId` on the client.
4. Keep local Bible listening on the translation default manifest.

### Task 3: Narrator Manifests

**Files:**
- Modify: `src/main/java/com/livingword/audio/DefaultAudioChapterUriResolver.java`
- Create: `src/main/resources/data/livingword/audio/bsb/hays.json`
- Create: `src/main/resources/data/livingword/audio/bsb/souer.json`
- Test: `src/test/java/com/livingword/audio/DefaultAudioChapterUriResolverTest.java`
- Test: `src/test/java/com/livingword/audio/AudioManifestRepositoryTest.java`

**Steps:**
1. Add failing tests for HelloAO Hays and Souer URLs.
2. Generalize the HelloAO BSB resolver to use the narrator suffix from the strategy.
3. Add bundled manifest resources.

### Task 4: Jukebox Controls And Feedback

**Files:**
- Modify: `src/main/java/com/livingword/discs/ScriptureDisc.java`
- Modify: `src/main/java/com/livingword/discs/ScriptureDiscEvents.java`
- Modify: `src/main/java/com/livingword/discs/JukeboxListeningSessionRegistry.java`
- Modify: `src/main/resources/assets/livingword/lang/en_us.json`
- Test: `src/test/java/com/livingword/discs/JukeboxListeningSessionRegistryTest.java`
- Test: `src/test/java/com/livingword/discs/ScriptureDiscTest.java`

**Steps:**
1. Add tests for same-selection resume, changed-selection reset, and clearer source/mode tooltip text.
2. Make normal jukebox use toggle play/pause/resume.
3. Make shift-use stop/reset.
4. Improve player messages with passage, source, mode, and participant intent.

### Task 5: Disc Selection UI

**Files:**
- Modify: `src/main/java/com/livingword/client/gui/ScriptureDiscSelectionScreen.java`
- Modify: `src/main/java/com/livingword/client/LivingWordClient.java`
- Test: `src/test/java/com/livingword/client/gui/ScriptureDiscSelectionScreenTest.java`

**Steps:**
1. Add failing static tests for source/mode/search/preview controls and `Set Disc` wording.
2. Replace vague button labels with translation, book, chapter, narrator/source, and mode rows.
3. Add book search and preview button.
4. Send full selection metadata to the server.

### Task 6: Verification And Packaging

**Files:**
- Generated jar: `build/libs/living-word-0.1.0.jar`

**Steps:**
1. Run focused tests.
2. Run `.\gradlew.bat test`.
3. Run `.\gradlew.bat build`.
4. Copy the all-jar to the configured client and server mods folders.
5. Commit and push to private GitHub.
