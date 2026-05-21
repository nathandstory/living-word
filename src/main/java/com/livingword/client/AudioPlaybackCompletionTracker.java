package com.livingword.client;

import com.livingword.audio.AudioChapterId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

final class AudioPlaybackCompletionTracker {
    private static final long MIN_COMPLETION_AGE_MILLIS = 1_000L;
    private static final long UNOBSERVED_STALE_AGE_MILLIS = 30_000L;

    private final Map<AudioChapterId, State> tracked = new ConcurrentHashMap<>();

    void track(AudioChapterId chapterId, long nowMillis) {
        tracked.put(chapterId, new State(Math.max(0L, nowMillis), false));
    }

    void forget(AudioChapterId chapterId) {
        tracked.remove(chapterId);
    }

    void clear() {
        tracked.clear();
    }

    List<AudioChapterId> drainCompleted(long nowMillis, Predicate<AudioChapterId> isActive) {
        List<AudioChapterId> completed = new ArrayList<>();
        long now = Math.max(0L, nowMillis);
        for (Map.Entry<AudioChapterId, State> entry : tracked.entrySet()) {
            AudioChapterId chapterId = entry.getKey();
            State state = entry.getValue();
            long ageMillis = now - state.startedAtMillis();
            if (ageMillis < MIN_COMPLETION_AGE_MILLIS) {
                continue;
            }
            if (isActive.test(chapterId)) {
                tracked.put(chapterId, new State(state.startedAtMillis(), true));
                continue;
            }
            if (state.observedActive()) {
                if (tracked.remove(chapterId, state)) {
                    completed.add(chapterId);
                }
                continue;
            }
            if (ageMillis >= UNOBSERVED_STALE_AGE_MILLIS) {
                tracked.remove(chapterId, state);
            }
        }
        return List.copyOf(completed);
    }

    private record State(long startedAtMillis, boolean observedActive) {
    }
}
