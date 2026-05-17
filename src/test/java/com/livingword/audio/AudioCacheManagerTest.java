package com.livingword.audio;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AudioCacheManagerTest {
    @Test
    void buildsStableChapterCachePath() {
        AudioCacheManager cache = new AudioCacheManager(Path.of(".minecraft", "livingword", "cache", "audio"));
        Path path = cache.chapterAudioPath(new AudioChapterId("kjv", "john", 3));
        assertEquals(Path.of(".minecraft", "livingword", "cache", "audio", "kjv", "john", "john_003.ogg"), path);
    }
}
