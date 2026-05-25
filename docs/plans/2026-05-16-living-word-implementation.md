# Living Word Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a compile-ready NeoForge 1.21.1 mod foundation for Living Word with a usable Bible item/GUI path, data-driven translations, and production-shaped audio/session APIs.

**Architecture:** The mod is split into common domain services, server-side session coordination, and client-only GUI/audio code. Bible text is loaded from data resources through translation manifests, with KJV bundled as default content but no KJV-only assumptions. Multiplayer listening is server-coordinated and client-streamed: the server syncs state and timestamps, while each client fetches/caches audio independently.

**Tech Stack:** Java 21, NeoForge 1.21.1 MDK/NeoGradle, Gradle, JUnit Jupiter for pure Java tests, Minecraft client GUI classes, NeoForge config and networking APIs.

---

## References

- NeoForge 1.21.1 Getting Started: https://docs.neoforged.net/docs/1.21.1/gettingstarted/
- NeoForge 1.21.1 Resources: https://docs.neoforged.net/docs/1.21.1/resources/
- NeoForge 1.21.1 Networking: https://docs.neoforged.net/docs/1.21.1/networking/
- NeoForge 1.21.1 Config: https://docs.neoforged.net/docs/1.21.1/misc/config/

---

### Task 1: Scaffold NeoForge Project

**Files:**
- Create: `settings.gradle`
- Create: `build.gradle`
- Create: `gradle.properties`
- Create: `src/main/resources/META-INF/neoforge.mods.toml`
- Create: `src/main/resources/pack.mcmeta`
- Create: `src/main/java/com/livingword/LivingWord.java`
- Create: `README.md`

**Step 1: Create the Gradle/NeoForge skeleton**

Use the NeoForge 1.21.1 MDK layout. The project id is `livingword`, display name is `Living Word`, Java version is 21, and root package is `com.livingword`.

`settings.gradle`:

```groovy
pluginManagement {
    repositories {
        mavenLocal()
        maven { url = 'https://maven.neoforged.net/releases' }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        maven { url = 'https://maven.neoforged.net/releases' }
        mavenCentral()
    }
}

rootProject.name = 'living-word'
```

`gradle.properties` should declare:

```properties
org.gradle.jvmargs=-Xmx2G
org.gradle.daemon=false
org.gradle.debug=false
minecraft_version=1.21.1
minecraft_version_range=[1.21.1]
neo_version=21.1.200
neo_version_range=[21.1,)
loader_version_range=[4,)
mod_id=livingword
mod_name=Living Word
mod_license=MIT
mod_version=0.1.0
mod_group_id=com.livingword
mod_authors=Nathan Story
mod_description=Peaceful in-game Bible reading and synchronized scripture listening for NeoForge.
```

Use the current NeoForge docs/MDK if `neo_version=21.1.200` needs adjustment during dependency resolution.

**Step 2: Add the main mod class**

`src/main/java/com/livingword/LivingWord.java`:

```java
package com.livingword;

import com.livingword.config.LivingWordConfig;
import com.livingword.items.LivingWordItems;
import com.livingword.network.LivingWordNetwork;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(LivingWord.MOD_ID)
public final class LivingWord {
    public static final String MOD_ID = "livingword";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LivingWord(IEventBus modEventBus, ModContainer modContainer) {
        LivingWordConfig.register(modContainer);
        LivingWordItems.register(modEventBus);
        LivingWordNetwork.register(modEventBus);
    }
}
```

Use placeholder classes for config/items/network in this task if needed, then fill them in later tasks.

**Step 3: Run Gradle help to verify dependency resolution**

Run:

```powershell
.\gradlew.bat --version
```

Expected: Gradle starts and reports Java 21. If the wrapper does not exist yet, create or copy the wrapper from the MDK template before continuing.

**Step 4: Commit**

```powershell
git add settings.gradle build.gradle gradle.properties src/main/resources src/main/java README.md
git commit -m "chore: scaffold NeoForge mod project"
```

---

### Task 2: Add Bible Domain Model With Tests

**Files:**
- Create: `src/main/java/com/livingword/bible/BibleReference.java`
- Create: `src/main/java/com/livingword/bible/BookId.java`
- Create: `src/main/java/com/livingword/bible/TranslationManifest.java`
- Create: `src/main/java/com/livingword/bible/ChapterData.java`
- Create: `src/test/java/com/livingword/bible/BibleReferenceTest.java`
- Create: `src/test/java/com/livingword/bible/TranslationManifestTest.java`
- Modify: `build.gradle`

**Step 1: Add JUnit dependencies**

In `build.gradle`, add JUnit Jupiter for pure Java tests:

```groovy
dependencies {
    testImplementation platform('org.junit:junit-bom:5.11.4')
    testImplementation 'org.junit.jupiter:junit-jupiter'
}

test {
    useJUnitPlatform()
}
```

Keep the NeoForge dependencies from Task 1.

**Step 2: Write failing tests**

`BibleReferenceTest.java`:

```java
package com.livingword.bible;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class BibleReferenceTest {
    @Test
    void serializesStableTranslationScopedReference() {
        BibleReference reference = new BibleReference("kjv", "john", 3, 16);
        assertEquals("kjv:john:3:16", reference.toStableId());
    }

    @Test
    void rejectsInvalidChapterOrVerse() {
        assertThrows(IllegalArgumentException.class, () -> new BibleReference("kjv", "john", 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new BibleReference("kjv", "john", 3, 0));
    }
}
```

`TranslationManifestTest.java`:

```java
package com.livingword.bible;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TranslationManifestTest {
    @Test
    void storesTranslationMetadataWithoutKjvAssumptions() {
        TranslationManifest manifest = new TranslationManifest(
            "web",
            "World English Bible",
            "en_us",
            "Public Domain",
            "World English Bible contributors",
            "ltr",
            List.of("genesis", "john"),
            "web-default"
        );

        assertEquals("web", manifest.id());
        assertEquals("World English Bible", manifest.displayName());
        assertTrue(manifest.bookOrder().contains("john"));
    }
}
```

**Step 3: Run tests to verify failure**

Run:

```powershell
.\gradlew.bat test --tests com.livingword.bible.BibleReferenceTest --tests com.livingword.bible.TranslationManifestTest
```

Expected: FAIL because model classes do not exist.

**Step 4: Implement model records**

`BibleReference.java`:

```java
package com.livingword.bible;

public record BibleReference(String translationId, String bookId, int chapter, int verse) {
    public BibleReference {
        if (isBlank(translationId)) throw new IllegalArgumentException("translationId is required");
        if (isBlank(bookId)) throw new IllegalArgumentException("bookId is required");
        if (chapter < 1) throw new IllegalArgumentException("chapter must be positive");
        if (verse < 1) throw new IllegalArgumentException("verse must be positive");
    }

    public String toStableId() {
        return translationId + ':' + bookId + ':' + chapter + ':' + verse;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
```

`BookId.java`:

```java
package com.livingword.bible;

import java.util.Locale;

public record BookId(String value) {
    public BookId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("book id is required");
        value = value.toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
```

`TranslationManifest.java`:

```java
package com.livingword.bible;

import java.util.List;

public record TranslationManifest(
    String id,
    String displayName,
    String language,
    String license,
    String attribution,
    String textDirection,
    List<String> bookOrder,
    String audioManifestId
) {
    public TranslationManifest {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("translation id is required");
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("display name is required");
        bookOrder = List.copyOf(bookOrder == null ? List.of() : bookOrder);
        textDirection = textDirection == null || textDirection.isBlank() ? "ltr" : textDirection;
    }
}
```

`ChapterData.java`:

```java
package com.livingword.bible;

import java.util.Map;

public record ChapterData(String translationId, String bookId, int chapter, Map<Integer, String> verses) {
    public ChapterData {
        if (chapter < 1) throw new IllegalArgumentException("chapter must be positive");
        verses = Map.copyOf(verses == null ? Map.of() : verses);
    }

    public String verseText(int verse) {
        return verses.getOrDefault(verse, "");
    }
}
```

**Step 5: Run tests to verify pass**

Run:

```powershell
.\gradlew.bat test --tests com.livingword.bible.*
```

Expected: PASS.

**Step 6: Commit**

```powershell
git add build.gradle src/main/java/com/livingword/bible src/test/java/com/livingword/bible
git commit -m "feat: add Bible translation domain model"
```

---

### Task 3: Implement Translation Resource Loader And Sample KJV Data

**Files:**
- Create: `src/main/java/com/livingword/bible/BibleDataManager.java`
- Create: `src/main/java/com/livingword/bible/BibleResourceLoader.java`
- Create: `src/main/java/com/livingword/bible/BibleSearchIndex.java`
- Create: `src/main/resources/data/livingword/bible/kjv/translation.json`
- Create: `src/main/resources/data/livingword/bible/kjv/books/john/003.json`
- Create: `src/main/resources/data/livingword/bible/kjv/books/psalms/023.json`
- Create: `src/test/java/com/livingword/bible/BibleDataManagerTest.java`

**Step 1: Write failing loader test**

`BibleDataManagerTest.java` should build a manager from in-memory manifests/chapters and verify translation-agnostic lookup:

```java
package com.livingword.bible;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BibleDataManagerTest {
    @Test
    void retrievesVerseByStableReference() {
        BibleDataManager manager = new BibleDataManager();
        manager.registerTranslation(new TranslationManifest("kjv", "King James Version", "en_us", "Public Domain", "", "ltr", List.of("john"), "kjv-default"));
        manager.registerChapter(new ChapterData("kjv", "john", 3, Map.of(16, "For God so loved the world...")));

        assertEquals("For God so loved the world...", manager.getVerse(new BibleReference("kjv", "john", 3, 16)).orElseThrow());
        assertTrue(manager.translations().stream().anyMatch(t -> t.id().equals("kjv")));
    }
}
```

**Step 2: Run test to verify failure**

Run:

```powershell
.\gradlew.bat test --tests com.livingword.bible.BibleDataManagerTest
```

Expected: FAIL because `BibleDataManager` does not exist.

**Step 3: Implement manager and search stub**

`BibleDataManager` should maintain maps keyed by translation id and `translation:book:chapter`. It should expose:

```java
public void registerTranslation(TranslationManifest manifest)
public void registerChapter(ChapterData chapter)
public List<TranslationManifest> translations()
public Optional<ChapterData> getChapter(String translationId, String bookId, int chapter)
public Optional<String> getVerse(BibleReference reference)
```

`BibleResourceLoader` can be a Minecraft-resource-facing skeleton for now with a `reload()` placeholder and comments explaining that resource parsing will load `data/*/bible/<translation>/translation.json` and chapter JSON files.

`BibleSearchIndex` should expose a minimal API:

```java
public List<BibleReference> search(String translationId, String query, int limit)
```

Return an empty list until full indexing is implemented, but keep the service injectable into the GUI.

**Step 4: Add sample KJV resources**

`translation.json`:

```json
{
  "id": "kjv",
  "displayName": "King James Version",
  "language": "en_us",
  "license": "Public Domain",
  "attribution": "King James Version, 1769 Oxford edition",
  "textDirection": "ltr",
  "bookOrder": ["genesis", "exodus", "john", "psalms"],
  "audioManifestId": "kjv-default"
}
```

`john/003.json` should include at least John 3:16-17 as sample KJV text. `psalms/023.json` should include Psalm 23:1-4. Keep this small in the first commit; full import is a later task.

**Step 5: Run tests**

Run:

```powershell
.\gradlew.bat test --tests com.livingword.bible.*
```

Expected: PASS.

**Step 6: Commit**

```powershell
git add src/main/java/com/livingword/bible src/main/resources/data src/test/java/com/livingword/bible
git commit -m "feat: add data-driven Bible loader foundation"
```

---

### Task 4: Register Config, Items, And Bible Item

**Files:**
- Create: `src/main/java/com/livingword/config/LivingWordConfig.java`
- Create: `src/main/java/com/livingword/items/LivingWordItems.java`
- Create: `src/main/java/com/livingword/items/BibleItem.java`
- Create: `src/main/resources/assets/livingword/lang/en_us.json`
- Create: `src/main/resources/assets/livingword/models/item/bible.json`
- Create: `src/main/resources/assets/livingword/textures/item/bible.png` or placeholder generated texture
- Create: `src/main/resources/data/livingword/recipe/bible.json`

**Step 1: Add config spec**

`LivingWordConfig` should register common/client specs through `ModContainer#registerConfig`. Include config values for:

```java
cdnBaseUrl
cacheLimitMegabytes
subtitleEnabled
syncToleranceMillis
autoplayJoinedSessions
dailyVerseEnabled
narrationVolume
verseDisplayStyle
```

**Step 2: Register item**

`LivingWordItems` should use `DeferredRegister.Items` and register:

```java
public static final DeferredItem<BibleItem> BIBLE = ITEMS.registerItem("bible", BibleItem::new, new Item.Properties().stacksTo(1));
```

Also register an optional creative mode tab entry if the current NeoForge API supports it cleanly.

**Step 3: Implement item behavior**

`BibleItem#use`:

- On server: if not crouching, send/open Bible screen through the screen opening path.
- If crouching, send a translatable message that nearby listening sessions are not implemented in this build.
- On client: return success without doing server work.

Use a clean placeholder if menu opening needs a dedicated screen handler in the next task.

**Step 4: Add assets and recipe**

Use a simple placeholder texture if image generation is not being done. Recipe can be book + gold nugget + paper, staying vanilla-friendly.

**Step 5: Verify compile**

Run:

```powershell
.\gradlew.bat compileJava
```

Expected: PASS.

**Step 6: Commit**

```powershell
git add src/main/java/com/livingword/config src/main/java/com/livingword/items src/main/resources/assets src/main/resources/data/livingword/recipe src/main/java/com/livingword/LivingWord.java
git commit -m "feat: add configurable Bible item"
```

---

### Task 5: Add Client GUI Foundation

**Files:**
- Create: `src/main/java/com/livingword/client/LivingWordClient.java`
- Create: `src/main/java/com/livingword/client/gui/BibleScreen.java`
- Create: `src/main/java/com/livingword/client/gui/BibleGuiState.java`
- Create: `src/main/java/com/livingword/client/gui/widgets/VerseListWidget.java`
- Modify: `src/main/java/com/livingword/items/BibleItem.java`
- Modify: `src/main/resources/assets/livingword/lang/en_us.json`

**Step 1: Implement GUI state model first**

`BibleGuiState` should track:

```java
String translationId
String bookId
int chapter
int selectedVerse
String searchQuery
List<BibleReference> recentHistory
List<BibleReference> bookmarks
```

Keep this pure Java where possible.

**Step 2: Build screen skeleton**

`BibleScreen` should extend `Screen`, draw a dark parchment-style background, title, translation selector placeholder, book/chapter controls, search box, verse list area, and footer buttons. Keep controls compact and readable.

Do not put client GUI classes in common packages.

**Step 3: Wire Bible item to open screen**

Use a client packet or client-only handler as appropriate so right-click opens `BibleScreen` for the local player. If a network screen factory is too heavy for this first pass, use a server acknowledgement packet that tells the client to open the local screen.

**Step 4: Add copy-to-chat behavior**

Implement a GUI button/action that copies the selected verse text into the chat input or clipboard. Prefer clipboard for the initial implementation if chat insertion is unstable.

**Step 5: Run client compile**

Run:

```powershell
.\gradlew.bat compileJava
```

Expected: PASS.

**Step 6: Commit**

```powershell
git add src/main/java/com/livingword/client src/main/java/com/livingword/items src/main/resources/assets/livingword/lang/en_us.json
git commit -m "feat: add Bible reading screen foundation"
```

---

### Task 6: Add Networking And Session Sync Models

**Files:**
- Create: `src/main/java/com/livingword/network/LivingWordNetwork.java`
- Create: `src/main/java/com/livingword/network/payload/OpenBiblePayload.java`
- Create: `src/main/java/com/livingword/network/payload/JoinListeningSessionPayload.java`
- Create: `src/main/java/com/livingword/network/payload/LeaveListeningSessionPayload.java`
- Create: `src/main/java/com/livingword/network/payload/PlaybackControlPayload.java`
- Create: `src/main/java/com/livingword/network/payload/TimestampCorrectionPayload.java`
- Create: `src/main/java/com/livingword/sync/ListeningSession.java`
- Create: `src/main/java/com/livingword/sync/ListeningSessionManager.java`
- Create: `src/main/java/com/livingword/sync/PlaybackState.java`
- Create: `src/test/java/com/livingword/sync/ListeningSessionTest.java`

**Step 1: Write failing session tests**

Test that a playing session calculates elapsed position from server time and that paused sessions freeze position:

```java
package com.livingword.sync;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ListeningSessionTest {
    @Test
    void playingSessionAdvancesFromStartTime() {
        ListeningSession session = ListeningSession.started("kjv", "john", 3, 1_000L);
        assertEquals(5_000L, session.positionMillisAt(6_000L));
    }

    @Test
    void pausedSessionKeepsPausedPosition() {
        ListeningSession session = ListeningSession.started("kjv", "john", 3, 1_000L).pauseAt(6_000L);
        assertEquals(5_000L, session.positionMillisAt(20_000L));
    }
}
```

**Step 2: Run tests to verify failure**

Run:

```powershell
.\gradlew.bat test --tests com.livingword.sync.ListeningSessionTest
```

Expected: FAIL because sync classes do not exist.

**Step 3: Implement sync model**

`ListeningSession` should be immutable or mutation-minimal and store translation id, book id, chapter, state, start server millis, paused position millis, and participants. `ListeningSessionManager` should create, join, leave, play, pause, seek, and snapshot sessions.

**Step 4: Register payload classes**

Use NeoForge custom payload registration. Payloads should carry only metadata:

- open screen
- session id
- translation id
- book id
- chapter
- playback position millis
- server timestamp millis
- source position if present

No audio bytes are ever sent.

**Step 5: Run tests and compile**

Run:

```powershell
.\gradlew.bat test --tests com.livingword.sync.*
.\gradlew.bat compileJava
```

Expected: PASS.

**Step 6: Commit**

```powershell
git add src/main/java/com/livingword/network src/main/java/com/livingword/sync src/test/java/com/livingword/sync src/main/java/com/livingword/LivingWord.java
git commit -m "feat: add listening session networking foundation"
```

---

### Task 7: Add Audio Manifest, Cache, And Download Service Skeleton

**Files:**
- Create: `src/main/java/com/livingword/audio/AudioChapterId.java`
- Create: `src/main/java/com/livingword/audio/AudioManifest.java`
- Create: `src/main/java/com/livingword/audio/AudioCacheManager.java`
- Create: `src/main/java/com/livingword/audio/AudioDownloadService.java`
- Create: `src/main/java/com/livingword/audio/AudioPlaybackService.java`
- Create: `src/main/java/com/livingword/audio/DownloadState.java`
- Create: `src/test/java/com/livingword/audio/AudioCacheManagerTest.java`

**Step 1: Write failing cache path test**

```java
package com.livingword.audio;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AudioCacheManagerTest {
    @Test
    void buildsStableChapterCachePath() {
        AudioCacheManager cache = new AudioCacheManager(Path.of(".minecraft", "livingword", "cache", "audio"));
        Path path = cache.chapterAudioPath(new AudioChapterId("kjv", "john", 3));
        assertEquals(Path.of(".minecraft", "livingword", "cache", "audio", "kjv", "john", "john_003.ogg"), path);
    }
}
```

**Step 2: Run test to verify failure**

Run:

```powershell
.\gradlew.bat test --tests com.livingword.audio.AudioCacheManagerTest
```

Expected: FAIL because audio classes do not exist.

**Step 3: Implement audio foundation**

`AudioChapterId` stores translation/audio pack id, book id, chapter. `AudioCacheManager` produces audio and timestamp paths and exposes `isCached`, `markCorrupt`, and `temporaryDownloadPath` methods. `AudioDownloadService` exposes asynchronous method signatures using `CompletableFuture<DownloadState>` but may return `DownloadState.notImplemented()` in this build. `AudioPlaybackService` is an interface with `play`, `pause`, `seek`, `stop`.

**Step 4: Run tests**

Run:

```powershell
.\gradlew.bat test --tests com.livingword.audio.*
```

Expected: PASS.

**Step 5: Commit**

```powershell
git add src/main/java/com/livingword/audio src/test/java/com/livingword/audio
git commit -m "feat: add audio cache and playback service foundation"
```

---

### Task 8: Add Lectern And Scripture Disc Skeletons

**Files:**
- Create: `src/main/java/com/livingword/lectern/LecternListeningStation.java`
- Create: `src/main/java/com/livingword/lectern/LecternEvents.java`
- Create: `src/main/java/com/livingword/discs/ScriptureDisc.java`
- Create: `src/main/java/com/livingword/discs/ScriptureDiscRegistry.java`
- Modify: `src/main/java/com/livingword/items/LivingWordItems.java`
- Modify: `src/main/resources/assets/livingword/lang/en_us.json`
- Create: `src/main/resources/assets/livingword/models/item/scripture_disc_john.json`
- Create: `src/main/resources/data/livingword/recipe/scripture_disc_john.json`

**Step 1: Add metadata classes**

`ScriptureDisc` should store display key, translation id, book id, start chapter, end chapter, and optional audio manifest id. It does not need full jukebox integration yet.

`LecternListeningStation` should represent a source position and currently selected Bible reference.

**Step 2: Register one prototype disc**

Register `scripture_disc_john` as a prototype item. Tooltip should make clear it is a scripture listening disc foundation.

**Step 3: Add event skeletons**

`LecternEvents` should contain subscription methods or TODO-safe registration points for detecting Bible insertion/interaction. Keep them compile-safe and side-correct.

**Step 4: Verify compile**

Run:

```powershell
.\gradlew.bat compileJava
```

Expected: PASS.

**Step 5: Commit**

```powershell
git add src/main/java/com/livingword/lectern src/main/java/com/livingword/discs src/main/java/com/livingword/items src/main/resources/assets src/main/resources/data/livingword/recipe
git commit -m "feat: add lectern and scripture disc foundations"
```

---

### Task 9: Add Documentation And Verification Pass

**Files:**
- Modify: `README.md`
- Create: `docs/translation-packs.md`
- Create: `docs/audio-packs.md`
- Create: `docs/testing.md`

**Step 1: Document setup**

README should include:

```text
Requirements:
- Java 21
- Minecraft Java Edition 1.21.1
- NeoForge 1.21.1

Build:
.\gradlew.bat build

Run client:
.\gradlew.bat runClient
```

**Step 2: Document translation packs**

`docs/translation-packs.md` should describe the manifest and chapter JSON layout, including the fact that KJV is bundled but other translations can be provided by datapacks/mods if legal permissions allow.

**Step 3: Document audio packs**

`docs/audio-packs.md` should describe one OGG per chapter, timestamp JSON, CDN manifests, cache layout, and that clients fetch audio independently while the server syncs metadata only.

**Step 4: Run full verification**

Run:

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

Expected: PASS. If dependency or API drift occurs, update the exact NeoForge API usage and rerun until green.

**Step 5: Commit**

```powershell
git add README.md docs build.gradle src
git commit -m "docs: document Living Word extension points"
```

---

## Completion Checklist

Before calling the initial implementation complete:

- `git status --short` only shows intentional changes or is clean.
- `./gradlew.bat test` passes.
- `./gradlew.bat build` passes.
- The mod jar is produced under `build/libs`.
- Bible translation data is not hardcoded to KJV.
- Server networking never sends audio bytes.
- Client-only GUI/audio classes are not referenced from common/server-only initialization.
- README clearly says bundled KJV is public domain and other translations require legal permission or public-domain sources.
