package com.livingword.client;

import com.livingword.audio.AudioChapterId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AudioPlaybackCompletionTrackerTest {
    @Test
    void doesNotCompleteSoundThatWasNeverObservedActive() {
        AudioPlaybackCompletionTracker tracker = new AudioPlaybackCompletionTracker();
        AudioChapterId johnOne = new AudioChapterId("bsb", "john", 1);

        tracker.track(johnOne, 0L);

        assertEquals(List.of(), tracker.drainCompleted(2_000L, Set.of()::contains));
    }

    @Test
    void completesOnlyAfterSoundWasObservedActiveAndThenStoppedBeingActive() {
        AudioPlaybackCompletionTracker tracker = new AudioPlaybackCompletionTracker();
        AudioChapterId johnOne = new AudioChapterId("bsb", "john", 1);

        tracker.track(johnOne, 0L);
        assertEquals(List.of(), tracker.drainCompleted(1_100L, Set.of(johnOne)::contains));

        assertEquals(List.of(johnOne), tracker.drainCompleted(1_200L, Set.of()::contains));
        assertEquals(List.of(), tracker.drainCompleted(1_300L, Set.of()::contains));
    }

    @Test
    void forgetPreventsManualStopsFromBeingReportedAsCompletion() {
        AudioPlaybackCompletionTracker tracker = new AudioPlaybackCompletionTracker();
        AudioChapterId johnOne = new AudioChapterId("bsb", "john", 1);

        tracker.track(johnOne, 0L);
        tracker.drainCompleted(1_100L, Set.of(johnOne)::contains);
        tracker.forget(johnOne);

        assertEquals(List.of(), tracker.drainCompleted(1_200L, Set.of()::contains));
    }
}
