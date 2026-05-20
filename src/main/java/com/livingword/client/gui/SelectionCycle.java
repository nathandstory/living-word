package com.livingword.client.gui;

import java.util.List;
import java.util.Optional;

final class SelectionCycle {
    private SelectionCycle() {
    }

    static <T> Optional<T> next(List<T> values, T currentValue, int direction) {
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        int index = values.indexOf(currentValue);
        if (index < 0) {
            return Optional.of(values.getFirst());
        }
        return Optional.of(values.get(Math.floorMod(index + direction, values.size())));
    }
}
