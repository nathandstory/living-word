package com.livingword.bible;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BibleJsonParserTest {
    private final BibleJsonParser parser = new BibleJsonParser();

    @Test
    void parsesTranslationManifest() {
        String json = """
            {
              "id": "web",
              "displayName": "World English Bible",
              "language": "en_us",
              "license": "Public Domain",
              "attribution": "World English Bible",
              "textDirection": "ltr",
              "bookOrder": ["genesis", "john"],
              "audioManifestId": "web-default"
            }
            """;

        TranslationManifest manifest = parser.parseManifest(new StringReader(json));

        assertEquals("web", manifest.id());
        assertEquals("World English Bible", manifest.displayName());
        assertEquals(List.of("genesis", "john"), manifest.bookOrder());
        assertEquals("web-default", manifest.audioManifestId());
    }

    @Test
    void parsesTranslationIndex() {
        String json = """
            {
              "translationId": "kjv",
              "books": {
                "john": [1, 2, 3],
                "psalms": [23]
              }
            }
            """;

        BibleTranslationIndex index = parser.parseIndex(new StringReader(json));

        assertEquals("kjv", index.translationId());
        assertEquals(List.of("john", "psalms"), index.bookIds());
        assertEquals(List.of(1, 2, 3), index.chapters("john"));
        assertEquals(List.of(23), index.chapters("psalms"));
    }

    @Test
    void parsesChapterVerseKeysAsIntegers() {
        String json = """
            {
              "translationId": "kjv",
              "bookId": "john",
              "chapter": 3,
              "verses": {
                "16": "For God so loved the world...",
                "17": "For God sent not his Son..."
              }
            }
            """;

        ChapterData chapter = parser.parseChapter(new StringReader(json));

        assertEquals("kjv", chapter.translationId());
        assertEquals("john", chapter.bookId());
        assertEquals(3, chapter.chapter());
        assertEquals("For God so loved the world...", chapter.verseText(16));
        assertEquals("For God sent not his Son...", chapter.verseText(17));
    }
}
