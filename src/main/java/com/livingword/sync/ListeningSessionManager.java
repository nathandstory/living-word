package com.livingword.sync;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ListeningSessionManager {
    private final Map<UUID, ListeningSession> sessions = new HashMap<>();

    public ListeningSession create(String translationId, String bookId, int chapter, long serverMillis) {
        ListeningSession session = ListeningSession.started(translationId, bookId, chapter, serverMillis);
        sessions.put(session.id(), session);
        return session;
    }

    public Optional<ListeningSession> get(UUID sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public Collection<ListeningSession> sessions() {
        return List.copyOf(sessions.values());
    }

    public Optional<ListeningSession> remove(UUID sessionId) {
        return Optional.ofNullable(sessions.remove(sessionId));
    }

    public void join(UUID sessionId, UUID playerId) {
        update(sessionId, session -> session.withParticipant(playerId));
    }

    public void leave(UUID sessionId, UUID playerId) {
        update(sessionId, session -> session.withoutParticipant(playerId));
    }

    public void pause(UUID sessionId, long serverMillis) {
        update(sessionId, session -> session.pauseAt(serverMillis));
    }

    public void play(UUID sessionId, long serverMillis) {
        update(sessionId, session -> session.playAt(serverMillis));
    }

    public void seek(UUID sessionId, long positionMillis, long serverMillis) {
        update(sessionId, session -> session.seekTo(positionMillis, serverMillis));
    }

    public void control(UUID sessionId, PlaybackState state, long positionMillis, long serverMillis) {
        update(sessionId, session -> {
            ListeningSession positioned = session.seekTo(positionMillis, serverMillis);
            return switch (state) {
                case PLAYING -> positioned.playAt(serverMillis);
                case PAUSED -> positioned.pauseAt(serverMillis);
                case STOPPED -> positioned.stopAt();
            };
        });
    }

    private void update(UUID sessionId, java.util.function.Function<ListeningSession, ListeningSession> updater) {
        ListeningSession session = sessions.get(sessionId);
        if (session != null) {
            sessions.put(sessionId, updater.apply(session));
        }
    }
}
