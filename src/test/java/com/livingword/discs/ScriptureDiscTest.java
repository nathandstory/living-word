package com.livingword.discs;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScriptureDiscTest {
    @Test
    void handlesJukeboxUseWithoutVanillaJukeboxSongComponent() {
        assertTrue(ScriptureDisc.handlesBlockUseId(ResourceLocation.withDefaultNamespace("jukebox")));
        assertFalse(ScriptureDisc.handlesBlockUseId(ResourceLocation.withDefaultNamespace("lectern")));
    }

    @Test
    void jukeboxBlockUseDoesNotStartDuplicateClientLocalPlayback() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/discs/ScriptureDisc.java"));

        assertFalse(source.contains("if (level.isClientSide()) {\n            playLocalChapter();"));
    }
}
