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

Manual client checks should verify that the Bible item appears, right-click opens the Bible screen, sample KJV verses render, the copy button writes to the clipboard, and the prototype Scripture Disc item registers.
