package com.livingword.lectern;

import com.livingword.bible.BibleReference;
import net.minecraft.core.BlockPos;

public record LecternListeningStation(BlockPos sourcePos, BibleReference selectedReference) {
    public LecternListeningStation {
        if (sourcePos == null) {
            throw new IllegalArgumentException("sourcePos is required");
        }
        if (selectedReference == null) {
            throw new IllegalArgumentException("selectedReference is required");
        }
    }
}
