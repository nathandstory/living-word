package com.livingword.items;

import com.livingword.LivingWord;
import com.livingword.discs.ScriptureDisc;
import com.livingword.discs.ScriptureDiscRegistry;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LivingWordItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LivingWord.MOD_ID);

    public static final DeferredItem<BibleItem> BIBLE = ITEMS.registerItem(
        "bible",
        BibleItem::new,
        new net.minecraft.world.item.Item.Properties().stacksTo(1).fireResistant()
    );

    public static final DeferredItem<ScriptureDisc> SCRIPTURE_DISC_JOHN = ITEMS.registerItem(
        "scripture_disc_john",
        ScriptureDiscRegistry::johnDisc,
        new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(Rarity.RARE)
    );

    public static final DeferredItem<ShofarItem> SHOFAR = ITEMS.registerItem(
        "shofar",
        ShofarItem::new,
        new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)
    );

    private LivingWordItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        modEventBus.addListener(LivingWordItems::addCreativeTabContents);
    }

    private static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(BIBLE.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.accept(SCRIPTURE_DISC_JOHN.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.accept(SHOFAR.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}
