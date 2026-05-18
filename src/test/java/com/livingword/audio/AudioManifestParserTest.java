package com.livingword.audio;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AudioManifestParserTest {
    @Test
    void parsesChapterHashesAndBaseUri() {
        String json = """
            {
              "id": "webp-default",
              "translationId": "webp",
              "baseUri": "https://cdn.example.test/livingword/webp/",
              "chapters": {
                "john:3": "abc123",
                "psalms:23": "def456"
              }
            }
            """;

        AudioManifest manifest = new AudioManifestParser().parse(new StringReader(json));

        AudioChapterId john3 = new AudioChapterId("webp", "john", 3);
        assertEquals("webp-default", manifest.id());
        assertEquals("webp", manifest.translationId());
        assertEquals(URI.create("https://cdn.example.test/livingword/webp/john/john_003.ogg"), manifest.chapterUri(john3));
        assertEquals("abc123", manifest.expectedHash(john3).orElseThrow());
    }
}
