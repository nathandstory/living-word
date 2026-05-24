package com.livingword.lectern;

import com.livingword.bible.ChapterData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        String prefix = referenceLabel + ":" + verse;
        if (verseText == null || verseText.isBlank()) {
            return List.of(prefix);
        }
        return wrapLines(prefix + " - " + verseText.strip());
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

    private static List<String> wrapLines(String text) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        int lineLength = 0;
        for (String word : text.split("\\s+")) {
            String remaining = word;
            while (remaining.length() > MAX_LINE_LENGTH) {
                if (lineLength > 0) {
                    lines.add(line.toString());
                    line.setLength(0);
                    lineLength = 0;
                }
                lines.add(remaining.substring(0, MAX_LINE_LENGTH));
                remaining = remaining.substring(MAX_LINE_LENGTH);
            }
            if (remaining.isEmpty()) {
                continue;
            }
            int separator = lineLength == 0 ? 0 : 1;
            if (lineLength > 0 && lineLength + separator + remaining.length() > MAX_LINE_LENGTH) {
                lines.add(line.toString());
                line.setLength(0);
                lineLength = 0;
                separator = 0;
            }
            if (separator == 1) {
                line.append(' ');
                lineLength++;
            }
            line.append(remaining);
            lineLength += remaining.length();
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines.isEmpty() ? List.of(text) : List.copyOf(lines);
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
}
