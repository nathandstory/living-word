# Word Sync Research

## Goal

Floating lectern text should highlight the word being spoken, without requiring players to install anything beyond the mod.

## Runtime Constraint

Minecraft should not run speech recognition or forced alignment during normal gameplay. That would add large native dependencies, high CPU use, model downloads, and multiplayer timing drift. Runtime should only read compact timing sidecars:

```json
{
  "verses": { "1": 0.0, "2": 14.2 },
  "words": {
    "1": [
      { "text": "For", "start": 0.0, "end": 0.35 },
      { "text": "God", "start": 0.35, "end": 0.8 }
    ]
  }
}
```

## Viable Sources

1. Provider timings when available.
   - BibleBrain / Faith Comes By Hearing exposes Bible audio and timing-oriented API capabilities, but access and coverage are provider-dependent and may require an API key.
   - This is the cleanest option for verse timings.

2. Offline forced alignment for word timings.
   - WhisperX, Montreal Forced Aligner, aeneas, and Gentle are existing forced-alignment/transcription alignment tools.
   - This should be run as a release/build pipeline per translation, narrator, book, and chapter.
   - The generated sidecars can be bundled in the jar or downloaded as tiny JSON assets. Players still only install the mod.

3. Lightweight estimated timing fallback.
   - Useful only as a visual fallback when exact timing is missing.
   - It must not be presented as exact synchronization.

## Recommendation

Use provider verse timings where we can legally and reliably get them. For the BSB HelloAO narrators, generate word-level sidecars offline with a forced-alignment pipeline and bundle the approved sidecars. That gives the in-game feature the "just works" behavior while keeping the game client/server light.

## Implementation Status

The mod now treats timed verse display as an opt-in capability of an audio manifest. A source must declare `verseTimings: true`, and the matching chapter must have a sidecar at:

`data/livingword/audio/<translation>/<manifest>/<book>/<book_###.timestamps.json`

If either part is missing, the lectern verse display button is disabled and the server renders no floating verse text for that source. This avoids showing unsynchronized or guessed verse text as if it were real timing data.

## Sources To Revisit

- Freesound shofar source: https://freesound.org/people/jpors/sounds/69547/
- BibleBrain developer docs: https://www.faithcomesbyhearing.com/bible-brain/developer-documentation
- WhisperX: https://github.com/m-bain/whisperX
- Montreal Forced Aligner: https://montreal-forced-aligner.readthedocs.io/
- aeneas: https://www.readbeyond.it/aeneas/
- Gentle: https://github.com/strob/gentle
