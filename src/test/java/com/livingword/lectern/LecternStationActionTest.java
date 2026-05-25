package com.livingword.lectern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class LecternStationActionTest {
    @Test
    void parsesResetActionFromNetworkId() {
        assertEquals(LecternStationAction.RESET, LecternStationAction.fromId("reset"));
    }
}
