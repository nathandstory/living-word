package com.livingword.items;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class BibleItemEntityProtection {
    private BibleItemEntityProtection() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(BibleItemEntityProtection::onEntityJoinLevel);
        NeoForge.EVENT_BUS.addListener(BibleItemEntityProtection::onEntityInvulnerabilityCheck);
        NeoForge.EVENT_BUS.addListener(BibleItemEntityProtection::onItemExpire);
        NeoForge.EVENT_BUS.addListener(BibleItemEntityProtection::onExplosionDetonate);
        NeoForge.EVENT_BUS.addListener(BibleItemEntityProtection::onEntityTickPre);
    }

    private static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ItemEntity itemEntity && isBibleItemEntity(itemEntity)) {
            protect(itemEntity);
        }
    }

    private static void onEntityInvulnerabilityCheck(EntityInvulnerabilityCheckEvent event) {
        if (isBibleItemEntity(event.getEntity())) {
            event.setInvulnerable(true);
        }
    }

    private static void onItemExpire(ItemExpireEvent event) {
        if (isBibleItemEntity(event.getEntity())) {
            event.getEntity().setUnlimitedLifetime();
            event.addExtraLife(Short.MAX_VALUE - 1);
        }
    }

    private static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        event.getAffectedEntities().removeIf(BibleItemEntityProtection::isBibleItemEntity);
    }

    private static void onEntityTickPre(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof ItemEntity itemEntity && isBibleItemEntity(itemEntity)) {
            rescueFromVoid(itemEntity);
        }
    }

    private static void protect(ItemEntity itemEntity) {
        itemEntity.setInvulnerable(true);
        itemEntity.setUnlimitedLifetime();
    }

    private static void rescueFromVoid(ItemEntity itemEntity) {
        Level level = itemEntity.level();
        if (itemEntity.getY() >= level.getMinBuildHeight() - 48) {
            return;
        }

        BlockPos spawn = level.getSharedSpawnPos();
        double rescueX = spawn.getX() + 0.5D;
        double rescueY = Math.max(spawn.getY() + 1.0D, level.getMinBuildHeight() + 1.0D);
        double rescueZ = spawn.getZ() + 0.5D;
        itemEntity.teleportTo(rescueX, rescueY, rescueZ);
        itemEntity.setDeltaMovement(Vec3.ZERO);
        protect(itemEntity);
        notifyVoidRescue(level, rescueX, rescueY, rescueZ);
    }

    private static void notifyVoidRescue(Level level, double x, double y, double z) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Component message = Component.literal(String.format(
            Locale.ROOT,
            "A Bible was sent to spawn after falling into the void (x=%.1f, y=%.1f, z=%.1f).",
            x,
            y,
            z
        ));
        serverLevel.players().forEach(player -> player.sendSystemMessage(message));
    }

    private static boolean isBibleItemEntity(Entity entity) {
        return entity instanceof ItemEntity itemEntity && itemEntity.getItem().is(LivingWordItems.BIBLE.get());
    }
}
