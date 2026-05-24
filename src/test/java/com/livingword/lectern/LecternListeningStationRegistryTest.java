package com.livingword.lectern;

import com.livingword.bible.BibleReference;
import com.livingword.discs.ScriptureDiscPlaybackMode;
import com.livingword.discs.ScriptureDiscSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LecternListeningStationRegistryTest {
    @Test
    void remembersStationByDimensionAndPosition() {
        LecternListeningStationRegistry registry = new LecternListeningStationRegistry();
        ResourceLocation dimension = ResourceLocation.withDefaultNamespace("overworld");
        LecternListeningStation station = new LecternListeningStation(
            new BlockPos(1, 64, 2),
            new BibleReference("kjv", "john", 3, 1)
        );

        registry.remember(dimension, station);

        assertEquals(station, registry.get(dimension, station.sourcePos()).orElseThrow());
    }

    @Test
    void tracksPlayingLecternSessionsByDimensionAndPosition() {
        LecternListeningStationRegistry registry = new LecternListeningStationRegistry();
        ResourceLocation dimension = ResourceLocation.withDefaultNamespace("overworld");
        BlockPos pos = new BlockPos(4, 70, -2);
        ScriptureDiscSelection selection = new ScriptureDiscSelection("bsb", "john", 3, "default", ScriptureDiscPlaybackMode.CONTINUE_BOOK);
        UUID sessionId = UUID.randomUUID();

        registry.rememberSession(dimension, pos, selection, sessionId, 1200L);

        assertEquals(sessionId, registry.getPlayingSession(dimension, pos).orElseThrow());
        assertEquals(1200L, registry.resumePosition(dimension, pos, selection));
        assertEquals(selection, registry.findPlaying(sessionId).orElseThrow().selection());
        assertEquals(sessionId, registry.playingSessions().getFirst().sessionId());

        registry.pause(dimension, pos, 4300L);

        assertTrue(registry.getPlayingSession(dimension, pos).isEmpty());
        assertTrue(registry.playingSessions().isEmpty());
        assertEquals(4300L, registry.resumePosition(dimension, pos, selection));
    }
}
