package com.livingword.lectern;

import com.livingword.bible.BibleReference;
import com.livingword.discs.ScriptureDiscPlaybackMode;
import com.livingword.discs.ScriptureDiscSelection;
import net.minecraft.core.BlockPos;

import java.util.Optional;
import java.util.UUID;

public record LecternListeningStation(BlockPos sourcePos, ScriptureDiscSelection selection, long resumePositionMillis, Optional<UUID> displayEntityId, boolean displayEnabled) {
    public LecternListeningStation {
        if (sourcePos == null) {
            throw new IllegalArgumentException("sourcePos is required");
        }
        if (selection == null) {
            throw new IllegalArgumentException("selection is required");
        }
        resumePositionMillis = Math.max(0L, resumePositionMillis);
        displayEntityId = displayEntityId == null ? Optional.empty() : displayEntityId;
    }

    public LecternListeningStation(BlockPos sourcePos, ScriptureDiscSelection selection, long resumePositionMillis, Optional<UUID> displayEntityId) {
        this(sourcePos, selection, resumePositionMillis, displayEntityId, true);
    }

    public LecternListeningStation(BlockPos sourcePos, ScriptureDiscSelection selection) {
        this(sourcePos, selection, 0L, Optional.empty());
    }

    public LecternListeningStation(BlockPos sourcePos, BibleReference selectedReference) {
        this(
            sourcePos,
            new ScriptureDiscSelection(
                selectedReference.translationId(),
                selectedReference.bookId(),
                selectedReference.chapter(),
                "default",
                ScriptureDiscPlaybackMode.SINGLE_CHAPTER
            )
        );
    }

    public BibleReference selectedReference() {
        return new BibleReference(selection.translationId(), selection.bookId(), selection.chapter(), 1);
    }

    public LecternListeningStation withSelection(ScriptureDiscSelection nextSelection) {
        return new LecternListeningStation(sourcePos, nextSelection, 0L, displayEntityId, displayEnabled);
    }

    public LecternListeningStation withResumePosition(long positionMillis) {
        return new LecternListeningStation(sourcePos, selection, positionMillis, displayEntityId, displayEnabled);
    }

    public LecternListeningStation withDisplayEntityId(Optional<UUID> nextDisplayEntityId) {
        return new LecternListeningStation(sourcePos, selection, resumePositionMillis, nextDisplayEntityId, displayEnabled);
    }

    public LecternListeningStation withDisplayEnabled(boolean nextDisplayEnabled) {
        Optional<UUID> nextDisplayEntityId = nextDisplayEnabled ? displayEntityId : Optional.empty();
        return new LecternListeningStation(sourcePos, selection, resumePositionMillis, nextDisplayEntityId, nextDisplayEnabled);
    }
}
