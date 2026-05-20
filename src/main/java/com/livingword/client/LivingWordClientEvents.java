package com.livingword.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.livingword.LivingWord;
import com.livingword.items.LivingWordItems;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = LivingWord.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LivingWordClientEvents {
    private LivingWordClientEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.addListener(LivingWordClientEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(LivingWordClientEvents::onRenderHand);
        event.enqueueWork(() -> ItemProperties.register(
            LivingWordItems.BIBLE.get(),
            ResourceLocation.fromNamespaceAndPath(LivingWord.MOD_ID, "open"),
            (stack, level, entity, seed) -> LivingWordClient.isBibleOpenInHand() ? 1.0F : 0.0F
        ));
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(
                PoseStack poseStack,
                LocalPlayer player,
                HumanoidArm arm,
                ItemStack itemInHand,
                float partialTick,
                float equipProcess,
                float swingProcess
            ) {
                boolean rightHand = arm == HumanoidArm.RIGHT;
                float side = rightHand ? 1.0F : -1.0F;
                float swing = Mth.sqrt(swingProcess);
                BibleHeldItemTransform transform = BibleHeldItemTransform.forView(player.getViewXRot(partialTick), rightHand);

                poseStack.translate(
                    side * -0.06F * Mth.sin(swing * (float) Math.PI),
                    0.04F * Mth.sin(swingProcess * (float) Math.PI),
                    -0.06F * Mth.sin(swingProcess * (float) Math.PI)
                );
                poseStack.translate(
                    transform.sideOffset(),
                    transform.verticalOffset() + equipProcess * -0.6F,
                    transform.depthOffset()
                );
                poseStack.mulPose(Axis.YP.rotationDegrees(transform.yRotationDegrees()));
                poseStack.mulPose(Axis.XP.rotationDegrees(transform.xRotationDegrees()));
                poseStack.mulPose(Axis.ZP.rotationDegrees(transform.zRotationDegrees()));
                poseStack.scale(transform.scale(), transform.scale(), transform.scale());
                return true;
            }
        }, LivingWordItems.BIBLE.get());
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        LivingWordClient.tickBibleOpenAnimation();
    }

    private static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        if (event.getHand() == InteractionHand.OFF_HAND
            && player.getMainHandItem().is(LivingWordItems.BIBLE.get())
            && player.getOffhandItem().isEmpty()) {
            event.setCanceled(true);
            return;
        }

        if (event.getHand() != InteractionHand.MAIN_HAND
            || !event.getItemStack().is(LivingWordItems.BIBLE.get())
            || !player.getOffhandItem().isEmpty()) {
            return;
        }

        event.getPoseStack().pushPose();
        renderTwoHandedBible(event, player);
        event.getPoseStack().popPose();
        event.setCanceled(true);
    }

    private static void renderTwoHandedBible(RenderHandEvent event, LocalPlayer player) {
        float swing = Mth.sqrt(event.getSwingProgress());
        float swingLift = -0.2F * Mth.sin(event.getSwingProgress() * (float) Math.PI);
        float swingDepth = -0.4F * Mth.sin(swing * (float) Math.PI);
        float tilt = calculateBibleTilt(event.getInterpolatedPitch());

        event.getPoseStack().translate(
            0.0F,
            -swingLift / 2.0F,
            swingDepth
        );
        event.getPoseStack().translate(
            0.0F,
            BibleHeldItemTransform.twoHandedBaseVerticalOffset(event.getEquipProgress(), tilt),
            -0.72F
        );
        event.getPoseStack().mulPose(Axis.XP.rotationDegrees(tilt * -85.0F));

        if (!player.isInvisible()) {
            event.getPoseStack().pushPose();
            event.getPoseStack().mulPose(Axis.YP.rotationDegrees(90.0F));
            renderBibleHand(event, HumanoidArm.RIGHT);
            renderBibleHand(event, HumanoidArm.LEFT);
            event.getPoseStack().popPose();
        }

        float bookLift = Mth.sin(swing * (float) Math.PI);
        event.getPoseStack().mulPose(Axis.XP.rotationDegrees(bookLift * 16.0F));
        float bookScale = BibleHeldItemTransform.twoHandedBookScale(tilt);
        event.getPoseStack().scale(bookScale, bookScale, bookScale);
        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(
            player,
            event.getItemStack(),
            ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
            false,
            event.getPoseStack(),
            event.getMultiBufferSource(),
            event.getPackedLight()
        );
    }

    private static float calculateBibleTilt(float pitchDegrees) {
        float tilt = 1.0F - pitchDegrees / 45.0F + 0.1F;
        tilt = Mth.clamp(tilt, 0.0F, 1.0F);
        return -Mth.cos(tilt * (float) Math.PI) * 0.5F + 0.5F;
    }

    private static void renderBibleHand(RenderHandEvent event, HumanoidArm arm) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        PlayerRenderer renderer = (PlayerRenderer)minecraft.getEntityRenderDispatcher().<AbstractClientPlayer>getRenderer(minecraft.player);
        boolean rightHand = arm == HumanoidArm.RIGHT;
        BibleHeldItemTransform grip = BibleHeldItemTransform.handGrip(rightHand);

        event.getPoseStack().pushPose();
        event.getPoseStack().mulPose(Axis.YP.rotationDegrees(grip.yRotationDegrees()));
        event.getPoseStack().mulPose(Axis.XP.rotationDegrees(grip.xRotationDegrees()));
        event.getPoseStack().mulPose(Axis.ZP.rotationDegrees(grip.zRotationDegrees()));
        event.getPoseStack().translate(grip.sideOffset(), grip.verticalOffset(), grip.depthOffset());
        if (rightHand) {
            renderer.renderRightHand(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), minecraft.player);
        } else {
            renderer.renderLeftHand(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), minecraft.player);
        }
        event.getPoseStack().popPose();
    }
}
