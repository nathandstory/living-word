package com.livingword.audio;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class EbibleWebAudioIndexTest {
    @Test
    void resolvesJohnChapterThreeFromDirectoryListing() {
        String html = """
            <html><body>
            <a href="0997%20John-Chapter%20One.mp3">0997 John-Chapter One.mp3</a>
            <a href="0999%20John-Chapter%20Three.mp3">0999 John-Chapter Three.mp3</a>
            </body></html>
            """;

        URI uri = EbibleWebAudioIndex.resolveChapterUri(
            URI.create("https://ebible.org/eng-web/audio/"),
            new AudioChapterId("webp", "john", 3),
            html
        );

        assertEquals(URI.create("https://ebible.org/eng-web/audio/43_John/0999%20John-Chapter%20Three.mp3"), uri);
    }

    @Test
    void resolvesOldTestamentUnderscoreFilenames() {
        String html = """
            <html><body>
            <a href="01_20_Genesis_Chapter_Twenty.mp3">01_20_Genesis_Chapter_Twenty.mp3</a>
            <a href="01_21_Genesis_Chapter_Twenty_One.mp3">01_21_Genesis_Chapter_Twenty_One.mp3</a>
            </body></html>
            """;

        URI uri = EbibleWebAudioIndex.resolveChapterUri(
            URI.create("https://ebible.org/eng-web/audio/"),
            new AudioChapterId("webp", "genesis", 21),
            html
        );

        assertEquals(URI.create("https://ebible.org/eng-web/audio/01_Genesis/01_21_Genesis_Chapter_Twenty_One.mp3"), uri);
    }
}
