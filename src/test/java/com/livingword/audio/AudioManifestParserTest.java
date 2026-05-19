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

    @Test
    void parsesExplicitMp3ChapterPaths() {
        String json = """
            {
              "id": "webp-ebible",
              "translationId": "webp",
              "baseUri": "https://ebible.org/eng-web/audio/",
              "fileExtension": "mp3",
              "chapterPaths": {
                "john:3": "43_John/0999 John-Chapter Three.mp3"
              }
            }
            """;

        AudioManifest manifest = new AudioManifestParser().parse(new StringReader(json));

        AudioChapterId john3 = new AudioChapterId("webp", "john", 3);
        assertEquals("mp3", manifest.fileExtension());
        assertEquals(URI.create("https://ebible.org/eng-web/audio/43_John/0999%20John-Chapter%20Three.mp3"), manifest.chapterUri(john3));
    }

    @Test
    void parsesDirectoryIndexPathStrategy() {
        String json = """
            {
              "id": "webp-default",
              "translationId": "webp",
              "baseUri": "https://ebible.org/eng-web/audio/",
              "fileExtension": "mp3",
              "pathStrategy": "ebible-web-directory"
            }
            """;

        AudioManifest manifest = new AudioManifestParser().parse(new StringReader(json));

        assertEquals("ebible-web-directory", manifest.pathStrategy());
    }

    @Test
    void preservesAbsoluteExplicitChapterUrls() {
        String json = """
            {
              "id": "webp-custom",
              "translationId": "webp",
              "baseUri": "https://cdn.example.test/audio/",
              "fileExtension": "mp3",
              "chapterPaths": {
                "john:3": "https://media.example.test/John Chapter Three.mp3"
              }
            }
            """;

        AudioManifest manifest = new AudioManifestParser().parse(new StringReader(json));

        assertEquals(URI.create("https://media.example.test/John%20Chapter%20Three.mp3"), manifest.chapterUri(new AudioChapterId("webp", "john", 3)));
    }
}
