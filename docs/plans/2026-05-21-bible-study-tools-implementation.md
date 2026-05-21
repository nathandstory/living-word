# Bible Study Tools Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add search results, notes, verse collections/playlists, and clearer local audio controls to the Bible reader.

**Architecture:** Extend `BibleGuiState` for study-state behavior, extend `BibleClientPreferences` for persistence, and keep rendering in `BibleScreen`. Audio queue control remains client-local through `LivingWordClient` and existing local chapter playback.

**Tech Stack:** Java 21, NeoForge 1.21.1, JUnit 5, Gson, existing Minecraft GUI widgets.

---

### Task 1: Study State Tests

**Files:**
- Modify: `src/test/java/com/livingword/client/gui/BibleGuiStateTest.java`
- Modify: `src/main/java/com/livingword/client/gui/BibleGuiState.java`

**Step 1: Write failing tests**

Add tests for notes, collections, visible reader tab, and audio queue navigation.

**Step 2: Run tests**

Run:
`.\gradlew test --tests com.livingword.client.gui.BibleGuiStateTest`

Expected: compile failures for missing APIs.

**Step 3: Implement state APIs**

Add records and methods to `BibleGuiState`:

- `ReaderView`
- `VerseNote`
- `VerseCollection`
- `setNote`, `noteFor`, `notes`, `replaceNotes`
- `addSelectedVerseToCollection`, `removeFromCollection`, `collections`, `replaceCollections`
- `replaceAudioQueue`, `nextQueuedChapter`, `previousQueuedChapter`

**Step 4: Verify**

Run the same test command and expect pass.

### Task 2: Preferences Persistence

**Files:**
- Modify: `src/test/java/com/livingword/client/BibleClientPreferencesTest.java`
- Modify: `src/main/java/com/livingword/client/BibleClientPreferences.java`

**Step 1: Write failing tests**

Persist and load notes and collections in `bible_state.json`.

**Step 2: Run tests**

Run:
`.\gradlew test --tests com.livingword.client.BibleClientPreferencesTest`

Expected: compile failures or assertion failures.

**Step 3: Implement JSON storage**

Add JSON records for notes and collections. Ignore malformed stable references while loading.

**Step 4: Verify**

Run the same test command and expect pass.

### Task 3: Screen Contract and Layout

**Files:**
- Modify: `src/test/java/com/livingword/client/gui/BibleScreenRenderContractTest.java`
- Modify: `src/test/java/com/livingword/client/gui/BibleScreenLayoutTest.java`
- Modify: `src/main/java/com/livingword/client/gui/BibleScreenLayout.java`
- Modify: `src/main/java/com/livingword/client/gui/BibleScreen.java`
- Modify: `src/main/resources/assets/livingword/lang/en_us.json`

**Step 1: Write failing tests**

Assert that `BibleScreen` contains Search, Notes, Collections, and audio control rendering methods.

**Step 2: Run tests**

Run:
`.\gradlew test --tests com.livingword.client.gui.BibleScreenRenderContractTest --tests com.livingword.client.gui.BibleScreenLayoutTest`

Expected: failures for missing methods and layout fields.

**Step 3: Implement UI**

Add compact tab buttons and view renderers. Search `Go` should switch to Search results. Click rows to navigate. Notes and Collections render in the main list area.

**Step 4: Verify**

Run the same test command and expect pass.

### Task 4: Audio Queue Controls

**Files:**
- Modify: `src/test/java/com/livingword/client/LivingWordClientAudioRoutingTest.java`
- Modify: `src/main/java/com/livingword/client/LivingWordClient.java`
- Modify: `src/main/java/com/livingword/client/gui/BibleScreen.java`

**Step 1: Write failing tests**

Assert client exposes current local playback state and queue controls without using the network jukebox controller.

**Step 2: Run tests**

Run:
`.\gradlew test --tests com.livingword.client.LivingWordClientAudioRoutingTest`

Expected: failures for missing public client helpers.

**Step 3: Implement minimal helpers**

Add `localBiblePlaybackLabel`, `playLocalQueue`, and stop/next/previous hooks that continue using `bibleAudioController`.

**Step 4: Verify**

Run the same test command and expect pass.

### Task 5: Full Verification and Delivery

**Files:**
- All touched source, tests, docs.

**Step 1: Run full build**

Run:
`.\gradlew build`

Expected: `BUILD SUCCESSFUL`.

**Step 2: Copy jar to test locations**

Copy:
`build\libs\livingword-0.1.0-all.jar`

To:
- `C:\Users\nateh\curseforge\minecraft\Instances\arclight-neoforge-vanilla++\mods\livingword-0.1.0-all.jar`
- `C:\Users\nateh\Desktop\server-official\mods\livingword-0.1.0-all.jar`

**Step 3: Commit and push**

Commit with:
`feat: add bible study tools`

Push:
`git push origin HEAD:main`

