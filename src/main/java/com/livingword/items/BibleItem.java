package com.livingword.items;

import com.livingword.LivingWord;
import com.livingword.network.LivingWordNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class BibleItem extends Item {
    public BibleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            beginClientOpenAnimation();
        } else {
            if (player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer) {
                LivingWordNetwork.startNearbyListeningSession(serverPlayer, "kjv", "john", 3, 32.0D);
                player.displayClientMessage(Component.translatable("message.livingword.bible.nearby_listening_started"), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static void beginClientOpenAnimation() {
        try {
            Class<?> client = Class.forName("com.livingword.client.LivingWordClient");
            client.getMethod("beginBibleOpenAnimation").invoke(null);
        } catch (ReflectiveOperationException exception) {
            LivingWord.LOGGER.warn("Unable to open Living Word Bible screen", exception);
        }
    }
}
