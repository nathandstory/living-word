# Translation Packs

Living Word ships with public-domain KJV sample data, but translations are intended to be data-driven.

Example layout:

```text
data/livingword/bible/kjv/translation.json
data/livingword/bible/kjv/books/john/003.json
```

Translation manifests describe identity and legal metadata:

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

Chapter files store verse text by verse number:

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

Bookmarks, history, and playback references use stable identifiers:

```text
translation_id + book_id + chapter + verse
```

This allows KJV, WEB, ASV, or separately licensed translations to coexist without changing player data.
