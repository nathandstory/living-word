package com.livingword.client;

import com.livingword.LivingWord;
import com.livingword.audio.AudioCacheManager;
import com.livingword.audio.AudioManifest;
import com.livingword.audio.AudioPlaybackService;
import com.livingword.audio.CachedAudioDownloadService;
import com.livingword.audio.DownloadState;
import com.livingword.client.gui.BibleScreen;
import com.livingword.config.LivingWordConfig;
import com.livingword.network.payload.ListeningSessionSyncPayload;
import com.livingword.network.payload.PlaybackControlPayload;
import com.livingword.network.payload.TimestampCorrectionPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LivingWordClient {
    private static final ExecutorService AUDIO_DOWNLOAD_EXECUTOR = Executors.newFixedThreadPool(2, task -> {
        Thread thread = new Thread(task, "Living Word Audio Download");
        thread.setDaemon(true);
        return thread;
    });

    private static ListeningSessionSyncPayload activeSession;
    private static ClientAudioSessionController audioSessionController;

    private LivingWordClient() {
    }

    public static void openBibleScreen() {
        Minecraft.getInstance().setScreen(new BibleScreen());
    }

    public static void handleSessionSync(ListeningSessionSyncPayload payload) {
        activeSession = payload;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(
                Component.translatable(
                    "message.livingword.session.synced",
                    payload.translationId().toUpperCase(java.util.Locale.ROOT),
                    payload.bookId(),
                    payload.chapter(),
                    payload.participantCount()
                ).withStyle(ChatFormatting.GOLD),
                true
            );
        }
        controller().handleSessionSync(payload).thenAccept(LivingWordClient::reportDownloadState);
    }

    public static void handlePlaybackControl(PlaybackControlPayload payload) {
        controller().handlePlaybackControl(payload).thenAccept(LivingWordClient::reportDownloadState);
    }

    public static void handleTimestampCorrection(TimestampCorrectionPayload payload) {
        controller().handleTimestampCorrection(payload);
    }

    public static ListeningSessionSyncPayload activeSession() {
        return activeSession;
    }

    private static ClientAudioSessionController controller() {
        if (audioSessionController == null) {
            Minecraft minecraft = Minecraft.getInstance();
            Path cacheRoot = minecraft.gameDirectory.toPath().resolve("livingword").resolve("cache").resolve("audio");
            CachedAudioDownloadService downloadService = new CachedAudioDownloadService(new AudioCacheManager(cacheRoot), AUDIO_DOWNLOAD_EXECUTOR);
            audioSessionController = new ClientAudioSessionController(
                LivingWordClient::manifestFor,
                downloadService,
                AudioPlaybackService.noop(),
                LivingWordConfig.AUTOPLAY_JOINED_SESSIONS.get(),
                false,
                LivingWordConfig.SYNC_TOLERANCE_MILLIS.get()
            );
        }
        return audioSessionController;
    }

    private static AudioManifest manifestFor(String translationId) {
        String baseUrl = LivingWordConfig.CDN_BASE_URL.get();
        String normalized = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        return new AudioManifest(
            translationId + "-default",
            translationId,
            URI.create(normalized).resolve(translationId + "/"),
            Map.of()
        );
    }

    private static void reportDownloadState(DownloadState state) {
        if (state.status() == DownloadState.Status.CACHED || state.status() == DownloadState.Status.UNAVAILABLE) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                    Component.literal(state.message().isBlank() ? "Internet required for first playback." : state.message())
                        .withStyle(ChatFormatting.RED),
                    true
                );
            }
        });
        if (state.status() == DownloadState.Status.FAILED) {
            LivingWord.LOGGER.warn("Living Word audio download failed for {}: {}", state.chapterId(), state.message());
        }
    }
}
