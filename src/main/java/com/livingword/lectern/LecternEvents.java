package com.livingword.lectern;

import com.livingword.bible.BibleReference;
import com.livingword.items.LivingWordItems;
import com.livingword.network.LivingWordNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class LecternEvents {
    private static final BibleReference DEFAULT_LECTERN_REFERENCE = new BibleReference("kjv", "john", 3, 1);
    private static final LecternListeningStationRegistry STATIONS = new LecternListeningStationRegistry();

    private LecternEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(LecternEvents::onRightClickBlock);
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (!level.getBlockState(event.getPos()).is(Blocks.LECTERN)) {
            return;
        }
        ItemStack held = event.getItemStack();
        if (!held.is(LivingWordItems.BIBLE.get())) {
            return;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (level.isClientSide() || !(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        LecternListeningStation station = new LecternListeningStation(event.getPos(), DEFAULT_LECTERN_REFERENCE);
        STATIONS.remember(level.dimension().location(), station);
        if (serverPlayer.isShiftKeyDown()) {
            LivingWordNetwork.startNearbyListeningSession(
                serverPlayer,
                station.selectedReference().translationId(),
                station.selectedReference().bookId(),
                station.selectedReference().chapter(),
                48.0D
            );
            serverPlayer.displayClientMessage(Component.translatable("message.livingword.lectern.session_started"), true);
        } else {
            serverPlayer.displayClientMessage(Component.translatable("message.livingword.lectern.station_ready"), true);
        }
    }
}
