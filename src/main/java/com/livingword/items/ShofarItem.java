package com.livingword.items;

import com.livingword.sounds.LivingWordSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ShofarItem extends Item {
    private static final int COOLDOWN_TICKS = 120;
    private static final float LONG_RANGE_VOLUME = 8.0F;

    public ShofarItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            float pitch = 0.96F + level.getRandom().nextFloat() * 0.08F;
            level.playSound(null, player.getX(), player.getY(), player.getZ(), LivingWordSounds.SHOFAR_BLOW.get(), SoundSource.RECORDS, LONG_RANGE_VOLUME, pitch);
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
