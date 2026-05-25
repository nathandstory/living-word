package com.livingword.sounds;

import com.livingword.LivingWord;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LivingWordSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, LivingWord.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> SHOFAR_BLOW = SOUNDS.register(
        "shofar_blow",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(LivingWord.MOD_ID, "shofar_blow"))
    );

    private LivingWordSounds() {
    }

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
