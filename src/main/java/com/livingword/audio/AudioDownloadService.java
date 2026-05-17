package com.livingword.audio;

import java.util.concurrent.CompletableFuture;

public interface AudioDownloadService {
    CompletableFuture<DownloadState> requestChapter(AudioManifest manifest, AudioChapterId chapterId);

    static AudioDownloadService unavailable() {
        return (manifest, chapterId) -> CompletableFuture.completedFuture(DownloadState.notImplemented(chapterId));
    }
}
