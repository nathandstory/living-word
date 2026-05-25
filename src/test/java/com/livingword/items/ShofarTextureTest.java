package com.livingword.items;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ShofarTextureTest {
    @Test
    void shofarTextureUsesHardOpaqueEdges() throws Exception {
        BufferedImage image = ImageIO.read(Path.of("src/main/resources/assets/livingword/textures/item/shofar.png").toFile());
        int semiTransparentPixels = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = (image.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha > 0 && alpha < 255) {
                    semiTransparentPixels++;
                }
            }
        }

        assertEquals(0, semiTransparentPixels);
    }
}
