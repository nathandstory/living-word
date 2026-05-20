package com.livingword.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BibleScreenLayoutTest {
    @Test
    void wideScreensKeepControlsCompactInsteadOfStretchingAcrossPage() {
        BibleScreenLayout layout = BibleScreenLayout.compute(1420, 905, true, true);

        assertTrue(layout.navButtonWidth() <= 88);
        assertTrue(layout.toolButtonWidth() <= 72);
        assertTrue(layout.searchBox().width() <= 420);
    }

    @Test
    void verseAreaDoesNotOverlapControls() {
        BibleScreenLayout layout = BibleScreenLayout.compute(854, 480, true, true);

        for (BibleScreenLayout.Rect rect : layout.controlRects()) {
            assertFalse(layout.verseList().intersects(rect));
        }
        assertTrue(layout.verseList().height() >= 160);
    }

    @Test
    void collapsedControlsLeaveMoreRoomForReading() {
        BibleScreenLayout layout = BibleScreenLayout.compute(854, 480, false, false);

        assertTrue(layout.verseList().width() >= layout.panel().width() - 80);
        assertTrue(layout.verseList().y() < BibleScreenLayout.compute(854, 480, true, true).verseList().y());
        assertTrue(layout.searchToggle().x() < layout.toolsToggle().x());
    }

    @Test
    void compactScreensDoNotOverlapControls() {
        BibleScreenLayout layout = BibleScreenLayout.compute(320, 240, true, true);
        java.util.List<BibleScreenLayout.Rect> controls = layout.controlRects();

        for (int left = 0; left < controls.size(); left++) {
            for (int right = left + 1; right < controls.size(); right++) {
                assertFalse(controls.get(left).intersects(controls.get(right)));
            }
        }
    }
}
