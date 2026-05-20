package com.livingword.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class LivingWordClientItemModelPropertyTest {
    @Test
    void registersBibleOpenItemPredicateOnTheClient() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/LivingWordClientEvents.java"));

        assertTrue(source.contains("ItemProperties.register"));
        assertTrue(source.contains("ResourceLocation.fromNamespaceAndPath(LivingWord.MOD_ID, \"open\")"));
        assertTrue(source.contains("LivingWordClient.isBibleOpenInHand()"));
    }

    @Test
    void registersMapLikeBibleHandTransformOnTheClient() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/LivingWordClientEvents.java"));

        assertTrue(source.contains("RegisterClientExtensionsEvent"));
        assertTrue(source.contains("applyForgeHandTransform"));
        assertTrue(source.contains("BibleHeldItemTransform.forView"));
        assertTrue(source.contains("event.registerItem"));
    }

    @Test
    void rendersBibleWithTwoHandsWhenOffhandIsEmpty() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/LivingWordClientEvents.java"));

        assertTrue(source.contains("RenderHandEvent"));
        assertTrue(source.contains("renderTwoHandedBible"));
        assertTrue(source.contains("renderBibleHand"));
        assertTrue(source.contains("event.setCanceled(true)"));
    }

    @Test
    void twoHandBibleRendersHandsBeforeTheBookWithoutReversingFaceMappedArt() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/LivingWordClientEvents.java"));
        int methodStart = source.indexOf("private static void renderTwoHandedBible");
        int methodEnd = source.indexOf("private static float calculateBibleTilt", methodStart);
        String method = source.substring(methodStart, methodEnd);
        int handRender = source.indexOf("renderBibleHand(event, HumanoidArm.RIGHT)", methodStart);
        int itemRender = source.indexOf("renderItem(", methodStart);

        assertTrue(methodStart >= 0);
        assertTrue(handRender > methodStart);
        assertTrue(itemRender > methodStart);
        assertTrue(handRender < itemRender, "arms must render before the book model");
        assertTrue(!method.contains("Axis.YP.rotationDegrees(180.0F)"), "first-person closed Bible must not reverse the face-mapped front/back art");
    }

    @Test
    void twoHandBibleKeepsCoverUprightAndReadable() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/LivingWordClientEvents.java"));
        int methodStart = source.indexOf("private static void renderTwoHandedBible");
        int methodEnd = source.indexOf("private static float calculateBibleTilt", methodStart);
        String method = source.substring(methodStart, methodEnd);

        assertTrue(!method.contains("Axis.ZP.rotationDegrees(180.0F)"), "book render must not rotate around Z by 180 degrees; that flips the cover upside down");
        assertTrue(method.contains("BibleHeldItemTransform.twoHandedBookScale"), "two-handed scale should be constrained by the shared transform helper");
        assertTrue(method.contains("BibleHeldItemTransform.twoHandedBaseVerticalOffset"), "global reading pose should be adjusted separately from the hand-to-book grip");
    }

    @Test
    void twoHandBibleUsesCustomRaisedHandGripForTheScaledBook() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/LivingWordClientEvents.java"));
        int methodStart = source.indexOf("private static void renderBibleHand");
        int methodEnd = source.lastIndexOf('}');
        String method = source.substring(methodStart, methodEnd);

        assertTrue(method.contains("BibleHeldItemTransform.handGrip"), "hands should use the Bible-specific grip transform, not vanilla map hand constants");
        assertTrue(!method.contains("translate(side * 0.34F, -1.08F, 0.43F)"), "old hand pose left the smaller Bible floating above the hands");
    }

    @Test
    void clientExposesBibleScreenOpenStateForItemModels() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/LivingWordClient.java"));

        assertTrue(source.contains("boolean isBibleScreenOpen()"));
        assertTrue(source.contains("boolean isBibleOpenInHand()"));
        assertTrue(source.contains("beginBibleOpenAnimation()"));
        assertTrue(source.contains("tickBibleOpenAnimation()"));
        assertTrue(source.contains("screen instanceof BibleScreen"));
    }

    @Test
    void bibleItemStartsHeldOpenAnimationBeforeOpeningScreen() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/items/BibleItem.java"));

        assertTrue(source.contains("beginBibleOpenAnimation"));
        assertTrue(source.contains("getMethod(\"beginBibleOpenAnimation\")"));
    }
}
