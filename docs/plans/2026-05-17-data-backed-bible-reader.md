# Data-Backed Bible Reader Implementation Plan

**Goal:** Replace the hardcoded Bible GUI sample with bundled resource loading, indexed search, and navigation over loaded translations/books/chapters.

**Architecture:** Pure Java Bible parsing and indexing lives in `com.livingword.bible`. Client-only code in `com.livingword.client` exposes a cached repository to `BibleScreen`. Resource packs and future generated full-KJV data use the same manifest/index/chapter JSON format.

**Tech Stack:** Java 21, NeoForge 1.21.1, Gson from the Minecraft dependency set, JUnit Jupiter.

## Task 1: Add Bible resource parser and index tests

**Files:**
- Create: `src/test/java/com/livingword/bible/BibleJsonParserTest.java`
- Create: `src/test/java/com/livingword/bible/BibleResourceLoaderTest.java`
- Create: `src/main/resources/data/livingword/bible/kjv/index.json`

**Steps:**

1. Add tests proving a translation manifest parses from JSON.
2. Add tests proving chapter JSON converts verse keys to integer verse numbers.
3. Add tests proving an index file loads multiple chapters into `BibleDataManager`.
4. Run the focused tests and confirm they fail before implementation.

## Task 2: Implement parser, resource index, and manager APIs

**Files:**
- Create: `src/main/java/com/livingword/bible/BibleJsonParser.java`
- Create: `src/main/java/com/livingword/bible/BibleTranslationIndex.java`
- Modify: `src/main/java/com/livingword/bible/BibleResourceLoader.java`
- Modify: `src/main/java/com/livingword/bible/BibleDataManager.java`
- Modify: `src/main/java/com/livingword/bible/BibleSearchIndex.java`

**Steps:**

1. Parse `translation.json`, `index.json`, and chapter JSON with Gson.
2. Load resources by known translation ids and index entries instead of jar directory scanning.
3. Add manager methods for manifests, available books, available chapters, and deterministic search.
4. Keep missing resources non-fatal so optional translation packs can fail gracefully.
5. Run the focused Bible tests.

## Task 3: Add client repository and wire the GUI

**Files:**
- Create: `src/main/java/com/livingword/client/BibleClientRepository.java`
- Modify: `src/main/java/com/livingword/client/gui/BibleGuiState.java`
- Modify: `src/main/java/com/livingword/client/gui/BibleScreen.java`
- Modify: `src/main/java/com/livingword/client/gui/widgets/VerseListWidget.java`
- Modify: `src/main/resources/assets/livingword/lang/en_us.json`

**Steps:**

1. Cache the loaded bundled Bible data on the client.
2. Add `BibleGuiState` navigation methods for translation/book/chapter changes.
3. Render the current loaded chapter instead of a sample object.
4. Add previous/next book and chapter buttons plus search jump/bookmark controls.
5. Copy selected verse text from loaded data.

## Task 4: Document and verify the import path

**Files:**
- Create: `docs/kjv-import.md`
- Optionally create: `tools/kjv-import/README.md`

**Steps:**

1. Document the public-domain KJV requirement and generated output format.
2. Document how non-KJV translation packs should provide license metadata.
3. Run `.\gradlew.bat test`.
4. Run `.\gradlew.bat build`.
5. Commit each passing slice.
