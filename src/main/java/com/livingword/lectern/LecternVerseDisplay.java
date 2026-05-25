package com.livingword.lectern;

import com.livingword.bible.ChapterData;
import com.livingword.audio.VerseTimestampMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public final class LecternVerseDisplay {
    private static final int MAX_LINE_LENGTH = 44;
    private static final long ESTIMATED_MILLIS_PER_WORD = 405L;
    private static final long LONG_VERSE_MILLIS_PER_WORD = 375L;
    private static final long MIN_ESTIMATED_VERSE_MILLIS = 1_100L;
    private static final long COMMA_PAUSE_MILLIS = 120L;
    private static final long SEMICOLON_PAUSE_MILLIS = 180L;
    private static final long SENTENCE_PAUSE_MILLIS = 320L;
    private static final long SOURCE_DEFAULT_DELAY_MILLIS = -450L;
    private static final Map<String, Long> SOURCE_OFFSETS_MILLIS = Map.of(
        sourceKey("bsb", "default"), SOURCE_DEFAULT_DELAY_MILLIS,
        sourceKey("bsb", "hays"), -350L,
        sourceKey("bsb", "souer"), -400L,
        sourceKey("kjv", "default"), -650L,
        sourceKey("webp", "default"), -500L
    );
    private static final long COLOR_CYCLE_MILLIS = 6_000L;
    private static final int[] DISPLAY_COLORS = {
        0xFFF4B8,
        0xFFD166,
        0xE7F6FF,
        0xFFFFFF
    };

    private LecternVerseDisplay() {
    }

    public static int verseAt(ChapterData chapter, long positionMillis) {
        if (chapter.verses().isEmpty()) {
            return 1;
        }
        return verseAtAdjustedPosition(chapter, Math.max(0L, positionMillis));
    }

    public static int verseAt(ChapterData chapter, long positionMillis, String translationId, String audioManifestId) {
        long adjustedPositionMillis = Math.max(0L, positionMillis + sourceOffsetMillis(translationId, audioManifestId));
        return verseAtAdjustedPosition(chapter, adjustedPositionMillis);
    }

    public static int verseAt(ChapterData chapter, long positionMillis, Optional<VerseTimestampMap> timestamps, String translationId, String audioManifestId) {
        if (timestamps.isPresent() && !timestamps.orElseThrow().verseStartMillis().isEmpty()) {
            return timestamps.orElseThrow().verseAt(Math.max(0L, positionMillis)).orElseGet(() -> verseAt(chapter, positionMillis));
        }
        return verseAt(chapter, positionMillis, translationId, audioManifestId);
    }

    public static int activeWordIndex(ChapterData chapter, int verse, long positionMillis, Optional<VerseTimestampMap> timestamps, String translationId, String audioManifestId) {
        if (!chapter.verses().containsKey(verse)) {
            return -1;
        }
        if (timestamps.isPresent()) {
            Optional<Integer> exactWord = timestamps.orElseThrow().wordIndexAt(verse, Math.max(0L, positionMillis));
            if (exactWord.isPresent()) {
                return exactWord.orElseThrow();
            }
        }
        long adjustedPositionMillis = timestamps.isPresent() && !timestamps.orElseThrow().verseStartMillis().isEmpty()
            ? Math.max(0L, positionMillis)
            : Math.max(0L, positionMillis + sourceOffsetMillis(translationId, audioManifestId));
        long verseStart = timestamps.flatMap(value -> value.startMillis(verse)).orElseGet(() -> estimatedVerseStartMillis(chapter, verse));
        long verseEnd = timestamps.flatMap(value -> value.nextStartMillis(verse)).orElseGet(() -> verseStart + estimatedDurationMillis(chapter.verseText(verse)));
        long verseDuration = Math.max(MIN_ESTIMATED_VERSE_MILLIS, verseEnd - verseStart);
        return estimatedWordIndex(chapter.verseText(verse), Math.max(0L, adjustedPositionMillis - verseStart), verseDuration);
    }

    public static boolean hasUsableVerseTiming(Optional<VerseTimestampMap> timestamps) {
        return timestamps.isPresent() && !timestamps.orElseThrow().verseStartMillis().isEmpty();
    }

    private static int verseAtAdjustedPosition(ChapterData chapter, long positionMillis) {
        long elapsed = 0L;
        int activeVerse = new TreeMap<>(chapter.verses()).firstKey();
        for (Map.Entry<Integer, String> verse : new TreeMap<>(chapter.verses()).entrySet()) {
            if (elapsed > positionMillis) {
                break;
            }
            activeVerse = verse.getKey();
            elapsed += estimatedDurationMillis(verse.getValue());
        }
        return activeVerse;
    }

    public static String text(String referenceLabel, int verse, String verseText) {
        return String.join("\n", lines(referenceLabel, verse, verseText));
    }

    public static List<String> lines(String referenceLabel, int verse, String verseText) {
        return displayLines(referenceLabel, verse, verseText, -1).stream()
            .map(DisplayLine::plainText)
            .toList();
    }

    public static List<DisplayLine> displayLines(String referenceLabel, int verse, String verseText, int activeWordIndex) {
        String prefix = referenceLabel + ":" + verse;
        if (verseText == null || verseText.isBlank()) {
            return List.of(new DisplayLine(List.of(new DisplayToken(prefix, false))));
        }
        return wrapDisplayLines(prefix, verseText.strip(), activeWordIndex);
    }

    public static int colorAt(long millis) {
        long normalized = Math.floorMod(millis, COLOR_CYCLE_MILLIS);
        double progress = normalized / (double) COLOR_CYCLE_MILLIS * DISPLAY_COLORS.length;
        int index = (int) Math.floor(progress);
        int nextIndex = (index + 1) % DISPLAY_COLORS.length;
        double localProgress = progress - index;
        return interpolate(DISPLAY_COLORS[index], DISPLAY_COLORS[nextIndex], localProgress);
    }

    private static long estimatedDurationMillis(String verseText) {
        if (verseText == null || verseText.isBlank()) {
            return MIN_ESTIMATED_VERSE_MILLIS;
        }
        String stripped = verseText.strip();
        int words = stripped.split("\\s+").length;
        long millisPerWord = words >= 28 ? LONG_VERSE_MILLIS_PER_WORD : ESTIMATED_MILLIS_PER_WORD;
        long punctuationPause = count(stripped, ',') * COMMA_PAUSE_MILLIS
            + count(stripped, ';') * SEMICOLON_PAUSE_MILLIS
            + count(stripped, ':') * SEMICOLON_PAUSE_MILLIS
            + sentencePauseMillis(stripped);
        return Math.max(MIN_ESTIMATED_VERSE_MILLIS, words * millisPerWord + punctuationPause);
    }

    private static long estimatedVerseStartMillis(ChapterData chapter, int verse) {
        long elapsed = 0L;
        for (Map.Entry<Integer, String> entry : new TreeMap<>(chapter.verses()).entrySet()) {
            if (entry.getKey() >= verse) {
                break;
            }
            elapsed += estimatedDurationMillis(entry.getValue());
        }
        return elapsed;
    }

    private static int estimatedWordIndex(String verseText, long elapsedMillis, long durationMillis) {
        if (verseText == null || verseText.isBlank()) {
            return -1;
        }
        String[] words = verseText.strip().split("\\s+");
        if (words.length == 0) {
            return -1;
        }
        double progress = Math.min(0.999D, Math.max(0.0D, elapsedMillis / (double) Math.max(1L, durationMillis)));
        return Math.max(0, Math.min(words.length - 1, (int) Math.floor(progress * words.length)));
    }

    private static List<DisplayLine> wrapDisplayLines(String prefix, String verseText, int activeWordIndex) {
        List<DisplayLine> lines = new ArrayList<>();
        List<DisplayToken> current = new ArrayList<>();
        int lineLength = 0;
        boolean lineHasVerseWord = false;

        current.add(new DisplayToken(prefix + " - ", false));
        lineLength = prefix.length() + 3;

        String[] words = verseText.split("\\s+");
        for (int wordIndex = 0; wordIndex < words.length; wordIndex++) {
            String word = words[wordIndex];
            boolean active = wordIndex == activeWordIndex;
            String remaining = word;
            while (remaining.length() > MAX_LINE_LENGTH) {
                if (lineLength > 0) {
                    lines.add(new DisplayLine(List.copyOf(current)));
                    current.clear();
                    lineLength = 0;
                    lineHasVerseWord = false;
                }
                lines.add(new DisplayLine(List.of(new DisplayToken(remaining.substring(0, MAX_LINE_LENGTH), active))));
                remaining = remaining.substring(MAX_LINE_LENGTH);
            }
            if (remaining.isEmpty()) {
                continue;
            }
            String separator = lineHasVerseWord ? " " : "";
            if (lineLength > 0 && lineHasVerseWord && lineLength + separator.length() + remaining.length() > MAX_LINE_LENGTH) {
                lines.add(new DisplayLine(List.copyOf(current)));
                current.clear();
                lineLength = 0;
                lineHasVerseWord = false;
                separator = "";
            }
            String tokenText = separator + remaining;
            if (lineLength > 0 && !lineHasVerseWord && lineLength + tokenText.length() > MAX_LINE_LENGTH) {
                lines.add(new DisplayLine(List.copyOf(current)));
                current.clear();
                lineLength = 0;
                tokenText = remaining;
            }
            current.add(new DisplayToken(tokenText, active));
            lineLength += tokenText.length();
            lineHasVerseWord = true;
        }
        if (!current.isEmpty()) {
            lines.add(new DisplayLine(List.copyOf(current)));
        }
        return lines.isEmpty() ? List.of(new DisplayLine(List.of(new DisplayToken(prefix, false)))) : List.copyOf(lines);
    }

    private static int interpolate(int start, int end, double progress) {
        int red = interpolateChannel((start >> 16) & 0xFF, (end >> 16) & 0xFF, progress);
        int green = interpolateChannel((start >> 8) & 0xFF, (end >> 8) & 0xFF, progress);
        int blue = interpolateChannel(start & 0xFF, end & 0xFF, progress);
        return (red << 16) | (green << 8) | blue;
    }

    private static int interpolateChannel(int start, int end, double progress) {
        return (int) Math.round(start + (end - start) * progress);
    }

    private static long sourceOffsetMillis(String translationId, String audioManifestId) {
        return SOURCE_OFFSETS_MILLIS.getOrDefault(sourceKey(translationId, audioManifestId), 0L);
    }

    private static String sourceKey(String translationId, String audioManifestId) {
        String normalizedTranslation = translationId == null || translationId.isBlank() ? "bsb" : translationId.toLowerCase(Locale.ROOT);
        String normalizedManifest = audioManifestId == null || audioManifestId.isBlank() ? "default" : audioManifestId.toLowerCase(Locale.ROOT);
        return normalizedTranslation + ":" + normalizedManifest;
    }

    private static long sentencePauseMillis(String text) {
        long pauses = 0L;
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == '.' || character == '?' || character == '!') {
                pauses += SENTENCE_PAUSE_MILLIS;
            }
        }
        return pauses;
    }

    private static int count(String text, char target) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == target) {
                count++;
            }
        }
        return count;
    }

    public record DisplayToken(String text, boolean active) {
        public DisplayToken {
            text = text == null ? "" : text;
        }
    }

    public record DisplayLine(List<DisplayToken> tokens) {
        public DisplayLine {
            tokens = List.copyOf(tokens == null ? List.of() : tokens);
        }

        public String plainText() {
            StringBuilder text = new StringBuilder();
            for (DisplayToken token : tokens) {
                text.append(token.text());
            }
            return text.toString();
        }
    }
}
