package com.livingword.discs;

import com.livingword.LivingWord;
import com.livingword.network.LivingWordNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public final class ScriptureDisc extends Item {
    private final String displayKey;
    private final String translationId;
    private final String bookId;
    private final int startChapter;
    private final int endChapter;
    private final String audioManifestId;

    public ScriptureDisc(
        Properties properties,
        String displayKey,
        String translationId,
        String bookId,
        int startChapter,
        int endChapter,
        String audioManifestId
    ) {
        super(properties);
        this.displayKey = displayKey;
        this.translationId = translationId;
        this.bookId = bookId;
        this.startChapter = startChapter;
        this.endChapter = endChapter;
        this.audioManifestId = audioManifestId;
    }

    public String displayKey() {
        return displayKey;
    }

    public String translationId() {
        return translationId;
    }

    public String bookId() {
        return bookId;
    }

    public int startChapter() {
        return startChapter;
    }

    public int endChapter() {
        return endChapter;
    }

    public String audioManifestId() {
        return audioManifestId;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            playLocalChapter();
        } else if (player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer) {
            LivingWordNetwork.startNearbyListeningSession(serverPlayer, translationId, bookId, startChapter, 48.0D);
            serverPlayer.displayClientMessage(Component.translatable("message.livingword.disc.session_started"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(displayKey).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.livingword.scripture_disc.tooltip").withStyle(ChatFormatting.GRAY));
    }

    private void playLocalChapter() {
        try {
            Class<?> client = Class.forName("com.livingword.client.LivingWordClient");
            client.getMethod("playLocalChapter", String.class, String.class, int.class).invoke(null, translationId, bookId, startChapter);
        } catch (ReflectiveOperationException exception) {
            LivingWord.LOGGER.warn("Unable to play local Scripture Disc chapter", exception);
        }
    }
}
