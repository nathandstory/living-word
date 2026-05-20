package com.livingword.audio;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class AudioCacheManager {
    private final Path root;

    public AudioCacheManager(Path root) {
        this.root = root;
    }

    public Path chapterAudioPath(AudioChapterId chapterId) {
        return root.resolve(chapterId.translationId()).resolve(chapterId.bookId()).resolve(chapterId.fileName());
    }

    public Path chapterAudioPath(AudioChapterId chapterId, String extension) {
        return root.resolve(chapterId.translationId()).resolve(chapterId.bookId()).resolve(chapterId.fileName(extension));
    }

    public Path chapterTimestampsPath(AudioChapterId chapterId) {
        return root.resolve(chapterId.translationId()).resolve(chapterId.bookId()).resolve(chapterId.timestampsFileName());
    }

    public Path temporaryDownloadPath(AudioChapterId chapterId) {
        return chapterAudioPath(chapterId).resolveSibling(chapterId.fileName() + ".part");
    }

    public Path temporaryDownloadPath(AudioChapterId chapterId, String extension) {
        return chapterAudioPath(chapterId, extension).resolveSibling(chapterId.fileName(extension) + ".part");
    }

    public Path sourceMarkerPath(AudioChapterId chapterId, String extension) {
        return chapterAudioPath(chapterId, extension).resolveSibling(chapterId.fileName(extension) + ".source");
    }

    public boolean isCached(AudioChapterId chapterId) {
        return Files.isRegularFile(chapterAudioPath(chapterId));
    }

    public boolean isCached(AudioChapterId chapterId, String extension) {
        return Files.isRegularFile(chapterAudioPath(chapterId, extension));
    }

    public Optional<Path> cachedChapterAudioPath(AudioChapterId chapterId) {
        Path oggPath = chapterAudioPath(chapterId, "ogg");
        if (Files.isRegularFile(oggPath)) {
            return Optional.of(oggPath);
        }
        Path mp3Path = chapterAudioPath(chapterId, "mp3");
        if (Files.isRegularFile(mp3Path)) {
            return Optional.of(mp3Path);
        }
        return Optional.empty();
    }

    public Path markCorrupt(AudioChapterId chapterId) {
        return chapterAudioPath(chapterId).resolveSibling(chapterId.fileName() + ".corrupt");
    }
}
