# KJV Import Pipeline

Living Word stores Bible text as translation packs under:

```text
data/livingword/bible/<translation>/
```

KJV is the default bundled translation because the King James Version is usable as public-domain text in the United States. Project Gutenberg lists "The Bible, King James Version, Complete" with copyright status "Public domain in the USA" and provides UTF-8 text downloads:

```text
https://www.gutenberg.org/ebooks/30
```

CrossWire also maintains a KJV module with OSIS XML markup and detailed provenance notes:

```text
https://wiki.crosswire.org/CrossWire_KJV
```

Use the import utility with text that is already verse-marked as `chapter:verse text`. Project Gutenberg text is a good public-domain source, but always inspect the parsed output before shipping because source formatting can change and some KJV sources include Apocrypha.

## Output Contract

The generated files are:

```text
translation.json
index.json
books/<book>/<chapter>.json
```

Example chapter file:

```json
{
  "translationId": "kjv",
  "bookId": "john",
  "chapter": 3,
  "verses": {
    "16": "For God so loved the world..."
  }
}
```

`index.json` lists every generated chapter, which lets the mod load resources from a packaged jar without trying to scan directories.

## Running The Import

From the repository root:

```powershell
python tools\kjv-import\kjv_to_livingword.py path\to\kjv.txt src\main\resources\data\livingword\bible\kjv
```

After importing:

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

## Translation Packs Beyond KJV

Do not import modern translations unless the translation owner permits redistribution in a Minecraft mod. Translation packs should keep accurate `license` and `attribution` fields in `translation.json`, and should use their own translation id such as `web`, `asv`, or a publisher-approved id.
