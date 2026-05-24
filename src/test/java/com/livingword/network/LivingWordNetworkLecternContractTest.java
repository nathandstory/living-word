package com.livingword.network;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class LivingWordNetworkLecternContractTest {
    @Test
    void networkRegistersLecternStationPayloadsAndRoutesCompletion() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/network/LivingWordNetwork.java"));

        assertTrue(source.contains("OpenLecternStationPayload"));
        assertTrue(source.contains("ConfigureLecternStationPayload"));
        assertTrue(source.contains("LecternEvents.configureStation"));
        assertTrue(source.contains("LecternEvents.completeLecternChapter"));
        assertTrue(source.contains("currentListeningSession"));
    }
}
