# Audio Packs

Audio is intentionally not bundled in the mod jar. Clients fetch or stream chapter audio independently while the server synchronizes only metadata and playback timestamps.

Chapter audio should be one OGG Vorbis file per chapter:

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

The first build includes:

- `AudioChapterId`
- `AudioManifest`
- `AudioCacheManager`
- `AudioDownloadService`
- `AudioPlaybackService`
- `DownloadState`

Future work should add resumable downloads, hash verification, stream-and-save playback, corrupt-file recovery, progress UI, and real OGG playback integration with the Minecraft client sound engine.
