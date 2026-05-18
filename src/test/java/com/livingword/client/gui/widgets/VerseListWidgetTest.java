package com.livingword.client.gui.widgets;

import com.livingword.bible.ChapterData;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VerseListWidgetTest {
    @Test
    void hitTestingAccountsForScrollOffset() {
        VerseListWidget widget = new VerseListWidget();
        ChapterData chapter = new ChapterData("kjv", "psalms", 119, Map.of(
            1, "Blessed are the undefiled in the way.",
            2, "Blessed are they that keep his testimonies.",
            3, "They also do no iniquity."
        ));

        assertEquals(1, widget.verseAt(chapter, 100, 100, 0).orElseThrow());
        assertEquals(2, widget.verseAt(chapter, 100, 100, 14).orElseThrow());
        assertEquals(3, widget.verseAt(chapter, 100, 114, 14).orElseThrow());
        assertTrue(widget.verseAt(chapter, 100, 200, 14).isEmpty());
    }
}
