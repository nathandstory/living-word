package com.livingword;

import com.livingword.config.LivingWordConfig;
import com.livingword.daily.DailyVerseEvents;
import com.livingword.items.LivingWordItems;
import com.livingword.lectern.LecternEvents;
import com.livingword.network.LivingWordNetwork;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(LivingWord.MOD_ID)
public final class LivingWord {
    public static final String MOD_ID = "livingword";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LivingWord(IEventBus modEventBus, ModContainer modContainer) {
        LivingWordConfig.register(modContainer);
        LivingWordItems.register(modEventBus);
        LecternEvents.register();
        LivingWordNetwork.register(modEventBus);
        DailyVerseEvents.register();
    }
}
