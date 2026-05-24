package com.livingword.lectern;

import com.livingword.discs.ScriptureDiscPlaybackMode;
import com.livingword.discs.ScriptureDiscSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LecternStationSavedDataTest {
    @Test
    void savesAndLoadsLecternStationSelections() {
        LecternStationSavedData data = new LecternStationSavedData();
        BlockPos pos = new BlockPos(10, 64, -5);
        UUID displayId = UUID.randomUUID();
        LecternListeningStation station = new LecternListeningStation(
            pos,
            new ScriptureDiscSelection("bsb", "psalms", 23, "bsb-david", ScriptureDiscPlaybackMode.LOOP_CHAPTER),
            8700L,
            java.util.Optional.of(displayId)
        );

        data.put(station);
        CompoundTag saved = data.save(new CompoundTag(), null);
        LecternStationSavedData loaded = LecternStationSavedData.load(saved, null);

        LecternListeningStation restored = loaded.get(pos).orElseThrow();
        assertEquals(station.selection(), restored.selection());
        assertEquals(8700L, restored.resumePositionMillis());
        assertEquals(displayId, restored.displayEntityId().orElseThrow());
        assertTrue(restored.displayEnabled());
    }

    @Test
    void removingStationsMarksThemAbsent() {
        LecternStationSavedData data = new LecternStationSavedData();
        BlockPos pos = new BlockPos(1, 65, 1);

        data.put(new LecternListeningStation(pos, ScriptureDiscSelection.defaults()));
        data.remove(pos);

        assertTrue(data.get(pos).isEmpty());
    }
}
