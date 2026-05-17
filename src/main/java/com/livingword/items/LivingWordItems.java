package com.livingword.items;

import com.livingword.LivingWord;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LivingWordItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LivingWord.MOD_ID);

    public static final DeferredItem<BibleItem> BIBLE = ITEMS.registerItem(
        "bible",
        BibleItem::new,
        new net.minecraft.world.item.Item.Properties().stacksTo(1)
    );

    private LivingWordItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
