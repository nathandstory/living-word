package com.livingword.discs;

import com.livingword.bible.BibleDataManager;
import com.livingword.bible.BibleResourceLoader;
import com.livingword.lectern.LecternEvents;
import com.livingword.network.LivingWordNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
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
    private static BibleDataManager bibleDataManager;

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

    public static boolean stopActiveJukeboxSession(ResourceLocation dimension, BlockPos pos) {
        return JUKEBOX_SESSIONS.removePlaying(dimension, pos)
            .map(sessionId -> {
                LivingWordNetwork.stopListeningSession(sessionId);
                return true;
            }).orElse(false);
    }

    public static boolean completeJukeboxChapter(ServerPlayer player, UUID sessionId) {
        return JUKEBOX_SESSIONS.findPlaying(sessionId)
            .map(snapshot -> {
                ServerLevel level = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, snapshot.dimension()));
                if (level == null) {
                    return false;
                }
                if (!hasMatchingInsertedDisc(level, snapshot.pos(), snapshot.selection())) {
                    LivingWordNetwork.stopListeningSession(sessionId);
                    JUKEBOX_SESSIONS.remove(snapshot.dimension(), snapshot.pos());
                    return true;
                }
                LivingWordNetwork.stopListeningSession(sessionId);
                var nextSelection = ScriptureDiscPlaybackSequencer.nextSelection(dataManager(), snapshot.selection());
                if (nextSelection.isEmpty()) {
                    JUKEBOX_SESSIONS.pause(snapshot.dimension(), snapshot.pos(), 0L);
                    return true;
                }
                ScriptureDiscSelection next = nextSelection.orElseThrow();
                updateInsertedDiscSelection(level, snapshot.pos(), next);
                var nextSession = LivingWordNetwork.startPositionedListeningSession(
                    level,
                    snapshot.pos(),
                    next.translationId(),
                    next.bookId(),
                    next.chapter(),
                    next.audioManifestId(),
                    48.0D,
                    0L
                );
                rememberJukeboxSession(snapshot.dimension(), snapshot.pos(), next, nextSession.id(), 0L);
                return true;
            })
            .orElse(false);
    }

    public static void pauseSessionsForParticipant(ServerPlayer player) {
        long now = net.minecraft.Util.getMillis();
        UUID playerId = player.getUUID();
        for (JukeboxListeningSessionRegistry.PlayingSessionSnapshot snapshot : JUKEBOX_SESSIONS.playingSessions()) {
            var session = LivingWordNetwork.currentListeningSession(snapshot.sessionId());
            if (session.isEmpty() || !session.orElseThrow().participants().contains(playerId)) {
                continue;
            }
            long resumePosition = LivingWordNetwork.stopListeningSession(snapshot.sessionId())
                .map(stopped -> stopped.positionMillisAt(now))
                .orElseGet(() -> session.orElseThrow().positionMillisAt(now));
            JUKEBOX_SESSIONS.pause(snapshot.dimension(), snapshot.pos(), resumePosition);
        }
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
        LecternEvents.pauseSessionsForParticipant(serverPlayer);
        pauseSessionsForParticipant(serverPlayer);
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

    private static void updateInsertedDiscSelection(ServerLevel level, BlockPos pos, ScriptureDiscSelection selection) {
        if (level.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox && jukebox.getTheItem().getItem() instanceof ScriptureDisc) {
            ScriptureDiscSelection.write(jukebox.getTheItem(), selection);
            jukebox.setChanged();
        }
    }

    private static boolean hasMatchingInsertedDisc(ServerLevel level, BlockPos pos, ScriptureDiscSelection selection) {
        if (!(level.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox)) {
            return false;
        }
        ItemStack inserted = jukebox.getTheItem();
        return inserted.getItem() instanceof ScriptureDisc && ScriptureDiscSelection.from(inserted).equals(selection);
    }

    private static BibleDataManager dataManager() {
        if (bibleDataManager == null) {
            bibleDataManager = new BibleDataManager();
            new BibleResourceLoader(bibleDataManager, ScriptureDiscEvents.class.getClassLoader()).reload();
        }
        return bibleDataManager;
    }
}
