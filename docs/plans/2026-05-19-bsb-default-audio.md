# BSB Default Audio Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make Berean Standard Bible with the HelloAO David narrator the polished default while keeping public-domain KJV/WEB audio available and treating NKJV as licensed-only.

**Architecture:** Add BSB as a normal bundled translation using the existing data-pack resource format. Add a resolver strategy for HelloAO chapter audio URLs, keep KJV/WEB manifests unchanged, and reject restricted audio manifests with a clear error so copyrighted providers can be added later only through explicit licensed/BYO packs.

**Tech Stack:** Java 21, NeoForge 1.21.1 resources, JUnit 5, Gradle, Python only for generated Bible resource data.

---

### Task 1: Add Audio Resolver Coverage

**Files:**
- Modify: `src/test/java/com/livingword/audio/DefaultAudioChapterUriResolverTest.java`
- Modify: `src/main/java/com/livingword/audio/DefaultAudioChapterUriResolver.java`

**Steps:**
1. Add a failing test for `helloao-bsb-david` resolving `bsb/john/3` to `https://audio.bible.helloao.org/api/BSB/JHN/3/audio/david.mp3`.
2. Add a failing test that `restricted-licensed-provider` throws an `IOException` with a player-facing licensing message.
3. Implement USFM book-code mapping in the existing book path table.
4. Implement the two new path strategies.
5. Run `.\gradlew.bat test --tests com.livingword.audio.DefaultAudioChapterUriResolverTest`.

### Task 2: Add Bundled BSB Manifest

**Files:**
- Create: `src/main/resources/data/livingword/audio/bsb/default.json`
- Modify: `src/test/java/com/livingword/audio/AudioManifestRepositoryTest.java`

**Steps:**
1. Add a failing test that the bundled BSB manifest loads with id `bsb-helloao-david`, MP3 extension, and `helloao-bsb-david` strategy.
2. Add the manifest resource.
3. Run `.\gradlew.bat test --tests com.livingword.audio.AudioManifestRepositoryTest`.

### Task 3: Generate Bundled BSB Bible Text

**Files:**
- Create: `src/main/resources/data/livingword/bible/bsb/**`
- Modify: `src/main/resources/data/livingword/bible/translations.json`
- Modify: `src/test/java/com/livingword/bible/BibleResourceLoaderTest.java`

**Steps:**
1. Add a failing test that default reload includes BSB and John 3:16 matches the BSB wording.
2. Generate BSB resources from the HelloAO complete JSON into the existing translation/chapter schema.
3. Add `bsb` to the bundled translation registry.
4. Run `.\gradlew.bat test --tests com.livingword.bible.BibleResourceLoaderTest`.

### Task 4: Document Provider Policy

**Files:**
- Modify: `docs/audio-packs.md`

**Steps:**
1. Document BSB David as the default polished source.
2. Document KJV/WEB as safe public/free bundled sources.
3. Document NKJV as a licensed/BYO provider only.
4. Run `.\gradlew.bat test`.

