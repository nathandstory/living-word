package com.livingword.discs;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ScriptureDiscSelectionTest {
    @Test
    void defaultsToKjvJohnOne() {
        assertEquals(new ScriptureDiscSelection("kjv", "john", 1), ScriptureDiscSelection.fromRootTag(new CompoundTag()));
    }

    @Test
    void storesSelectionInItemCustomData() {
        CompoundTag root = new CompoundTag();
        ScriptureDiscSelection selection = new ScriptureDiscSelection("webp", "romans", 8);

        ScriptureDiscSelection.writeToRootTag(root, selection);

        assertEquals(selection, ScriptureDiscSelection.fromRootTag(root));
    }
}
