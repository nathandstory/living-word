package com.livingword.client;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class Mp3AudioStreamSmokeTest {
    @Test
    void decodesRealRemoteMp3WhenSmokeUrlIsProvided() throws Exception {
        String smokeUrl = System.getProperty("livingword.audioSmokeUrl", "");
        assumeTrue(!smokeUrl.isBlank(), "Set -Dlivingword.audioSmokeUrl to run the remote MP3 smoke test.");

        try (InputStream inputStream = URI.create(smokeUrl).toURL().openStream();
             Mp3AudioStream stream = new Mp3AudioStream(inputStream)) {
            assertTrue(stream.getFormat().getSampleRate() > 0.0F);
            java.nio.ByteBuffer buffer = stream.read(4096);
            assertTrue(buffer.remaining() > 0);
            assertTrue(buffer.isDirect());
        }
    }
}
