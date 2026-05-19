package com.livingword.audio;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
