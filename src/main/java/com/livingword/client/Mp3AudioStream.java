package com.livingword.client;

import net.minecraft.client.sounds.AudioStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public final class Mp3AudioStream implements AudioStream {
    private final AudioInputStream pcmStream;
    private final AudioFormat format;

    public Mp3AudioStream(InputStream inputStream) throws IOException {
        try {
            AudioInputStream encodedStream = AudioSystem.getAudioInputStream(new BufferedInputStream(inputStream));
            AudioFormat encodedFormat = encodedStream.getFormat();
            this.format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                encodedFormat.getSampleRate(),
                16,
                encodedFormat.getChannels(),
                encodedFormat.getChannels() * 2,
                encodedFormat.getSampleRate(),
                false
            );
            this.pcmStream = AudioSystem.getAudioInputStream(format, encodedStream);
        } catch (UnsupportedAudioFileException exception) {
            throw new IOException("Unsupported MP3 audio stream", exception);
        }
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public ByteBuffer read(int size) throws IOException {
        return ByteBuffer.wrap(pcmStream.readNBytes(size));
    }

    @Override
    public void close() throws IOException {
        pcmStream.close();
    }
}
