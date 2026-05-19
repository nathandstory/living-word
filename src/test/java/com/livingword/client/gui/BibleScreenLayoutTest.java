package com.livingword.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BibleScreenLayoutTest {
    @Test
    void wideScreensKeepControlsCompactInsteadOfStretchingAcrossPage() {
        BibleScreenLayout layout = BibleScreenLayout.compute(1420, 905);

        assertTrue(layout.navButtonWidth() <= 88);
        assertTrue(layout.bottomButtonWidth() <= 96);
        assertTrue(layout.searchBox().width() <= 420);
    }

    @Test
    void verseAreaDoesNotOverlapControls() {
        BibleScreenLayout layout = BibleScreenLayout.compute(854, 480);

        for (BibleScreenLayout.Rect rect : layout.controlRects()) {
            assertFalse(layout.verseList().intersects(rect));
        }
        assertTrue(layout.verseList().height() >= 160);
    }
}
