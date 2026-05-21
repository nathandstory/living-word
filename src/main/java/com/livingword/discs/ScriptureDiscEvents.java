package com.livingword.discs;

import com.livingword.network.LivingWordNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.UUID;

public final class ScriptureDiscEvents {
    private static final JukeboxListeningSessionRegistry JUKEBOX_SESSIONS = new JukeboxListeningSessionRegistry();

    private ScriptureDiscEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ScriptureDiscEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(ScriptureDiscEvents::onBlockBreak);
    }

    public static void rememberJukeboxSession(ResourceLocation dimension, BlockPos pos, UUID sessionId) {
        rememberJukeboxSession(dimension, pos, ScriptureDiscSelection.defaults(), sessionId, 0L);
    }

    public static void rememberJukeboxSession(ResourceLocation dimension, BlockPos pos, ScriptureDiscSelection selection, UUID sessionId, long resumePositionMillis) {
        JUKEBOX_SESSIONS.remember(dimension, pos, selection, sessionId, resumePositionMillis);
    }

    public static long resumePosition(ResourceLocation dimension, BlockPos pos, ScriptureDiscSelection selection) {
        return JUKEBOX_SESSIONS.resumePosition(dimension, pos, selection);
    }

    public static boolean pauseJukeboxSession(ResourceLocation dimension, BlockPos pos) {
        return JUKEBOX_SESSIONS.get(dimension, pos)
            .flatMap(LivingWordNetwork::stopListeningSession)
            .map(stopped -> {
                JUKEBOX_SESSIONS.pause(dimension, pos, stopped.positionMillisAt(net.minecraft.Util.getMillis()));
                return true;
            }).orElse(false);
    }

    public static boolean stopJukeboxSession(ResourceLocation dimension, BlockPos pos) {
        return pauseJukeboxSession(dimension, pos);
    }

    public static boolean stopAndForgetJukeboxSession(ResourceLocation dimension, BlockPos pos) {
        return JUKEBOX_SESSIONS.remove(dimension, pos)
            .map(sessionId -> {
                LivingWordNetwork.stopListeningSession(sessionId);
                return true;
            }).orElse(false);
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !event.getItemStack().isEmpty()) {
            return;
        }
        Level clickedLevel = event.getLevel();
        if (!clickedLevel.getBlockState(event.getPos()).is(Blocks.JUKEBOX)) {
            return;
        }
        if (!(clickedLevel.getBlockEntity(event.getPos()) instanceof JukeboxBlockEntity jukebox)) {
            return;
        }
        ItemStack inserted = jukebox.getTheItem();
        if (!(inserted.getItem() instanceof ScriptureDisc)) {
            return;
        }

        consume(event);
        if (clickedLevel.isClientSide() || !(clickedLevel instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ResourceLocation dimension = level.dimension().location();
        BlockPos pos = event.getPos();
        if (serverPlayer.isShiftKeyDown()) {
            stopAndForgetJukeboxSession(dimension, pos);
            jukebox.popOutTheItem();
            serverPlayer.displayClientMessage(Component.translatable("message.livingword.disc.session_reset"), true);
            return;
        }
        if (pauseJukeboxSession(dimension, pos)) {
            serverPlayer.displayClientMessage(Component.translatable("message.livingword.disc.session_paused"), true);
            return;
        }

        ScriptureDiscSelection selection = ScriptureDiscSelection.from(inserted);
        long resumePositionMillis = resumePosition(dimension, pos, selection);
        var session = LivingWordNetwork.startPositionedListeningSession(
            serverPlayer,
            pos,
            selection.translationId(),
            selection.bookId(),
            selection.chapter(),
            selection.audioManifestId(),
            48.0D,
            resumePositionMillis
        );
        rememberJukeboxSession(dimension, pos, selection, session.id(), resumePositionMillis);
        serverPlayer.displayClientMessage(Component.translatable("message.livingword.disc.session_started", formatSelection(selection)), true);
    }

    private static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getState().getBlock() != Blocks.JUKEBOX) {
            return;
        }
        stopAndForgetJukeboxSession(level.dimension().location(), event.getPos());
    }

    private static void consume(PlayerInteractEvent.RightClickBlock event) {
        event.setCancellationResult(InteractionResult.CONSUME);
        event.setCanceled(true);
    }

    private static String formatSelection(ScriptureDiscSelection selection) {
        return selection.translationId().toUpperCase(java.util.Locale.ROOT)
            + " / "
            + selection.bookId().replace('_', ' ')
            + " "
            + selection.chapter();
    }
}
