package com.livingword.discs;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JukeboxListeningSessionRegistryTest {
    @Test
    void remembersAndRemovesSessionByDimensionAndPosition() {
        JukeboxListeningSessionRegistry registry = new JukeboxListeningSessionRegistry();
        ResourceLocation dimension = ResourceLocation.withDefaultNamespace("overworld");
        BlockPos pos = new BlockPos(1, 64, 2);
        UUID sessionId = UUID.randomUUID();

        registry.remember(dimension, pos, ScriptureDiscSelection.defaults(), sessionId, 0L);
        Optional<UUID> removed = registry.remove(dimension, pos);

        assertEquals(Optional.of(sessionId), removed);
        assertTrue(registry.remove(dimension, pos).isEmpty());
    }

    @Test
    void pausedSessionKeepsResumePositionForSameSelection() {
        JukeboxListeningSessionRegistry registry = new JukeboxListeningSessionRegistry();
        ResourceLocation dimension = ResourceLocation.withDefaultNamespace("overworld");
        BlockPos pos = new BlockPos(1, 64, 2);
        ScriptureDiscSelection selection = new ScriptureDiscSelection("kjv", "john", 3);
        UUID sessionId = UUID.randomUUID();

        registry.remember(dimension, pos, selection, sessionId, 0L);
        registry.pause(dimension, pos, 42_000L);

        assertEquals(42_000L, registry.resumePosition(dimension, pos, selection));
        assertEquals(0L, registry.resumePosition(dimension, pos, new ScriptureDiscSelection("kjv", "romans", 8)));
    }
}
