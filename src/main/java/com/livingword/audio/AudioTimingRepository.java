package com.livingword.audio;

import com.livingword.LivingWord;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class AudioTimingRepository {
    private final ClassLoader classLoader;
    private final VerseTimestampParser parser;
    private final Optional<Path> filesystemRoot;
    private final Map<TimingKey, Optional<VerseTimestampMap>> cache = new ConcurrentHashMap<>();

    public AudioTimingRepository(ClassLoader classLoader) {
        this(classLoader, Optional.empty(), new VerseTimestampParser());
    }

    public AudioTimingRepository(ClassLoader classLoader, Path filesystemRoot) {
        this(classLoader, Optional.ofNullable(filesystemRoot), new VerseTimestampParser());
    }

    AudioTimingRepository(ClassLoader classLoader, VerseTimestampParser parser) {
        this(classLoader, Optional.empty(), parser);
    }

    private AudioTimingRepository(ClassLoader classLoader, Optional<Path> filesystemRoot, VerseTimestampParser parser) {
        this.classLoader = classLoader == null ? AudioTimingRepository.class.getClassLoader() : classLoader;
        this.parser = parser;
        this.filesystemRoot = filesystemRoot;
    }

    public Optional<VerseTimestampMap> timestamps(AudioChapterId chapterId, String audioManifestId) {
        String manifestId = audioManifestId == null || audioManifestId.isBlank() ? "default" : audioManifestId;
        return cache.computeIfAbsent(new TimingKey(chapterId, manifestId), key -> load(key.chapterId(), key.audioManifestId()));
    }

    private Optional<VerseTimestampMap> load(AudioChapterId chapterId, String audioManifestId) {
        Optional<VerseTimestampMap> filesystemTimings = loadFilesystem(chapterId, audioManifestId);
        if (filesystemTimings.isPresent()) {
            return filesystemTimings;
        }
        return loadResource(chapterId, audioManifestId);
    }

    private Optional<VerseTimestampMap> loadFilesystem(AudioChapterId chapterId, String audioManifestId) {
        if (filesystemRoot.isEmpty()) {
            return Optional.empty();
        }
        Path path = filesystemRoot.orElseThrow()
            .resolve(chapterId.translationId())
            .resolve(audioManifestId)
            .resolve(chapterId.bookId())
            .resolve(chapterId.timestampsFileName());
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return Optional.of(parser.parse(reader));
        } catch (RuntimeException | java.io.IOException exception) {
            LivingWord.LOGGER.warn("Unable to load Living Word audio timing sidecar {}", path, exception);
            return Optional.empty();
        }
    }

    private Optional<VerseTimestampMap> loadResource(AudioChapterId chapterId, String audioManifestId) {
        String resourcePath = "data/livingword/audio/%s/%s/%s/%s".formatted(
            chapterId.translationId(),
            audioManifestId,
            chapterId.bookId(),
            chapterId.timestampsFileName()
        );
        try (InputStream inputStream = classLoader.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                return Optional.empty();
            }
            try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                return Optional.of(parser.parse(reader));
            }
        } catch (RuntimeException | java.io.IOException exception) {
            LivingWord.LOGGER.warn("Unable to load Living Word audio timing sidecar {}", resourcePath, exception);
            return Optional.empty();
        }
    }

    private record TimingKey(AudioChapterId chapterId, String audioManifestId) {
    }
}
