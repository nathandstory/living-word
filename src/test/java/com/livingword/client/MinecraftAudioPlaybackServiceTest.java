package com.livingword.client;

import net.minecraft.client.sounds.AudioStream;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MinecraftAudioPlaybackServiceTest {
    @Test
    void invalidCachedAudioCompletesWithEmptyStreamInsteadOfCrashingSoundEngine() throws Exception {
        Path audio = Files.createTempFile("livingword-invalid-audio", ".mp3");
        Files.writeString(audio, "not an mp3");

        try (AudioStream stream = MinecraftAudioPlaybackService.openCachedAudioStream(audio, 0L).get(5, TimeUnit.SECONDS)) {
            assertEquals(44_100.0F, stream.getFormat().getSampleRate());
            java.nio.ByteBuffer fallbackBuffer = stream.read(4096);
            assertEquals(0, fallbackBuffer.remaining());
            assertTrue(fallbackBuffer.isDirect());
            assertFalse(Files.exists(audio));
        } finally {
            Files.deleteIfExists(audio);
        }
    }

    @Test
    void nonSpatialRelativePlaybackUsesListenerOriginSoNarrationIsCentered() {
        Vec3 playerPosition = new Vec3(120.0D, 64.0D, -240.0D);

        assertEquals(Vec3.ZERO, MinecraftAudioPlaybackService.playbackPositionFor(false, playerPosition));
        assertEquals(playerPosition, MinecraftAudioPlaybackService.playbackPositionFor(true, playerPosition));
    }
}
