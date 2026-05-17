package com.livingword.bible;

import java.util.Locale;

public record BookId(String value) {
    public BookId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("book id is required");
        }
        value = value.toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
