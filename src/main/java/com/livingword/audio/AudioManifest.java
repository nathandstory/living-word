package com.livingword.audio;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public record AudioManifest(
    String id,
    String translationId,
    URI baseUri,
    String fileExtension,
    String pathStrategy,
    Map<AudioChapterId, String> chapterHashes,
    Map<AudioChapterId, String> chapterPaths
) {
    public AudioManifest(String id, String translationId, URI baseUri, Map<AudioChapterId, String> chapterHashes) {
        this(id, translationId, baseUri, "ogg", "direct", chapterHashes, Map.of());
    }

    public AudioManifest {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("audio manifest id is required");
        }
        if (translationId == null || translationId.isBlank()) {
            throw new IllegalArgumentException("translationId is required");
        }
        if (baseUri == null) {
            throw new IllegalArgumentException("baseUri is required");
        }
        if (fileExtension == null || fileExtension.isBlank()) {
            throw new IllegalArgumentException("fileExtension is required");
        }
        if (pathStrategy == null || pathStrategy.isBlank()) {
            throw new IllegalArgumentException("pathStrategy is required");
        }
        fileExtension = normalizeExtension(fileExtension);
        pathStrategy = pathStrategy.toLowerCase(Locale.ROOT);
        chapterHashes = Map.copyOf(chapterHashes == null ? Map.of() : chapterHashes);
        chapterPaths = Map.copyOf(chapterPaths == null ? Map.of() : chapterPaths);
    }

    public URI chapterUri(AudioChapterId chapterId) {
        String explicitPath = chapterPaths.get(chapterId);
        if (explicitPath != null && !explicitPath.isBlank()) {
            return resolvePath(explicitPath);
        }
        return baseUri.resolve("%s/%s".formatted(chapterId.bookId(), chapterId.fileName(fileExtension)));
    }

    public Optional<String> expectedHash(AudioChapterId chapterId) {
        return Optional.ofNullable(chapterHashes.get(chapterId));
    }

    private URI resolvePath(String path) {
        if (isAbsoluteUrl(path)) {
            return URI.create(encodeAbsoluteUrl(path));
        }
        URI uri = URI.create(encodePath(path));
        if (uri.isAbsolute()) {
            return uri;
        }
        return baseUri.resolve(uri);
    }

    private static String normalizeExtension(String extension) {
        String normalized = extension.startsWith(".") ? extension.substring(1) : extension;
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9]+")) {
            throw new IllegalArgumentException("fileExtension must be alphanumeric");
        }
        return normalized;
    }

    private static String encodePath(String path) {
        if (path.contains("%")) {
            return path;
        }
        return Arrays.stream(path.split("/", -1))
            .map(segment -> URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"))
            .collect(Collectors.joining("/"));
    }

    private static boolean isAbsoluteUrl(String path) {
        return path.matches("^[A-Za-z][A-Za-z0-9+.-]*://.*");
    }

    private static String encodeAbsoluteUrl(String url) {
        if (url.contains("%")) {
            return url;
        }
        int schemeEnd = url.indexOf("://") + 3;
        String schemeAndSlashes = url.substring(0, schemeEnd);
        String remainder = url.substring(schemeEnd);
        int pathStart = remainder.indexOf('/');
        if (pathStart < 0) {
            return url;
        }
        String authority = remainder.substring(0, pathStart);
        String path = remainder.substring(pathStart + 1);
        return schemeAndSlashes + authority + "/" + encodePath(path);
    }
}
