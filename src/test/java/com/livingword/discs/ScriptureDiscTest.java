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

    @Test
    void tooltipDocumentsConfigureJukeboxAndResetControls() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/discs/ScriptureDisc.java"));

        assertTrue(source.contains("item.livingword.scripture_disc.tooltip.configure"));
        assertTrue(source.contains("item.livingword.scripture_disc.tooltip.jukebox"));
        assertTrue(source.contains("item.livingword.scripture_disc.tooltip.stop"));
        assertTrue(source.contains("message.livingword.disc.session_paused"));
        assertTrue(source.contains("message.livingword.disc.session_reset"));
    }

    @Test
    void discInHandDoesNotHaveHiddenSneakStartSessionShortcut() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/discs/ScriptureDisc.java"));
        int useMethodStart = source.indexOf("public InteractionResultHolder<ItemStack> use(");
        int tooltipStart = source.indexOf("public void appendHoverText", useMethodStart);
        String useMethod = source.substring(useMethodStart, tooltipStart);

        assertFalse(useMethod.contains("player.isShiftKeyDown()"));
        assertFalse(useMethod.contains("startNearbyListeningSession"));
    }
}
