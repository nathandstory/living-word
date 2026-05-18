package com.livingword.daily;

import com.livingword.bible.BibleReference;

public record DailyVerse(BibleReference reference, String text) {
    public DailyVerse {
        if (reference == null) {
            throw new IllegalArgumentException("reference is required");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text is required");
        }
    }
}
