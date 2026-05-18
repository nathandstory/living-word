package com.livingword.audio;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class AudioManifestRepository {
    private final ClassLoader classLoader;
    private final AudioManifestParser parser = new AudioManifestParser();

    public AudioManifestRepository(ClassLoader classLoader) {
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
    }

    public Optional<AudioManifest> find(String translationId, String manifestId) {
        String path = "data/livingword/audio/%s/%s.json".formatted(translationId, manifestId);
        InputStream stream = classLoader.getResourceAsStream(path);
        if (stream == null) {
            return Optional.empty();
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return Optional.of(parser.parse(reader));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Failed to read audio manifest " + path, exception);
        }
    }

    public AudioManifest manifestOrFallback(String translationId, String manifestId, URI fallbackBaseUri) {
        return find(translationId, manifestId)
            .orElseGet(() -> new AudioManifest(translationId + "-" + manifestId, translationId, fallbackBaseUri, Map.of()));
    }
}
