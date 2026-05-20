package com.livingword.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Mp3AudioStreamContractTest {
    @Test
    void mp3PlaybackUsesBundledDecoderInsteadOfJavaSoundServiceLookup() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/Mp3AudioStream.java"));

        assertFalse(source.contains("AudioSystem"));
        assertFalse(source.contains("ByteBuffer.wrap"));
        assertTrue(source.contains("javazoom.jl.decoder"));
        assertTrue(source.contains("BufferUtils.createByteBuffer"));
    }
}
