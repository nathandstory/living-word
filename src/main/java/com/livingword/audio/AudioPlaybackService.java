package com.livingword.audio;

import com.livingword.sync.AudioSourcePosition;

import java.util.List;
import java.util.Optional;

public interface AudioPlaybackService {
    void play(AudioChapterId chapterId, long positionMillis, boolean spatial);

    default void play(AudioChapterId chapterId, long positionMillis, boolean spatial, String fileExtension) {
        play(chapterId, positionMillis, spatial);
    }

    default void play(AudioChapterId chapterId, long positionMillis, boolean spatial, String fileExtension, Optional<AudioSourcePosition> sourcePosition) {
        play(chapterId, positionMillis, spatial, fileExtension);
    }

    void pause(AudioChapterId chapterId);

    void seek(AudioChapterId chapterId, long positionMillis);

    default void seek(AudioChapterId chapterId, long positionMillis, String fileExtension) {
        seek(chapterId, positionMillis);
    }

    default void seek(AudioChapterId chapterId, long positionMillis, String fileExtension, Optional<AudioSourcePosition> sourcePosition) {
        seek(chapterId, positionMillis, fileExtension);
    }

    void stop(AudioChapterId chapterId);

    default void stopAll() {
    }

    default List<AudioChapterId> drainCompletedChapters() {
        return List.of();
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
