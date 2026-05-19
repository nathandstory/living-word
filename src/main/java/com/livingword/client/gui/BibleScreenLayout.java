package com.livingword.client.gui;

import java.util.List;

public record BibleScreenLayout(
    Rect panel,
    Rect searchBox,
    Rect searchGo,
    Rect searchNext,
    Rect close,
    Rect previousBook,
    Rect previousChapter,
    Rect nextChapter,
    Rect nextBook,
    Rect bookmark,
    Rect version,
    Rect listen,
    Rect copy,
    Rect verseList,
    int navButtonWidth,
    int bottomButtonWidth
) {
    public static BibleScreenLayout compute(int screenWidth, int screenHeight) {
        int panelWidth = Math.min(640, Math.max(260, screenWidth - 16));
        int panelHeight = Math.min(420, Math.max(220, screenHeight - 16));
        int left = (screenWidth - panelWidth) / 2;
        int top = (screenHeight - panelHeight) / 2;

        int searchWidth = Math.min(420, Math.max(140, panelWidth - 220));
        int searchTotalWidth = searchWidth + 6 + 42 + 6 + 50;
        int searchX = left + (panelWidth - searchTotalWidth) / 2;
        int searchY = top + 32;

        int navWidth = Math.min(88, Math.max(54, (panelWidth - 72) / 4));
        int navGap = 8;
        int navTotalWidth = navWidth * 4 + navGap * 3;
        int navX = left + (panelWidth - navTotalWidth) / 2;
        int navY = top + 60;

        int bottomWidth = Math.min(96, Math.max(64, (panelWidth - 72) / 4));
        int bottomGap = 8;
        int bottomTotalWidth = bottomWidth * 4 + bottomGap * 3;
        int bottomX = left + (panelWidth - bottomTotalWidth) / 2;
        int bottomY = top + panelHeight - 30;

        int verseY = top + 112;
        int verseHeight = Math.max(32, bottomY - verseY - 12);

        return new BibleScreenLayout(
            new Rect(left, top, panelWidth, panelHeight),
            new Rect(searchX, searchY, searchWidth, 20),
            new Rect(searchX + searchWidth + 6, searchY, 42, 20),
            new Rect(searchX + searchWidth + 54, searchY, 50, 20),
            new Rect(left + panelWidth - 62, top + 8, 42, 20),
            new Rect(navX, navY, navWidth, 20),
            new Rect(navX + navWidth + navGap, navY, navWidth, 20),
            new Rect(navX + (navWidth + navGap) * 2, navY, navWidth, 20),
            new Rect(navX + (navWidth + navGap) * 3, navY, navWidth, 20),
            new Rect(bottomX, bottomY, bottomWidth, 20),
            new Rect(bottomX + bottomWidth + bottomGap, bottomY, bottomWidth, 20),
            new Rect(bottomX + (bottomWidth + bottomGap) * 2, bottomY, bottomWidth, 20),
            new Rect(bottomX + (bottomWidth + bottomGap) * 3, bottomY, bottomWidth, 20),
            new Rect(left + 28, verseY, panelWidth - 56, verseHeight),
            navWidth,
            bottomWidth
        );
    }

    public List<Rect> controlRects() {
        return List.of(
            searchBox,
            searchGo,
            searchNext,
            close,
            previousBook,
            previousChapter,
            nextChapter,
            nextBook,
            bookmark,
            version,
            listen,
            copy
        );
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
