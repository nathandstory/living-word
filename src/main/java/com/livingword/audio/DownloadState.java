package com.livingword.audio;

public record DownloadState(AudioChapterId chapterId, Status status, double progress, String message) {
    public enum Status {
        CACHED,
        DOWNLOADING,
        UNAVAILABLE,
        HASH_MISMATCH,
        FAILED,
        NOT_IMPLEMENTED
    }

    public DownloadState {
        if (chapterId == null) {
            throw new IllegalArgumentException("chapterId is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        progress = Math.clamp(progress, 0.0D, 1.0D);
        message = message == null ? "" : message;
    }

    public static DownloadState cached(AudioChapterId chapterId) {
        return new DownloadState(chapterId, Status.CACHED, 1.0D, "");
    }

    public static DownloadState notImplemented(AudioChapterId chapterId) {
        return new DownloadState(chapterId, Status.NOT_IMPLEMENTED, 0.0D, "Audio downloading is not implemented in this build.");
    }
}
