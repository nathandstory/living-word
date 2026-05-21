package com.livingword.discs;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class JukeboxListeningSessionRegistry {
    private final Map<Key, Entry> sessions = new ConcurrentHashMap<>();

    void remember(ResourceLocation dimension, BlockPos pos, ScriptureDiscSelection selection, UUID sessionId, long resumePositionMillis) {
        sessions.put(new Key(dimension, pos.immutable()), new Entry(selection, sessionId, Math.max(0L, resumePositionMillis), true));
    }

    Optional<UUID> get(ResourceLocation dimension, BlockPos pos) {
        Entry entry = sessions.get(new Key(dimension, pos.immutable()));
        return entry != null && entry.playing() ? Optional.of(entry.sessionId()) : Optional.empty();
    }

    Optional<UUID> removePlaying(ResourceLocation dimension, BlockPos pos) {
        Key key = new Key(dimension, pos.immutable());
        Entry entry = sessions.get(key);
        if (entry == null || !entry.playing()) {
            return Optional.empty();
        }
        return sessions.remove(key, entry) ? Optional.of(entry.sessionId()) : Optional.empty();
    }

    Optional<SessionSnapshot> findPlaying(UUID sessionId) {
        for (Map.Entry<Key, Entry> entry : sessions.entrySet()) {
            Entry session = entry.getValue();
            if (session.playing() && session.sessionId().equals(sessionId)) {
                Key key = entry.getKey();
                return Optional.of(new SessionSnapshot(key.dimension(), key.pos(), session.selection()));
            }
        }
        return Optional.empty();
    }

    long resumePosition(ResourceLocation dimension, BlockPos pos, ScriptureDiscSelection selection) {
        Entry entry = sessions.get(new Key(dimension, pos.immutable()));
        if (entry == null || !entry.selection().equals(selection)) {
            return 0L;
        }
        return entry.resumePositionMillis();
    }

    void pause(ResourceLocation dimension, BlockPos pos, long positionMillis) {
        Key key = new Key(dimension, pos.immutable());
        Entry entry = sessions.get(key);
        if (entry != null) {
            sessions.put(key, new Entry(entry.selection(), entry.sessionId(), Math.max(0L, positionMillis), false));
        }
    }

    Optional<UUID> remove(ResourceLocation dimension, BlockPos pos) {
        return Optional.ofNullable(sessions.remove(new Key(dimension, pos.immutable()))).map(Entry::sessionId);
    }

    private record Key(ResourceLocation dimension, BlockPos pos) {
    }

    private record Entry(ScriptureDiscSelection selection, UUID sessionId, long resumePositionMillis, boolean playing) {
    }

    record SessionSnapshot(ResourceLocation dimension, BlockPos pos, ScriptureDiscSelection selection) {
    }
}
