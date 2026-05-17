package com.livingword.audio;

import java.nio.file.Files;
import java.nio.file.Path;

public final class AudioCacheManager {
    private final Path root;

    public AudioCacheManager(Path root) {
        this.root = root;
    }

    public Path chapterAudioPath(AudioChapterId chapterId) {
        return root.resolve(chapterId.translationId()).resolve(chapterId.bookId()).resolve(chapterId.fileName());
    }

    public Path chapterTimestampsPath(AudioChapterId chapterId) {
        return root.resolve(chapterId.translationId()).resolve(chapterId.bookId()).resolve(chapterId.timestampsFileName());
    }

    public Path temporaryDownloadPath(AudioChapterId chapterId) {
        return chapterAudioPath(chapterId).resolveSibling(chapterId.fileName() + ".part");
    }

    public boolean isCached(AudioChapterId chapterId) {
        return Files.isRegularFile(chapterAudioPath(chapterId));
    }

    public Path markCorrupt(AudioChapterId chapterId) {
        return chapterAudioPath(chapterId).resolveSibling(chapterId.fileName() + ".corrupt");
    }
}
