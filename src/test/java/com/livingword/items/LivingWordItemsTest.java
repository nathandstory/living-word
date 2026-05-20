package com.livingword.items;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class LivingWordItemsTest {
    @Test
    void bibleItemIsFireResistantSoDroppedCopiesDoNotBurn() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/items/LivingWordItems.java"));
        int bibleRegistration = source.indexOf("public static final DeferredItem<BibleItem> BIBLE");
        int nextRegistration = source.indexOf("public static final DeferredItem<ScriptureDisc>", bibleRegistration);
        String bibleBlock = source.substring(bibleRegistration, nextRegistration);

        assertTrue(bibleBlock.contains(".fireResistant()"), "Bible item properties should mark dropped Bibles as fire resistant");
    }

    @Test
    void bibleAndScriptureDiscAreVisibleInCreativeSearchAndJeiIngredientDiscovery() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/items/LivingWordItems.java"));

        assertTrue(source.contains("BuildCreativeModeTabContentsEvent"), "Living Word items should contribute to creative tab contents");
        assertTrue(source.contains("CreativeModeTabs.TOOLS_AND_UTILITIES"), "Bible items should live in a vanilla creative tab");
        assertTrue(source.contains("CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS"), "Bible items should also appear in creative search/JEI discovery");
        assertTrue(source.contains("event.accept(BIBLE.get()"), "Holy Bible should be added to the creative tab/search entries");
        assertTrue(source.contains("event.accept(SCRIPTURE_DISC_JOHN.get()"), "Scripture Disc should be added to the creative tab/search entries");
    }
}
