package com.livingword.client;

import com.livingword.LivingWord;
import com.livingword.audio.AudioCacheManager;
import com.livingword.audio.AudioChapterId;
import com.livingword.audio.AudioPlaybackService;
import com.livingword.config.LivingWordConfig;
import com.livingword.sync.AudioSourcePosition;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.BufferUtils;

import javax.annotation.Nullable;
import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class MinecraftAudioPlaybackService implements AudioPlaybackService {
    private final AudioCacheManager cacheManager;
    private final Map<AudioChapterId, ActiveSound> activeSounds = new ConcurrentHashMap<>();

    public MinecraftAudioPlaybackService(AudioCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void play(AudioChapterId chapterId, long positionMillis, boolean spatial) {
        play(chapterId, positionMillis, spatial, "ogg");
    }

    @Override
    public void play(AudioChapterId chapterId, long positionMillis, boolean spatial, String fileExtension) {
        play(chapterId, positionMillis, spatial, fileExtension, Optional.empty());
    }

    @Override
    public void play(AudioChapterId chapterId, long positionMillis, boolean spatial, String fileExtension, Optional<AudioSourcePosition> sourcePosition) {
        Minecraft minecraft = Minecraft.getInstance();
        String normalizedExtension = normalizeAudioExtension(fileExtension);
        Optional<AudioSourcePosition> normalizedSourcePosition = sourcePosition == null ? Optional.empty() : sourcePosition;
        minecraft.execute(() -> {
            stopNow(chapterId, minecraft.getSoundManager());
            Path path = cacheManager.chapterAudioPath(chapterId, normalizedExtension);
            if (!Files.isRegularFile(path)) {
                return;
            }
            CachedChapterSoundInstance sound = new CachedChapterSoundInstance(
                chapterId,
                path,
                Math.max(0L, positionMillis),
                spatial,
                playbackPosition(minecraft, spatial, normalizedSourcePosition)
            );
            activeSounds.put(chapterId, new ActiveSound(sound, spatial, normalizedExtension, normalizedSourcePosition));
            minecraft.getSoundManager().play(sound);
        });
    }

    @Override
    public void pause(AudioChapterId chapterId) {
        stop(chapterId);
    }

    @Override
    public void seek(AudioChapterId chapterId, long positionMillis) {
        ActiveSound activeSound = activeSounds.get(chapterId);
        play(
            chapterId,
            positionMillis,
            activeSound != null && activeSound.spatial(),
            activeSound == null ? "ogg" : activeSound.fileExtension(),
            activeSound == null ? Optional.empty() : activeSound.sourcePosition()
        );
    }

    @Override
    public void seek(AudioChapterId chapterId, long positionMillis, String fileExtension) {
        ActiveSound activeSound = activeSounds.get(chapterId);
        play(chapterId, positionMillis, activeSound != null && activeSound.spatial(), fileExtension, activeSound == null ? Optional.empty() : activeSound.sourcePosition());
    }

    @Override
    public void seek(AudioChapterId chapterId, long positionMillis, String fileExtension, Optional<AudioSourcePosition> sourcePosition) {
        ActiveSound activeSound = activeSounds.get(chapterId);
        Optional<AudioSourcePosition> nextSourcePosition = sourcePosition == null ? Optional.empty() : sourcePosition;
        play(chapterId, positionMillis, activeSound != null && activeSound.spatial() || nextSourcePosition.isPresent(), fileExtension, nextSourcePosition);
    }

    @Override
    public void stop(AudioChapterId chapterId) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            ActiveSound activeSound = activeSounds.remove(chapterId);
            if (activeSound != null) {
                minecraft.getSoundManager().stop(activeSound.sound());
            }
        });
    }

    @Override
    public void stopAll() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> stopAllNow(minecraft.getSoundManager()));
    }

    private void stopAllNow(SoundManager soundManager) {
        for (ActiveSound activeSound : activeSounds.values()) {
            soundManager.stop(activeSound.sound());
        }
        activeSounds.clear();
    }

    private void stopNow(AudioChapterId chapterId, SoundManager soundManager) {
        ActiveSound activeSound = activeSounds.remove(chapterId);
        if (activeSound != null) {
            soundManager.stop(activeSound.sound());
        }
    }

    private static Vec3 playbackPosition(Minecraft minecraft, boolean spatial) {
        return playbackPosition(minecraft, spatial, Optional.empty());
    }

    private static Vec3 playbackPosition(Minecraft minecraft, boolean spatial, Optional<AudioSourcePosition> sourcePosition) {
        return playbackPositionFor(spatial, minecraft.player == null ? Vec3.ZERO : minecraft.player.position(), sourcePosition);
    }

    static Vec3 playbackPositionFor(boolean spatial, Vec3 playerPosition) {
        return playbackPositionFor(spatial, playerPosition, Optional.empty());
    }

    static Vec3 playbackPositionFor(boolean spatial, Vec3 playerPosition, Optional<AudioSourcePosition> sourcePosition) {
        if (!spatial) {
            return Vec3.ZERO;
        }
        if (sourcePosition != null && sourcePosition.isPresent()) {
            AudioSourcePosition source = sourcePosition.orElseThrow();
            return new Vec3(source.x(), source.y(), source.z());
        }
        return playerPosition == null ? Vec3.ZERO : playerPosition;
    }

    private static String normalizeAudioExtension(String fileExtension) {
        if (fileExtension == null || fileExtension.isBlank()) {
            return "ogg";
        }
        String normalized = fileExtension.startsWith(".") ? fileExtension.substring(1) : fileExtension;
        normalized = normalized.toLowerCase(java.util.Locale.ROOT);
        return normalized.matches("[a-z0-9]+") ? normalized : "ogg";
    }

    private record ActiveSound(CachedChapterSoundInstance sound, boolean spatial, String fileExtension, Optional<AudioSourcePosition> sourcePosition) {
    }

    private static final class CachedChapterSoundInstance implements SoundInstance {
        private final AudioChapterId chapterId;
        private final Path path;
        private final long positionMillis;
        private final boolean spatial;
        private final Vec3 position;
        private final ResourceLocation location;
        private final Sound sound;
        private final WeighedSoundEvents resolved;

        private CachedChapterSoundInstance(AudioChapterId chapterId, Path path, long positionMillis, boolean spatial, Vec3 position) {
            this.chapterId = chapterId;
            this.path = path;
            this.positionMillis = positionMillis;
            this.spatial = spatial;
            this.position = position;
            this.location = ResourceLocation.fromNamespaceAndPath(
                LivingWord.MOD_ID,
                "chapter/%s/%s/%03d".formatted(chapterId.translationId(), chapterId.bookId(), chapterId.chapter())
            );
            this.sound = new Sound(location, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, true, false, 32);
            this.resolved = new WeighedSoundEvents(location, "subtitles.livingword.narration");
            this.resolved.addSound(sound);
        }

        @Override
        public ResourceLocation getLocation() {
            return location;
        }

        @Override
        public @Nullable WeighedSoundEvents resolve(SoundManager manager) {
            return resolved;
        }

        @Override
        public Sound getSound() {
            return sound;
        }

        @Override
        public SoundSource getSource() {
            return SoundSource.RECORDS;
        }

        @Override
        public boolean isLooping() {
            return false;
        }

        @Override
        public boolean isRelative() {
            return !spatial;
        }

        @Override
        public int getDelay() {
            return 0;
        }

        @Override
        public float getVolume() {
            return LivingWordConfig.NARRATION_VOLUME.get().floatValue();
        }

        @Override
        public float getPitch() {
            return 1.0F;
        }

        @Override
        public double getX() {
            return position.x();
        }

        @Override
        public double getY() {
            return position.y();
        }

        @Override
        public double getZ() {
            return position.z();
        }

        @Override
        public Attenuation getAttenuation() {
            return spatial ? Attenuation.LINEAR : Attenuation.NONE;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
            return openCachedAudioStream(path, positionMillis);
        }

        @Override
        public String toString() {
            return "CachedChapterSoundInstance[" + chapterId + "]";
        }
    }

    static CompletableFuture<AudioStream> openCachedAudioStream(Path path, long positionMillis) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return openAudioStream(path, positionMillis);
            } catch (IOException exception) {
                LivingWord.LOGGER.warn("Unable to open cached Living Word audio {}", path, exception);
                deleteInvalidCachedAudio(path);
                return new EmptyAudioStream();
            }
        }, Util.nonCriticalIoPool());
    }

    private static AudioStream openAudioStream(Path path, long positionMillis) throws IOException {
        InputStream inputStream = Files.newInputStream(path);
        AudioStream stream;
        try {
            stream = isMp3(path) ? new Mp3AudioStream(inputStream) : new JOrbisAudioStream(inputStream);
        } catch (IOException exception) {
            inputStream.close();
            throw exception;
        }

        try {
            skipTo(stream, positionMillis);
            return stream;
        } catch (IOException exception) {
            try {
                stream.close();
            } catch (IOException suppressed) {
                exception.addSuppressed(suppressed);
            }
            throw exception;
        }
    }

    private static boolean isMp3(Path path) {
        return path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".mp3");
    }

    private static void deleteInvalidCachedAudio(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            LivingWord.LOGGER.warn("Unable to delete invalid Living Word cached audio {}", path, exception);
        }
    }

    private static void skipTo(AudioStream stream, long positionMillis) throws IOException {
        if (positionMillis <= 0L) {
            return;
        }
        AudioFormat format = stream.getFormat();
        int frameSize = frameSize(format);
        long bytesToSkip = frameAlignedBytesToSkip(format, positionMillis, frameSize);
        int maxReadBytes = frameAlignedReadLimit(frameSize);
        while (bytesToSkip > 0L) {
            ByteBuffer skipped = stream.read((int) Math.min(bytesToSkip, maxReadBytes));
            int skippedBytes = skipped.remaining();
            if (skippedBytes <= 0) {
                return;
            }
            bytesToSkip -= skippedBytes;
        }
    }

    private static long frameAlignedBytesToSkip(AudioFormat format, long positionMillis, int frameSize) {
        long rawBytes = (long) ((positionMillis / 1000.0D) * format.getFrameRate() * frameSize);
        return rawBytes - Math.floorMod(rawBytes, frameSize);
    }

    private static int frameAlignedReadLimit(int frameSize) {
        int aligned = 65_536 - Math.floorMod(65_536, frameSize);
        return aligned > 0 ? aligned : frameSize;
    }

    private static int frameSize(AudioFormat format) {
        if (format.getFrameSize() > 0) {
            return format.getFrameSize();
        }
        int bytesPerSample = Math.max(1, format.getSampleSizeInBits() / 8);
        return Math.max(1, bytesPerSample * Math.max(1, format.getChannels()));
    }

    private static final class EmptyAudioStream implements AudioStream {
        private static final AudioFormat FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            44_100.0F,
            16,
            2,
            4,
            44_100.0F,
            false
        );

        @Override
        public AudioFormat getFormat() {
            return FORMAT;
        }

        @Override
        public ByteBuffer read(int size) {
            return BufferUtils.createByteBuffer(0);
        }

        @Override
        public void close() {
        }
    }
}
