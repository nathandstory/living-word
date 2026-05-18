package com.livingword.lectern;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class LecternListeningStationRegistry {
    private final Map<Key, LecternListeningStation> stations = new ConcurrentHashMap<>();

    public void remember(ResourceLocation dimension, LecternListeningStation station) {
        stations.put(new Key(dimension, station.sourcePos()), station);
    }

    public Optional<LecternListeningStation> get(ResourceLocation dimension, BlockPos sourcePos) {
        return Optional.ofNullable(stations.get(new Key(dimension, sourcePos)));
    }

    private record Key(ResourceLocation dimension, BlockPos sourcePos) {
    }
}
