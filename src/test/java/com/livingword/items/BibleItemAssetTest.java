package com.livingword.items;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BibleItemAssetTest {
    private static final Path ASSET_ROOT = Path.of("src/main/resources/assets/livingword");

    @Test
    void bibleItemHasClosedAndOpenPixelTextures() throws Exception {
        BufferedImage frontCover = ImageIO.read(ASSET_ROOT.resolve("textures/item/bible/closed/front_cover.png").toFile());
        BufferedImage backCover = ImageIO.read(ASSET_ROOT.resolve("textures/item/bible/closed/back_cover.png").toFile());
        BufferedImage spine = ImageIO.read(ASSET_ROOT.resolve("textures/item/bible/closed/spine.png").toFile());
        BufferedImage pageRightEdge = ImageIO.read(ASSET_ROOT.resolve("textures/item/bible/closed/page_right_edge.png").toFile());
        BufferedImage pageTopEdge = ImageIO.read(ASSET_ROOT.resolve("textures/item/bible/closed/page_top_edge.png").toFile());
        BufferedImage pageBottomEdge = ImageIO.read(ASSET_ROOT.resolve("textures/item/bible/closed/page_bottom_edge.png").toFile());
        BufferedImage open = ImageIO.read(ASSET_ROOT.resolve("textures/item/bible/open/open.png").toFile());
        BufferedImage openLeftEdge = ImageIO.read(ASSET_ROOT.resolve("textures/item/bible/open/open_left_edge.png").toFile());
        BufferedImage openRightEdge = ImageIO.read(ASSET_ROOT.resolve("textures/item/bible/open/open_right_edge.png").toFile());
        BufferedImage openTopEdge = ImageIO.read(ASSET_ROOT.resolve("textures/item/bible/open/open_top_edge.png").toFile());
        BufferedImage openBottomEdge = ImageIO.read(ASSET_ROOT.resolve("textures/item/bible/open/open_bottom_edge.png").toFile());

        assertNotNull(frontCover);
        assertNotNull(backCover);
        assertNotNull(spine);
        assertNotNull(pageRightEdge);
        assertNotNull(pageTopEdge);
        assertNotNull(pageBottomEdge);
        assertNotNull(open);
        assertNotNull(openLeftEdge);
        assertNotNull(openRightEdge);
        assertNotNull(openTopEdge);
        assertNotNull(openBottomEdge);
        assertEquals(512, frontCover.getWidth());
        assertEquals(768, frontCover.getHeight());
        assertEquals(512, backCover.getWidth());
        assertEquals(768, backCover.getHeight());
        assertEquals(128, spine.getWidth());
        assertEquals(768, spine.getHeight());
        assertEquals(192, pageRightEdge.getWidth());
        assertEquals(768, pageRightEdge.getHeight());
        assertEquals(512, pageTopEdge.getWidth());
        assertEquals(128, pageTopEdge.getHeight());
        assertEquals(512, pageBottomEdge.getWidth());
        assertEquals(128, pageBottomEdge.getHeight());
        assertEquals(1219, open.getWidth());
        assertEquals(784, open.getHeight());
        assertEquals(96, openLeftEdge.getWidth());
        assertEquals(784, openLeftEdge.getHeight());
        assertEquals(96, openRightEdge.getWidth());
        assertEquals(784, openRightEdge.getHeight());
        assertEquals(1219, openTopEdge.getWidth());
        assertEquals(96, openTopEdge.getHeight());
        assertEquals(1219, openBottomEdge.getWidth());
        assertEquals(96, openBottomEdge.getHeight());
        assertTrue(distinctOpaqueColors(frontCover) >= 24, "front cover should retain the supplied high-resolution cover art");
        assertTrue(distinctOpaqueColors(pageRightEdge) >= 24, "right edge should show a high-resolution page stack");
        assertTrue(distinctOpaqueColors(open) >= 24, "open Bible should retain the supplied high-resolution opened-book art");
        assertTrue(distinctOpaqueColors(openLeftEdge) >= 10, "open left edge should be derived from the supplied opened-book art");
        assertTrue(distinctOpaqueColors(openRightEdge) >= 10, "open right edge should be derived from the supplied opened-book art");
        assertTrue(distinctOpaqueColors(openTopEdge) >= 10, "open top edge should be derived from the supplied opened-book art");
        assertTrue(distinctOpaqueColors(openBottomEdge) >= 10, "open bottom edge should be derived from the supplied opened-book art");
    }

    @Test
    void bibleModelUsesReadingTransformsAndOpenStateOverride() throws Exception {
        String closedModel = Files.readString(ASSET_ROOT.resolve("models/item/bible.json"));
        String openModel = Files.readString(ASSET_ROOT.resolve("models/item/bible_open.json"));

        assertTrue(closedModel.contains("\"livingword:item/bible/closed/front_cover\""));
        assertTrue(closedModel.contains("\"livingword:item/bible/closed/back_cover\""));
        assertTrue(closedModel.contains("\"livingword:item/bible/closed/spine\""));
        assertTrue(closedModel.contains("\"livingword:item/bible/closed/page_right_edge\""));
        assertTrue(closedModel.contains("\"livingword:item/bible/closed/page_top_edge\""));
        assertTrue(closedModel.contains("\"livingword:item/bible/closed/page_bottom_edge\""));
        assertTrue(closedModel.contains("\"elements\""), "closed Bible should be a real 3D model");
        assertTrue(closedModel.contains("\"from\""));
        assertTrue(closedModel.contains("\"to\""));
        assertTrue(closedModel.contains("\"livingword:open\""));
        assertTrue(closedModel.contains("\"model\": \"livingword:item/bible_open\""));
        assertTrue(closedModel.contains("\"firstperson_righthand\""));
        assertTrue(closedModel.contains("\"thirdperson_righthand\""));
        assertTrue(openModel.contains("\"livingword:item/bible/open/open\""));
        assertTrue(openModel.contains("\"livingword:item/bible/open/open_left_edge\""));
        assertTrue(openModel.contains("\"livingword:item/bible/open/open_right_edge\""));
        assertTrue(openModel.contains("\"livingword:item/bible/open/open_top_edge\""));
        assertTrue(openModel.contains("\"livingword:item/bible/open/open_bottom_edge\""));
        assertTrue(openModel.contains("\"elements\""), "open Bible should be a real 3D model");
        assertTrue(openModel.contains("\"texture\": \"#open\""), "open Bible should use the supplied opened-book art as its visible surface");
        assertTrue(!openModel.contains("\"livingword:item/bible_open\""), "open Bible should no longer use the old generated open texture");
        assertTrue(!openModel.contains("\"livingword:item/bible_edges\""), "open Bible should use edge crops from open.png");
        assertTrue(!openModel.contains("\"livingword:item/bible_gold\""), "open Bible should not add separate gold slabs over the supplied art");
        assertTrue(!openModel.contains("\"texture\": \"#gold\""), "open Bible should get trim details directly from the supplied art");
        assertTrue(!closedModel.contains("\"texture\": \"#gold\""), "closed book should get gold details from the supplied front-cover art, not extra model slabs");
        assertTrue(!closedModel.contains("\"texture\": \"#cover\""), "closed book should use one texture per supplied face");
        assertTrue(openModel.contains("\"firstperson_righthand\""));
    }

    private static int distinctOpaqueColors(BufferedImage image) {
        Set<Integer> colors = new HashSet<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                if (((argb >>> 24) & 0xFF) > 0) {
                    colors.add(argb);
                }
            }
        }
        return colors.size();
    }

}
