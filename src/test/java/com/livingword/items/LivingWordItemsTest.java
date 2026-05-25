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

    @Test
    void shofarIsRegisteredVisibleAndHasClientAssets() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/items/LivingWordItems.java"));

        assertTrue(source.contains("DeferredItem<ShofarItem> SHOFAR"), "Shofar should be a registered Living Word item");
        assertTrue(source.contains("event.accept(SHOFAR.get()"), "Shofar should be visible in creative search/JEI discovery");
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/livingword/models/item/shofar.json")), "Shofar needs an item model");
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/livingword/textures/item/shofar.png")), "Shofar needs an item texture");
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/livingword/sounds/shofar_blow.ogg")), "Shofar needs a bundled horn sound");
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/livingword/sounds/ATTRIBUTION.md")), "Bundled shofar sound needs source attribution");
    }

    @Test
    void shofarUsesCeremonialLongRangeBlastTuning() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/items/ShofarItem.java"));

        assertTrue(source.contains("COOLDOWN_TICKS = 120"), "Shofar cooldown should match the longer ceremonial blast");
        assertTrue(source.contains("LONG_RANGE_VOLUME = 8.0F"), "Shofar should be audible across a large gathering area");
    }

    @Test
    void shofarHeldModelFacesMouthpieceTowardPlayer() throws Exception {
        String model = Files.readString(Path.of("src/main/resources/assets/livingword/models/item/shofar.json"));
        String thirdPersonRightHandBlock = model.substring(model.indexOf("\"thirdperson_righthand\""), model.indexOf("\"thirdperson_lefthand\""));
        String thirdPersonLeftHandBlock = model.substring(model.indexOf("\"thirdperson_lefthand\""), model.indexOf("\"firstperson_righthand\""));
        String firstPersonRightHandBlock = model.substring(model.indexOf("\"firstperson_righthand\""), model.indexOf("\"firstperson_lefthand\""));
        String firstPersonLeftHandBlock = model.substring(model.indexOf("\"firstperson_lefthand\""));

        assertTrue(thirdPersonRightHandBlock.contains("\"rotation\": [0, -90, 215]"), "Right hand third-person transform should roll the bell away from the hand");
        assertTrue(thirdPersonRightHandBlock.contains("\"scale\": [0.85, 0.85, 0.85]"), "Right hand third-person transform should keep positive scale so the held model renders normally");
        assertTrue(thirdPersonLeftHandBlock.contains("\"rotation\": [0, 90, -215]"), "Left hand third-person transform should mirror the corrected right-hand orientation");
        assertTrue(thirdPersonLeftHandBlock.contains("\"scale\": [0.85, 0.85, 0.85]"), "Left hand third-person transform should keep positive scale so the held model renders normally");
        assertTrue(firstPersonRightHandBlock.contains("\"rotation\": [0, -90, 25]"), "Right hand first-person transform should be rotated 180 degrees from the third-person roll");
        assertTrue(firstPersonRightHandBlock.contains("\"scale\": [0.9, 0.9, 0.9]"), "Right hand first-person transform should keep positive scale so the held model renders normally");
        assertTrue(firstPersonLeftHandBlock.contains("\"rotation\": [0, 90, -25]"), "Left hand first-person transform should mirror the corrected right-hand orientation");
        assertTrue(firstPersonLeftHandBlock.contains("\"scale\": [0.9, 0.9, 0.9]"), "Left hand first-person transform should keep positive scale so the held model renders normally");
    }
}
