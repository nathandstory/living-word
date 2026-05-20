package com.livingword.audio;

public interface AudioPlaybackService {
    void play(AudioChapterId chapterId, long positionMillis, boolean spatial);

    default void play(AudioChapterId chapterId, long positionMillis, boolean spatial, String fileExtension) {
        play(chapterId, positionMillis, spatial);
    }

    void pause(AudioChapterId chapterId);

    void seek(AudioChapterId chapterId, long positionMillis);

    default void seek(AudioChapterId chapterId, long positionMillis, String fileExtension) {
        seek(chapterId, positionMillis);
    }

    void stop(AudioChapterId chapterId);

    default void stopAll() {
    }

    static AudioPlaybackService noop() {
        return new AudioPlaybackService() {
            @Override
            public void play(AudioChapterId chapterId, long positionMillis, boolean spatial) {
            }

            @Override
            public void pause(AudioChapterId chapterId) {
            }

            @Override
            public void seek(AudioChapterId chapterId, long positionMillis) {
            }

            @Override
            public void stop(AudioChapterId chapterId) {
            }
        };
    }
}
