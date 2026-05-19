package com.livingword.audio;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultAudioChapterUriResolver implements AudioChapterUriResolver {
    private final RemoteResourceReader resourceReader;
    private final Map<URI, String> directoryCache = new ConcurrentHashMap<>();

    public DefaultAudioChapterUriResolver() {
        this(uri -> uri.toURL().openStream());
    }

    DefaultAudioChapterUriResolver(RemoteResourceReader resourceReader) {
        this.resourceReader = Objects.requireNonNull(resourceReader, "resourceReader");
    }

    @Override
    public URI resolve(AudioManifest manifest, AudioChapterId chapterId) throws IOException {
        if ("ebible-web-directory".equals(manifest.pathStrategy())) {
            URI directoryUri = EbibleWebAudioIndex.bookDirectoryUri(manifest.baseUri(), chapterId);
            String directoryHtml = directoryCache.computeIfAbsent(directoryUri, this::readUnchecked);
            return EbibleWebAudioIndex.resolveChapterUri(manifest.baseUri(), chapterId, directoryHtml);
        }
        return manifest.chapterUri(chapterId);
    }

    private String readUnchecked(URI uri) {
        try (InputStream inputStream = resourceReader.open(uri)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read audio directory index " + uri, exception);
        }
    }

    @FunctionalInterface
    interface RemoteResourceReader {
        InputStream open(URI uri) throws IOException;
    }
}
