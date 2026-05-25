# Audio Packs

Full chapter audio is intentionally not bundled in the mod jar. Clients fetch or stream chapter audio independently while the server synchronizes only metadata and playback timestamps.

Chapter audio should be one compressed file per chapter. OGG Vorbis is preferred for first-party or custom packs, but bundled remote providers may expose MP3 when that is the legal public source:

```text
audio/kjv/john/john_003.ogg
audio/kjv/john/john_003.timestamps.json
```

Local cache target:

```text
.minecraft/livingword/cache/audio/kjv/john/john_003.ogg
.minecraft/livingword/cache/audio/kjv/john/john_003.timestamps.json
```

Verse timestamps should map verse numbers to seconds:

```json
{
  "1": 0.0,
  "2": 14.2,
  "3": 28.6
}
```

Bundled provider policy:

- `bsb/default`: Berean Standard Bible, David narrator from HelloAO/OpenBible.
- `bsb/souer`: Berean Standard Bible, Souer narrator from HelloAO/OpenBible.
- `bsb/hays`: Berean Standard Bible, Hays narrator from HelloAO/OpenBible.
- `webp/default`: World English Bible, David Williams narration from PublicDomainAudioBibles.
- `kjv/default`: King James Version narration from AudioTreasure.
- NKJV, NIV, ESV, and similar copyrighted translations must not be bundled or silently auto-streamed from consumer sites. They should be added only through a licensed provider or a player/server configured bring-your-own audio pack.

Restricted provider manifests should use:

```json
{
  "id": "nkjv-example",
  "translationId": "nkjv",
  "baseUri": "https://licensed.example.test/nkjv/",
  "fileExtension": "mp3",
  "pathStrategy": "restricted-licensed-provider"
}
```

That strategy intentionally fails with a clear licensing message until a real licensed resolver or BYO direct chapter paths are configured.

The current build includes:

- `AudioChapterId`
- `AudioManifest`
- `AudioCacheManager`
- `AudioDownloadService`
- `AudioPlaybackService`
- `DownloadState`
- generated verse/word timing sidecars for supported KJV and BSB provider manifests

Future work should add resumable downloads, hash verification, corrupt-file recovery, richer progress UI, and more licensed narrator packs.
