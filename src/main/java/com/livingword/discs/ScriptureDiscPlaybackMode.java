package com.livingword.discs;

import java.util.Locale;

public enum ScriptureDiscPlaybackMode {
    SINGLE_CHAPTER("Single chapter"),
    CONTINUE_BOOK("Continue book"),
    LOOP_CHAPTER("Loop chapter");

    private final String displayName;

    ScriptureDiscPlaybackMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static ScriptureDiscPlaybackMode fromId(String id) {
        if (id == null || id.isBlank()) {
            return SINGLE_CHAPTER;
        }
        try {
            return ScriptureDiscPlaybackMode.valueOf(id.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return SINGLE_CHAPTER;
        }
    }
}
