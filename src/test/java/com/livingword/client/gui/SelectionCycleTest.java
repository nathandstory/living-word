package com.livingword.client.gui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SelectionCycleTest {
    @Test
    void cyclesForwardAndBackwardThroughAvailableValues() {
        List<String> values = List.of("genesis", "exodus", "leviticus");

        assertEquals("exodus", SelectionCycle.next(values, "genesis", 1).orElseThrow());
        assertEquals("leviticus", SelectionCycle.next(values, "genesis", -1).orElseThrow());
        assertEquals("genesis", SelectionCycle.next(values, "leviticus", 1).orElseThrow());
        assertEquals("exodus", SelectionCycle.next(values, "leviticus", -1).orElseThrow());
    }

    @Test
    void startsFromFirstValueWhenCurrentValueIsMissing() {
        assertEquals("john", SelectionCycle.next(List.of("john", "acts"), "missing", 1).orElseThrow());
    }
}
