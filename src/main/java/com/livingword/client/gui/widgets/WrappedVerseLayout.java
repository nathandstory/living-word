package com.livingword.client.gui.widgets;

import com.livingword.bible.ChapterData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class WrappedVerseLayout {
    private WrappedVerseLayout() {
    }

    public static List<VisualLine> wrap(ChapterData chapter, int maxWidth, TextWidth textWidth) {
        List<VisualLine> lines = new ArrayList<>();
        for (Map.Entry<Integer, String> verse : sortedVerses(chapter)) {
            appendWrappedVerse(lines, verse.getKey(), verse.getValue(), maxWidth, textWidth);
        }
        return List.copyOf(lines);
    }

    private static void appendWrappedVerse(List<VisualLine> lines, int verseNumber, String verseText, int maxWidth, TextWidth textWidth) {
        String firstPrefix = verseNumber + ". ";
        String wrappedPrefix = "   ";
        String prefix = firstPrefix;
        String current = prefix;
        for (String word : verseText.split("\\s+")) {
            if (word.isBlank()) {
                continue;
            }
            String candidate = current.equals(prefix) ? current + word : current + " " + word;
            if (!current.equals(prefix) && textWidth.width(candidate) > maxWidth) {
                lines.add(new VisualLine(verseNumber, current));
                prefix = wrappedPrefix;
                current = prefix + word;
                continue;
            }
            current = candidate;
        }
        if (!current.equals(prefix)) {
            lines.add(new VisualLine(verseNumber, current));
        }
    }

    private static List<Map.Entry<Integer, String>> sortedVerses(ChapterData chapter) {
        return chapter.verses().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
    }

    @FunctionalInterface
    public interface TextWidth {
        int width(String text);
    }

    public record VisualLine(int verseNumber, String text) {
    }
}
