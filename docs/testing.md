# Testing

Use Java 21 and the checked-in Gradle wrapper.

Run all tests:

```powershell
.\gradlew.bat test
```

Build the mod jar:

```powershell
.\gradlew.bat build
```

Run focused tests:

```powershell
.\gradlew.bat test --tests com.livingword.bible.*
.\gradlew.bat test --tests com.livingword.sync.ListeningSessionTest
.\gradlew.bat test --tests com.livingword.audio.AudioCacheManagerTest
```

Current automated coverage focuses on pure Java behavior:

- stable Bible references
- translation metadata
- Bible data manager lookups
- GUI state bookkeeping
- listening session timestamp math
- audio cache path generation
- Scripture Disc and lectern session conflict behavior
- generated asset and timing data contracts

Manual client checks should verify:

- `/give @p livingword:bible` gives the Bible item.
- Right-clicking the Bible opens the Bible screen.
- Search finds expected verses across translations.
- Highlighted verses appear in the highlighted tab across translations.
- The Bible audio button starts and stops playback cleanly.
- `/give @p livingword:scripture_disc` gives the Scripture Disc.
- Scripture Disc selection, jukebox playback, pause/resume, distance falloff, and chapter continuation work.
- Lectern playback starts, pauses, resets, and updates floating verse display.
- `/give @p livingword:shofar` gives the shofar and the sound plays for nearby players.
- A dedicated server can run with the same jar.
