package com.livingword.client.gui;

import java.util.ArrayList;
import java.util.List;

public record BibleScreenLayout(
    Rect panel,
    Rect searchToggle,
    Rect toolsToggle,
    Rect highlightedToggle,
    Rect searchBox,
    Rect searchGo,
    Rect searchNext,
    Rect close,
    Rect previousBook,
    Rect previousChapter,
    Rect nextChapter,
    Rect nextBook,
    Rect version,
    Rect listen,
    Rect highlight,
    Rect copy,
    Rect verseList,
    int headingBookY,
    int headingTranslationY,
    int statusY,
    int navButtonWidth,
    int toolButtonWidth
) {
    public static BibleScreenLayout compute(int screenWidth, int screenHeight) {
        return compute(screenWidth, screenHeight, false, false);
    }

    public static BibleScreenLayout compute(int screenWidth, int screenHeight, boolean searchExpanded, boolean toolsExpanded) {
        int panelWidth = Math.min(640, Math.max(260, screenWidth - 16));
        int panelHeight = Math.min(420, Math.max(220, screenHeight - 16));
        int left = (screenWidth - panelWidth) / 2;
        int top = (screenHeight - panelHeight) / 2;

        int chromeY = top + 8;
        int toggleWidth = 56;
        Rect searchToggle = new Rect(left + 20, chromeY, toggleWidth, 20);
        Rect toolsToggle = new Rect(searchToggle.right() + 6, chromeY, 48, 20);
        Rect highlightedToggle = new Rect(toolsToggle.right() + 6, chromeY, 78, 20);
        Rect close = new Rect(left + panelWidth - 62, chromeY, 42, 20);

        int rowY = top + 34;
        int rowGap = 6;
        int searchGap = 5;
        int searchGoWidth = 32;
        int searchNextWidth = 42;
        int searchAvailable = Math.max(210, panelWidth - 32);
        int searchWidth = Math.min(420, Math.max(120, searchAvailable - searchGoWidth - searchNextWidth - searchGap * 2));
        int searchTotalWidth = searchWidth + searchGoWidth + searchNextWidth + searchGap * 2;
        int searchX = left + (panelWidth - searchTotalWidth) / 2;
        Rect searchBox = new Rect(searchX, rowY, searchWidth, 20);
        Rect searchGo = new Rect(searchBox.right() + searchGap, rowY, searchGoWidth, 20);
        Rect searchNext = new Rect(searchGo.right() + searchGap, rowY, searchNextWidth, 20);
        if (searchExpanded) {
            rowY += 20 + rowGap;
        }

        int toolbarGap = 4;
        int toolbarAvailable = Math.max(220, panelWidth - 32);
        int navButtonWidth = Math.min(38, Math.max(20, toolbarAvailable / 14));
        int toolButtonWidth = Math.min(72, Math.max(32, (toolbarAvailable - navButtonWidth * 4 - toolbarGap * 7) / 4));
        int toolbarTotalWidth = toolButtonWidth * 4 + navButtonWidth * 4 + toolbarGap * 7;
        int toolbarX = left + (panelWidth - toolbarTotalWidth) / 2;
        Rect version = new Rect(toolbarX, rowY, toolButtonWidth, 20);
        Rect previousBook = new Rect(version.right() + toolbarGap, rowY, navButtonWidth, 20);
        Rect previousChapter = new Rect(previousBook.right() + toolbarGap, rowY, navButtonWidth, 20);
        Rect nextChapter = new Rect(previousChapter.right() + toolbarGap, rowY, navButtonWidth, 20);
        Rect nextBook = new Rect(nextChapter.right() + toolbarGap, rowY, navButtonWidth, 20);
        Rect listen = new Rect(nextBook.right() + toolbarGap, rowY, toolButtonWidth, 20);
        Rect highlight = new Rect(listen.right() + toolbarGap, rowY, toolButtonWidth, 20);
        Rect copy = new Rect(highlight.right() + toolbarGap, rowY, toolButtonWidth, 20);
        if (toolsExpanded) {
            rowY += 20 + rowGap;
        }

        int headingBookY = rowY + 3;
        int headingTranslationY = headingBookY + 12;
        int statusY = headingTranslationY + 12;
        int verseY = statusY + 14;
        int verseX = left + 28;
        int verseWidth = Math.max(120, panelWidth - 56);
        int verseHeight = Math.max(32, top + panelHeight - verseY - 24);

        return new BibleScreenLayout(
            new Rect(left, top, panelWidth, panelHeight),
            searchToggle,
            toolsToggle,
            highlightedToggle,
            searchBox,
            searchGo,
            searchNext,
            close,
            previousBook,
            previousChapter,
            nextChapter,
            nextBook,
            version,
            listen,
            highlight,
            copy,
            new Rect(verseX, verseY, verseWidth, verseHeight),
            headingBookY,
            headingTranslationY,
            statusY,
            navButtonWidth,
            toolButtonWidth
        );
    }

    public List<Rect> controlRects() {
        List<Rect> rects = new ArrayList<>();
        rects.add(searchToggle);
        rects.add(toolsToggle);
        rects.add(highlightedToggle);
        rects.add(searchBox);
        rects.add(searchGo);
        rects.add(searchNext);
        rects.add(close);
        rects.add(previousBook);
        rects.add(previousChapter);
        rects.add(nextChapter);
        rects.add(nextBook);
        rects.add(version);
        rects.add(listen);
        rects.add(highlight);
        rects.add(copy);
        return rects;
    }

    public record Rect(int x, int y, int width, int height) {
        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        public boolean intersects(Rect other) {
            return x < other.right() && right() > other.x && y < other.bottom() && bottom() > other.y;
        }
    }
}
