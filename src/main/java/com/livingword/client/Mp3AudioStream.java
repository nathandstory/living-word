package com.livingword.client;

import net.minecraft.client.sounds.AudioStream;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.Obuffer;
import javazoom.jl.decoder.SampleBuffer;

import javax.sound.sampled.AudioFormat;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.lwjgl.BufferUtils;

public final class Mp3AudioStream implements AudioStream {
    private final Bitstream bitstream;
    private final Decoder decoder = new Decoder();
    private final ByteArrayOutputStream pendingPcm = new ByteArrayOutputStream(8192);
    private AudioFormat format;
    private boolean ended;

    public Mp3AudioStream(InputStream inputStream) throws IOException {
        this.bitstream = new Bitstream(new BufferedInputStream(inputStream));
        decodeNextFrame();
        if (format == null) {
            throw new IOException("MP3 stream did not contain audio frames.");
        }
    }

    @Override
    public synchronized AudioFormat getFormat() {
        return format;
    }

    @Override
    public synchronized ByteBuffer read(int size) throws IOException {
        if (size <= 0) {
            return BufferUtils.createByteBuffer(0);
        }
        while (!ended && pendingPcm.size() < size) {
            decodeNextFrame();
        }
        byte[] buffered = pendingPcm.toByteArray();
        int byteCount = Math.min(size, buffered.length);
        ByteBuffer result = BufferUtils.createByteBuffer(byteCount);
        result.put(buffered, 0, byteCount);
        result.flip();

        pendingPcm.reset();
        if (byteCount < buffered.length) {
            pendingPcm.write(buffered, byteCount, buffered.length - byteCount);
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        try {
            bitstream.close();
        } catch (BitstreamException exception) {
            throw new IOException("Failed to close MP3 audio stream", exception);
        }
    }

    private void decodeNextFrame() throws IOException {
        Header header = null;
        try {
            header = bitstream.readFrame();
            if (header == null) {
                ended = true;
                return;
            }
            Obuffer output = decoder.decodeFrame(header, bitstream);
            if (!(output instanceof SampleBuffer sampleBuffer)) {
                throw new IOException("Unsupported MP3 decoder output buffer.");
            }
            if (format == null) {
                format = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    sampleBuffer.getSampleFrequency(),
                    16,
                    sampleBuffer.getChannelCount(),
                    sampleBuffer.getChannelCount() * 2,
                    sampleBuffer.getSampleFrequency(),
                    false
                );
            }
            writeLittleEndianPcm(sampleBuffer, pendingPcm);
        } catch (BitstreamException | DecoderException exception) {
            throw new IOException("Failed to decode MP3 audio stream", exception);
        } finally {
            if (header != null) {
                bitstream.closeFrame();
            }
        }
    }

    private static void writeLittleEndianPcm(SampleBuffer sampleBuffer, ByteArrayOutputStream output) {
        short[] samples = sampleBuffer.getBuffer();
        int length = sampleBuffer.getBufferLength();
        for (int index = 0; index < length; index++) {
            short sample = samples[index];
            output.write(sample & 0xFF);
            output.write((sample >> 8) & 0xFF);
        }
    }
}
