package com.livingword.discs;

import com.livingword.network.LivingWordNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.UUID;

public final class ScriptureDiscEvents {
    private static final JukeboxListeningSessionRegistry JUKEBOX_SESSIONS = new JukeboxListeningSessionRegistry();

    private ScriptureDiscEvents() {
    }

    public static void register() {
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

    private static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getState().getBlock() != Blocks.JUKEBOX) {
            return;
        }
        stopAndForgetJukeboxSession(level.dimension().location(), event.getPos());
    }
}
