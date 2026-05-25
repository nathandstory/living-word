package com.livingword.audio;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.List;
import java.util.LinkedHashMap;

public record VerseTimestampMap(Map<Integer, Long> verseStartMillis, Map<Integer, List<TimedWord>> verseWords) {
    public VerseTimestampMap {
        verseStartMillis = Map.copyOf(verseStartMillis == null ? Map.of() : verseStartMillis);
        Map<Integer, List<TimedWord>> copiedWords = new LinkedHashMap<>();
        if (verseWords != null) {
            for (Map.Entry<Integer, List<TimedWord>> entry : verseWords.entrySet()) {
                copiedWords.put(entry.getKey(), List.copyOf(entry.getValue() == null ? List.of() : entry.getValue()));
            }
        }
        verseWords = Map.copyOf(copiedWords);
    }

    public VerseTimestampMap(Map<Integer, Long> verseStartMillis) {
        this(verseStartMillis, Map.of());
    }

    public Optional<Long> startMillis(int verse) {
        return Optional.ofNullable(verseStartMillis.get(verse));
    }

    public Optional<Long> nextStartMillis(int verse) {
        return new TreeMap<>(verseStartMillis).entrySet().stream()
            .filter(entry -> entry.getKey() > verse)
            .map(Map.Entry::getValue)
            .findFirst();
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

    public Optional<TimedWord> wordAt(int verse, long positionMillis) {
        return wordIndexAt(verse, positionMillis).map(index -> verseWords.get(verse).get(index));
    }

    public Optional<Integer> wordIndexAt(int verse, long positionMillis) {
        List<TimedWord> words = verseWords.getOrDefault(verse, List.of());
        if (words.isEmpty()) {
            return Optional.empty();
        }
        Integer activeIndex = null;
        for (int i = 0; i < words.size(); i++) {
            TimedWord word = words.get(i);
            if (word.startMillis() > positionMillis) {
                break;
            }
            activeIndex = i;
            long endMillis = word.endMillis() > word.startMillis()
                ? word.endMillis()
                : i + 1 < words.size() ? words.get(i + 1).startMillis() : Long.MAX_VALUE;
            if (positionMillis < endMillis) {
                return Optional.of(i);
            }
        }
        return Optional.ofNullable(activeIndex);
    }

    public record TimedWord(String text, long startMillis, long endMillis) {
        public TimedWord {
            text = text == null ? "" : text;
            startMillis = Math.max(0L, startMillis);
            endMillis = Math.max(startMillis, endMillis);
        }
    }
}
