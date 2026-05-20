package com.livingword.discs;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ScriptureDiscSelectionTest {
    @Test
    void defaultsToBsbJohnOneWithDavidNarratorAndSingleChapterMode() {
        assertEquals(
            new ScriptureDiscSelection("bsb", "john", 1, "default", ScriptureDiscPlaybackMode.SINGLE_CHAPTER),
            ScriptureDiscSelection.fromRootTag(new CompoundTag())
        );
    }

    @Test
    void storesFullSelectionInItemCustomData() {
        CompoundTag root = new CompoundTag();
        ScriptureDiscSelection selection = new ScriptureDiscSelection("bsb", "romans", 8, "hays", ScriptureDiscPlaybackMode.CONTINUE_BOOK);

        ScriptureDiscSelection.writeToRootTag(root, selection);

        assertEquals(selection, ScriptureDiscSelection.fromRootTag(root));
    }

    @Test
    void readsOlderDiscDataWithDefaultAudioSourceAndMode() {
        CompoundTag root = new CompoundTag();
        ScriptureDiscSelection.writeToRootTag(root, new ScriptureDiscSelection("webp", "romans", 8));

        assertEquals(
            new ScriptureDiscSelection("webp", "romans", 8, "default", ScriptureDiscPlaybackMode.SINGLE_CHAPTER),
            ScriptureDiscSelection.fromRootTag(root)
        );
    }

    @Test
    void knowsBundledAudioSourceChoicesByTranslation() {
        assertEquals("David", ScriptureDiscAudioSource.defaultFor("bsb").displayName());
        assertEquals("hays", ScriptureDiscAudioSource.cycle("bsb", "default", 1).manifestId());
        assertEquals("souer", ScriptureDiscAudioSource.cycle("bsb", "hays", 1).manifestId());
        assertEquals("default", ScriptureDiscAudioSource.cycle("bsb", "souer", 1).manifestId());
        assertEquals("Default", ScriptureDiscAudioSource.defaultFor("kjv").displayName());
    }
}
