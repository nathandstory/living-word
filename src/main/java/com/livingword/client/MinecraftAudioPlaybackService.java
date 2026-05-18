package com.livingword.client;

import com.livingword.LivingWord;
import com.livingword.audio.AudioCacheManager;
import com.livingword.audio.AudioChapterId;
import com.livingword.audio.AudioPlaybackService;
import com.livingword.config.LivingWordConfig;
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

import javax.annotation.Nullable;
import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

public final class MinecraftAudioPlaybackService implements AudioPlaybackService {
    private final AudioCacheManager cacheManager;
    private final Map<AudioChapterId, ActiveSound> activeSounds = new ConcurrentHashMap<>();

    public MinecraftAudioPlaybackService(AudioCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void play(AudioChapterId chapterId, long positionMillis, boolean spatial) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            ActiveSound previous = activeSounds.remove(chapterId);
            if (previous != null) {
                minecraft.getSoundManager().stop(previous.sound());
            }
            Path path = cacheManager.chapterAudioPath(chapterId);
            if (!Files.isRegularFile(path)) {
                return;
            }
            CachedChapterSoundInstance sound = new CachedChapterSoundInstance(chapterId, path, Math.max(0L, positionMillis), spatial, playbackPosition(minecraft));
            activeSounds.put(chapterId, new ActiveSound(sound, spatial));
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
        play(chapterId, positionMillis, activeSound != null && activeSound.spatial());
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

    private static Vec3 playbackPosition(Minecraft minecraft) {
        return minecraft.player == null ? Vec3.ZERO : minecraft.player.position();
    }

    private record ActiveSound(CachedChapterSoundInstance sound, boolean spatial) {
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
            return CompletableFuture.supplyAsync(() -> {
                try {
                    InputStream inputStream = Files.newInputStream(path);
                    JOrbisAudioStream stream = new JOrbisAudioStream(inputStream);
                    skipTo(stream, positionMillis);
                    return stream;
                } catch (IOException exception) {
                    throw new CompletionException(exception);
                }
            }, Util.nonCriticalIoPool());
        }

        private static void skipTo(AudioStream stream, long positionMillis) throws IOException {
            if (positionMillis <= 0L) {
                return;
            }
            AudioFormat format = stream.getFormat();
            long bytesToSkip = (long) ((positionMillis / 1000.0D) * format.getFrameRate() * format.getFrameSize());
            while (bytesToSkip > 0L) {
                ByteBuffer skipped = stream.read((int) Math.min(bytesToSkip, 65_536L));
                int skippedBytes = skipped.remaining();
                if (skippedBytes <= 0) {
                    return;
                }
                bytesToSkip -= skippedBytes;
            }
        }

        @Override
        public String toString() {
            return "CachedChapterSoundInstance[" + chapterId + "]";
        }
    }
}
