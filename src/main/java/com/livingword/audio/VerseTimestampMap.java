package com.livingword.audio;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public record VerseTimestampMap(Map<Integer, Long> verseStartMillis) {
    public VerseTimestampMap {
        verseStartMillis = Map.copyOf(verseStartMillis == null ? Map.of() : verseStartMillis);
    }

    public Optional<Long> startMillis(int verse) {
        return Optional.ofNullable(verseStartMillis.get(verse));
    }

    public Optional<Integer> verseAt(long positionMillis) {
        Integer activeVerse = null;
        for (Map.Entry<Integer, Long> entry : new TreeMap<>(verseStartMillis).entrySet()) {
            if (entry.getValue() > positionMillis) {
                break;
            }
            activeVerse = entry.getKey();
        }
        return Optional.ofNullable(activeVerse);
    }
}
