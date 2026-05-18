package com.livingword.lectern;

import com.livingword.bible.BibleReference;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
