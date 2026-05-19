package com.livingword.discs;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScriptureDiscTest {
    @Test
    void handlesJukeboxUseWithoutVanillaJukeboxSongComponent() {
        assertTrue(ScriptureDisc.handlesBlockUseId(ResourceLocation.withDefaultNamespace("jukebox")));
        assertFalse(ScriptureDisc.handlesBlockUseId(ResourceLocation.withDefaultNamespace("lectern")));
    }
}
