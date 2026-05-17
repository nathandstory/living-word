package com.livingword.audio;

public interface AudioPlaybackService {
    void play(AudioChapterId chapterId, long positionMillis, boolean spatial);

    void pause(AudioChapterId chapterId);

    void seek(AudioChapterId chapterId, long positionMillis);

    void stop(AudioChapterId chapterId);
}
