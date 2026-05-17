package com.livingword.sync;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public record ListeningSession(
    UUID id,
    String translationId,
    String bookId,
    int chapter,
    PlaybackState state,
    long startServerMillis,
    long pausedPositionMillis,
    Set<UUID> participants
) {
    public ListeningSession {
        if (id == null) {
            throw new IllegalArgumentException("session id is required");
        }
        if (translationId == null || translationId.isBlank()) {
            throw new IllegalArgumentException("translationId is required");
        }
        if (bookId == null || bookId.isBlank()) {
            throw new IllegalArgumentException("bookId is required");
        }
        if (chapter < 1) {
            throw new IllegalArgumentException("chapter must be positive");
        }
        if (state == null) {
            throw new IllegalArgumentException("state is required");
        }
        participants = Set.copyOf(participants == null ? Set.of() : participants);
    }

    public static ListeningSession started(String translationId, String bookId, int chapter, long serverMillis) {
        return new ListeningSession(UUID.randomUUID(), translationId, bookId, chapter, PlaybackState.PLAYING, serverMillis, 0L, Set.of());
    }

    public long positionMillisAt(long serverMillis) {
        if (state != PlaybackState.PLAYING) {
            return pausedPositionMillis;
        }
        return Math.max(0L, serverMillis - startServerMillis);
    }

    public ListeningSession pauseAt(long serverMillis) {
        return new ListeningSession(id, translationId, bookId, chapter, PlaybackState.PAUSED, startServerMillis, positionMillisAt(serverMillis), participants);
    }

    public ListeningSession playAt(long serverMillis) {
        long startMillis = serverMillis - pausedPositionMillis;
        return new ListeningSession(id, translationId, bookId, chapter, PlaybackState.PLAYING, startMillis, pausedPositionMillis, participants);
    }

    public ListeningSession seekTo(long positionMillis, long serverMillis) {
        long clamped = Math.max(0L, positionMillis);
        long startMillis = state == PlaybackState.PLAYING ? serverMillis - clamped : startServerMillis;
        return new ListeningSession(id, translationId, bookId, chapter, state, startMillis, clamped, participants);
    }

    public ListeningSession withParticipant(UUID playerId) {
        LinkedHashSet<UUID> next = new LinkedHashSet<>(participants);
        next.add(playerId);
        return new ListeningSession(id, translationId, bookId, chapter, state, startServerMillis, pausedPositionMillis, next);
    }

    public ListeningSession withoutParticipant(UUID playerId) {
        LinkedHashSet<UUID> next = new LinkedHashSet<>(participants);
        next.remove(playerId);
        return new ListeningSession(id, translationId, bookId, chapter, state, startServerMillis, pausedPositionMillis, next);
    }
}
