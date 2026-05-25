# Data-Backed Bible Reader Design

Date: 2026-05-17

## Purpose

This slice replaces the initial hardcoded John 3 GUI sample with a resource-backed reader. The player should open the Bible item and see content loaded from bundled translation files, with the same path usable later by full KJV imports and non-KJV translation packs.

## Scope

- Load translation manifests, chapter indexes, and chapter JSON files from `data/livingword/bible/<translation>`.
- Keep KJV as the bundled default while avoiding KJV-only code paths.
- Add a searchable in-memory index over loaded chapters.
- Add book/chapter navigation to the Bible GUI.
- Keep bookmarks, history, verse selection, and copy behavior attached to stable references.
- Document the KJV import pipeline so full public-domain data can be generated into the same resource format.

This slice does not implement production audio, external CDN fetching, or a full public-domain KJV dump in the repository. It establishes the format and code path that full data will use.

## Resource Format

Each translation has three resource types:

```text
data/livingword/bible/<translation>/translation.json
data/livingword/bible/<translation>/index.json
data/livingword/bible/<translation>/books/<book>/<chapter>.json
```

`translation.json` describes the translation metadata, license, attribution, text direction, book order, and optional audio manifest id.

`index.json` lists which chapter files are available. This avoids jar resource directory scanning, works in dev and packaged jars, and gives translation packs a stable contract.

`books/<book>/<chapter>.json` stores chapter text with verse numbers as strings in JSON and integers in Java.

## Client Loading

Client code will use a small repository facade that loads bundled resources through the class loader. The facade returns a shared `BibleDataManager` instance so screens can be opened repeatedly without reparsing every resource.

The loading layer is pure Java and testable. Minecraft-specific GUI code should only ask for already-loaded chapters, translation lists, and search results.

## Search

Search is intentionally simple for this slice:

- case-insensitive substring matching
- searches only loaded chapters
- stable `BibleReference` results
- deterministic ordering by translation book order, chapter, then verse
- caller-provided result limit

This can later be replaced with tokenization, highlighting ranges, or async indexing without changing packet or GUI references.

## GUI Behavior

The GUI will:

- select the first loaded translation/book/chapter by default
- show the current translation, book, and chapter
- provide previous/next chapter buttons
- provide previous/next book buttons when multiple books are available
- search loaded text and jump to the first result
- copy the selected verse text from loaded data
- preserve bookmarks and recent history through `BibleGuiState`

The screen remains intentionally compact and vanilla-adjacent until a later visual polish pass adds smooth scrolling and richer selectors.

## KJV Pipeline

KJV remains the default bundled translation because it is public domain. The full-data pipeline should ingest a public-domain KJV source, normalize book ids, split chapters into per-chapter JSON, and emit `index.json`.

The generator must preserve license/attribution metadata and should never be used for copyrighted translations unless the translation pack author provides clear redistribution rights.
