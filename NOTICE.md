# Living Word Notices

This file documents third-party content and generated data used by Living Word.

The MIT license in `LICENSE` applies to this project's source code unless a file or asset states otherwise. Bible text, streamed audio, generated timing data, and media assets may have separate terms.

## Bible Text

### Berean Standard Bible

- Used for bundled `bsb` Bible data.
- Source: Berean Bible / HelloAO Free Use Bible API.
- License note: Berean Bible states that the Berean Standard Bible was dedicated to the public domain on April 30, 2023.
- Sources:
  - https://berean.bible/licensing.htm
  - https://bible.helloao.org/

### King James Version

- Used for bundled `kjv` Bible data.
- Source attribution in data manifest: Project Gutenberg eBook #30, The Bible, King James Version, Complete.
- License note: public domain in the United States.
- Source: https://www.gutenberg.org/ebooks/30

### World English Bible Protestant

- Used for bundled `webp` Bible data.
- Source attribution in data manifest: World English Bible Protestant, eBible.org ENGWEBP, 2020 stable text edition.
- License note: public domain.
- Source: https://ebible.org/web/

## Streamed Audio Providers

Living Word does not bundle full Bible chapter audio files in the jar. Clients stream and cache chapter audio on demand.

### BSB Audio

- Manifests:
  - `data/livingword/audio/bsb/default.json`
  - `data/livingword/audio/bsb/souer.json`
  - `data/livingword/audio/bsb/hays.json`
- Source: HelloAO / OpenBible audio API.
- Use: streamed and cached by the client.
- Source: https://bible.helloao.org/

### KJV Audio

- Manifest: `data/livingword/audio/kjv/default.json`
- Source: AudioTreasure KJV voice-only narration.
- Use: streamed and cached by the client.
- Note: this mod is intended for non-commercial ministry use. Do not redistribute mirrored KJV audio files in the repository or jar without separate permission.
- Source: https://www.audiotreasure.com/

### WEBP Audio

- Manifest: `data/livingword/audio/webp/default.json`
- Source: PublicDomainAudioBibles.com, World English Bible narrated by David Williams.
- Use: streamed and cached by the client.
- Source: https://publicdomainaudiobibles.com/

## Timing Data

Bundled timestamp sidecars under `data/livingword/audio/**/**/*.timestamps.json` were generated for scripture synchronization from the matching Bible text and streamed narration sources.

These timing files contain synchronization metadata only. They do not contain chapter audio.

## Sound Effects

### Shofar

`assets/livingword/sounds/shofar_blow.ogg` is derived from `Sjofar.wav` by `jpors` on Freesound.

- Source: https://freesound.org/people/jpors/sounds/69547/
- License: Creative Commons Sampling+ 1.0
- Changes: trimmed, mixed to mono, filtered, compressed, volume-shaped, faded, and encoded as OGG Vorbis for in-game use.

## Artwork

Living Word item textures, models, GUI art, and logo assets are project assets unless otherwise noted.
