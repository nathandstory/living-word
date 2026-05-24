package com.livingword.lectern;

import com.livingword.discs.ScriptureDiscSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LecternListeningStationRegistry {
    private final Map<Key, LecternListeningStation> stations = new ConcurrentHashMap<>();
    private final Map<Key, SessionEntry> sessions = new ConcurrentHashMap<>();

    public void remember(ResourceLocation dimension, LecternListeningStation station) {
        stations.put(new Key(dimension, station.sourcePos().immutable()), station);
    }

    public Optional<LecternListeningStation> get(ResourceLocation dimension, BlockPos sourcePos) {
        return Optional.ofNullable(stations.get(new Key(dimension, sourcePos.immutable())));
    }

    public Optional<LecternListeningStation> remove(ResourceLocation dimension, BlockPos sourcePos) {
        return Optional.ofNullable(stations.remove(new Key(dimension, sourcePos.immutable())));
    }

    public void rememberSession(ResourceLocation dimension, BlockPos sourcePos, ScriptureDiscSelection selection, UUID sessionId, long resumePositionMillis) {
        sessions.put(new Key(dimension, sourcePos.immutable()), new SessionEntry(selection, sessionId, Math.max(0L, resumePositionMillis), true));
    }

    public Optional<UUID> getPlayingSession(ResourceLocation dimension, BlockPos sourcePos) {
        SessionEntry entry = sessions.get(new Key(dimension, sourcePos.immutable()));
        return entry != null && entry.playing() ? Optional.of(entry.sessionId()) : Optional.empty();
    }

    public Optional<UUID> removePlaying(ResourceLocation dimension, BlockPos sourcePos) {
        Key key = new Key(dimension, sourcePos.immutable());
        SessionEntry entry = sessions.get(key);
        if (entry == null || !entry.playing()) {
            return Optional.empty();
        }
        return sessions.remove(key, entry) ? Optional.of(entry.sessionId()) : Optional.empty();
    }

    public Optional<UUID> removeSession(ResourceLocation dimension, BlockPos sourcePos) {
        return Optional.ofNullable(sessions.remove(new Key(dimension, sourcePos.immutable()))).map(SessionEntry::sessionId);
    }

    public void pause(ResourceLocation dimension, BlockPos sourcePos, long positionMillis) {
        Key key = new Key(dimension, sourcePos.immutable());
        SessionEntry entry = sessions.get(key);
        if (entry != null) {
            sessions.put(key, new SessionEntry(entry.selection(), entry.sessionId(), Math.max(0L, positionMillis), false));
        }
    }

    public long resumePosition(ResourceLocation dimension, BlockPos sourcePos, ScriptureDiscSelection selection) {
        SessionEntry entry = sessions.get(new Key(dimension, sourcePos.immutable()));
        if (entry == null || !entry.selection().equals(selection)) {
            return 0L;
        }
        return entry.resumePositionMillis();
    }

    public Optional<SessionSnapshot> findPlaying(UUID sessionId) {
        for (Map.Entry<Key, SessionEntry> entry : sessions.entrySet()) {
            SessionEntry session = entry.getValue();
            if (session.playing() && session.sessionId().equals(sessionId)) {
                Key key = entry.getKey();
                return Optional.of(new SessionSnapshot(key.dimension(), key.sourcePos(), session.selection()));
            }
        }
        return Optional.empty();
    }

    public List<PlayingSessionSnapshot> playingSessions() {
        return sessions.entrySet().stream()
            .filter(entry -> entry.getValue().playing())
            .map(entry -> {
                Key key = entry.getKey();
                SessionEntry session = entry.getValue();
                return new PlayingSessionSnapshot(key.dimension(), key.sourcePos(), session.selection(), session.sessionId());
            })
            .toList();
    }

    private record Key(ResourceLocation dimension, BlockPos sourcePos) {
    }

    private record SessionEntry(ScriptureDiscSelection selection, UUID sessionId, long resumePositionMillis, boolean playing) {
    }

    public record SessionSnapshot(ResourceLocation dimension, BlockPos sourcePos, ScriptureDiscSelection selection) {
    }

    public record PlayingSessionSnapshot(ResourceLocation dimension, BlockPos sourcePos, ScriptureDiscSelection selection, UUID sessionId) {
    }
}
