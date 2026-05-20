package com.livingword.audio;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class DefaultAudioChapterUriResolverTest {
    @Test
    void resolvesEbibleDirectoryStrategyThroughBookIndex() throws Exception {
        String html = """
            <html><body>
            <a href="0999%20John-Chapter%20Three.mp3">0999 John-Chapter Three.mp3</a>
            </body></html>
            """;
        DefaultAudioChapterUriResolver resolver = new DefaultAudioChapterUriResolver(uri ->
            new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8))
        );
        AudioChapterId chapterId = new AudioChapterId("webp", "john", 3);
        AudioManifest manifest = new AudioManifest(
            "webp-default",
            "webp",
            URI.create("https://ebible.org/eng-web/audio/"),
            "mp3",
            "ebible-web-directory",
            Map.of(),
            Map.of()
        );

        URI uri = resolver.resolve(manifest, chapterId);

        assertEquals(URI.create("https://ebible.org/eng-web/audio/43_John/0999%20John-Chapter%20Three.mp3"), uri);
    }

    @Test
    void resolvesAudioTreasureKjvChapterPaths() throws Exception {
        DefaultAudioChapterUriResolver resolver = new DefaultAudioChapterUriResolver(uri -> {
            throw new AssertionError("AudioTreasure strategy should not read directory listings");
        });
        AudioChapterId chapterId = new AudioChapterId("kjv", "john", 3);
        AudioManifest manifest = new AudioManifest(
            "kjv-audiotreasure-voice",
            "kjv",
            URI.create("https://www.audiotreasure.com/content/KJV_AT/"),
            "mp3",
            "audiotreasure-kjv",
            Map.of(),
            Map.of()
        );

        URI uri = resolver.resolve(manifest, chapterId);

        assertEquals(URI.create("https://www.audiotreasure.com/content/KJV_AT/43_John003.mp3"), uri);
    }

    @Test
    void resolvesPublicDomainAudioBiblesWebChapterPaths() throws Exception {
        DefaultAudioChapterUriResolver resolver = new DefaultAudioChapterUriResolver(uri -> {
            throw new AssertionError("PublicDomainAudioBibles strategy should not read directory listings");
        });
        AudioChapterId chapterId = new AudioChapterId("webp", "1_corinthians", 13);
        AudioManifest manifest = new AudioManifest(
            "webp-david-williams",
            "webp",
            URI.create("https://publicdomainaudiobibles.com/content/mp3/WEBD/"),
            "mp3",
            "public-domain-audio-bibles",
            Map.of(),
            Map.of()
        );

        URI uri = resolver.resolve(manifest, chapterId);

        assertEquals(URI.create("https://publicdomainaudiobibles.com/content/mp3/WEBD/46_1Cor/46_1Corinthians_13.mp3"), uri);
    }

    @Test
    void resolvesHelloAoBsbDavidChapterPaths() throws Exception {
        DefaultAudioChapterUriResolver resolver = new DefaultAudioChapterUriResolver(uri -> {
            throw new AssertionError("HelloAO strategy should not read directory listings");
        });
        AudioChapterId chapterId = new AudioChapterId("bsb", "john", 3);
        AudioManifest manifest = new AudioManifest(
            "bsb-helloao-david",
            "bsb",
            URI.create("https://audio.bible.helloao.org/api/BSB/"),
            "mp3",
            "helloao-bsb-david",
            Map.of(),
            Map.of()
        );

        URI uri = resolver.resolve(manifest, chapterId);

        assertEquals(URI.create("https://audio.bible.helloao.org/api/BSB/JHN/3/audio/david.mp3"), uri);
    }

    @Test
    void resolvesHelloAoBsbAlternateNarratorChapterPaths() throws Exception {
        DefaultAudioChapterUriResolver resolver = new DefaultAudioChapterUriResolver(uri -> {
            throw new AssertionError("HelloAO strategy should not read directory listings");
        });
        AudioChapterId chapterId = new AudioChapterId("bsb", "psalms", 23);
        AudioManifest manifest = new AudioManifest(
            "bsb-helloao-hays",
            "bsb",
            URI.create("https://audio.bible.helloao.org/api/BSB/"),
            "mp3",
            "helloao-bsb-hays",
            Map.of(),
            Map.of()
        );

        URI uri = resolver.resolve(manifest, chapterId);

        assertEquals(URI.create("https://audio.bible.helloao.org/api/BSB/PSA/23/audio/hays.mp3"), uri);
    }

    @Test
    void rejectsRestrictedLicensedProviderWithoutConfiguredSource() {
        DefaultAudioChapterUriResolver resolver = new DefaultAudioChapterUriResolver(uri -> {
            throw new AssertionError("Restricted strategy should not read directory listings");
        });
        AudioChapterId chapterId = new AudioChapterId("nkjv", "john", 3);
        AudioManifest manifest = new AudioManifest(
            "nkjv-restricted",
            "nkjv",
            URI.create("https://licensed.example.test/nkjv/"),
            "mp3",
            "restricted-licensed-provider",
            Map.of(),
            Map.of()
        );

        IOException exception = assertThrows(IOException.class, () -> resolver.resolve(manifest, chapterId));

        assertEquals("This audio source requires a licensed or bring-your-own provider configuration.", exception.getMessage());
    }
}
