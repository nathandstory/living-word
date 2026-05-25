package com.livingword.lectern;

import java.util.Locale;

public enum LecternStationAction {
    SAVE,
    START,
    STOP,
    PLAY,
    PAUSE,
    RESET,
    TOGGLE_DISPLAY;

    public static LecternStationAction fromId(String id) {
        if (id == null || id.isBlank()) {
            return SAVE;
        }
        try {
            return valueOf(id.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return SAVE;
        }
    }
}
