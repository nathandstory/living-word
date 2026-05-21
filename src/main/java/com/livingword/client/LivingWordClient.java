package com.livingword.client;

import com.livingword.LivingWord;
import com.livingword.audio.AudioCacheManager;
import com.livingword.audio.AudioChapterId;
import com.livingword.audio.AudioManifest;
import com.livingword.audio.AudioManifestRepository;
import com.livingword.audio.CachedAudioDownloadService;
import com.livingword.audio.DownloadState;
import com.livingword.client.gui.BibleScreen;
import com.livingword.client.gui.ScriptureDiscSelectionScreen;
import com.livingword.config.LivingWordConfig;
import com.livingword.discs.ScriptureDiscSelection;
import com.livingword.network.payload.ChapterFinishedPayload;
import com.livingword.network.payload.ConfigureScriptureDiscPayload;
import com.livingword.network.payload.ListeningSessionSyncPayload;
import com.livingword.network.payload.PlaybackControlPayload;
import com.livingword.network.payload.TimestampCorrectionPayload;
import com.livingword.sync.PlaybackState;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    private static ClientAudioSessionController bibleAudioController;
    private static ClientAudioSessionController scriptureDiscPreviewController;
    private static ListeningSessionSyncPayload localBibleSession;
    private static AudioManifestRepository audioManifestRepository;
    private static final Map<AudioChapterId, Long> LOCAL_RESUME_POSITIONS = new ConcurrentHashMap<>();
    private static final long BIBLE_OPEN_ANIMATION_DELAY_MILLIS = 180L;
    private static final long BIBLE_OPEN_HELD_MILLIS = 650L;
    private static long bibleOpenUntilMillis;
    private static long pendingBibleOpenAtMillis;

    private LivingWordClient() {
    }

    public static void openBibleScreen() {
        pendingBibleOpenAtMillis = 0L;
        Minecraft.getInstance().setScreen(new BibleScreen());
    }

    public static boolean isBibleScreenOpen() {
        return Minecraft.getInstance().screen instanceof BibleScreen;
    }

    public static void beginBibleOpenAnimation() {
        long now = Util.getMillis();
        bibleOpenUntilMillis = now + BIBLE_OPEN_HELD_MILLIS;
        pendingBibleOpenAtMillis = now + BIBLE_OPEN_ANIMATION_DELAY_MILLIS;
    }

    public static void tickBibleOpenAnimation() {
        if (pendingBibleOpenAtMillis == 0L) {
            return;
        }
        if (Util.getMillis() >= pendingBibleOpenAtMillis) {
            openBibleScreen();
        }
    }

    public static void tickAudioSessions() {
        if (audioSessionController == null) {
            return;
        }
        audioSessionController.drainCompletedPlayback().ifPresent(completed -> {
            if (activeSession != null && activeSession.sessionId().equals(completed.sessionId())) {
                activeSession = null;
            }
            PacketDistributor.sendToServer(new ChapterFinishedPayload(completed.sessionId()));
        });
    }

    public static boolean isBibleOpenInHand() {
        return isBibleScreenOpen() || Util.getMillis() < bibleOpenUntilMillis;
    }

    public static void handleSessionSync(ListeningSessionSyncPayload payload) {
        if (payload.state() != PlaybackState.STOPPED) {
            stopScriptureDiscPreview();
        }
        activeSession = payload.state() == PlaybackState.STOPPED ? null : payload;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && payload.state() != PlaybackState.STOPPED) {
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

    public static void playLocalChapter(String translationId, String bookId, int chapter) {
        AudioChapterId chapterId = new AudioChapterId(translationId, bookId, chapter);
        playLocalChapter(translationId, bookId, chapter, "default", LOCAL_RESUME_POSITIONS.getOrDefault(chapterId, 0L));
    }

    public static void playLocalChapter(String translationId, String bookId, int chapter, long positionMillis) {
        playLocalChapter(translationId, bookId, chapter, "default", positionMillis);
    }

    public static void playLocalChapter(String translationId, String bookId, int chapter, String audioManifestId, long positionMillis) {
        stopScriptureDiscPreview();
        localBibleSession = new ListeningSessionSyncPayload(
            UUID.randomUUID(),
            translationId,
            bookId,
            chapter,
            audioManifestId,
            PlaybackState.PLAYING,
            Math.max(0L, positionMillis),
            System.currentTimeMillis(),
            1
        );
        bibleAudioController().handleSessionSync(localBibleSession).thenAccept(LivingWordClient::reportDownloadState);
    }

    public static void previewScriptureDiscChapter(String translationId, String bookId, int chapter, String audioManifestId) {
        scriptureDiscPreviewController().handleSessionSync(new ListeningSessionSyncPayload(
            UUID.randomUUID(),
            translationId,
            bookId,
            chapter,
            audioManifestId,
            PlaybackState.PLAYING,
            0L,
            System.currentTimeMillis(),
            1
        )).thenAccept(LivingWordClient::reportDownloadState);
    }

    public static void stopScriptureDiscPreview() {
        if (scriptureDiscPreviewController != null) {
            scriptureDiscPreviewController.stopActive();
        }
    }

    public static void toggleLocalChapter(String translationId, String bookId, int chapter) {
        if (isLocalBibleChapterActive(translationId, bookId, chapter)) {
            AudioChapterId chapterId = new AudioChapterId(translationId, bookId, chapter);
            long positionMillis = bibleAudioController().pauseActive();
            LOCAL_RESUME_POSITIONS.put(chapterId, positionMillis);
            localBibleSession = null;
            return;
        }
        playLocalChapter(translationId, bookId, chapter);
    }

    public static void stopLocalPlayback() {
        long positionMillis = bibleAudioController().stopActive();
        if (localBibleSession != null) {
            LOCAL_RESUME_POSITIONS.put(
                new AudioChapterId(localBibleSession.translationId(), localBibleSession.bookId(), localBibleSession.chapter()),
                positionMillis
            );
        }
        localBibleSession = null;
    }

    public static void openScriptureDiscSelection(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new ScriptureDiscSelectionScreen(hand));
    }

    public static void configureScriptureDisc(InteractionHand hand, ScriptureDiscSelection selection) {
        PacketDistributor.sendToServer(new ConfigureScriptureDiscPayload(
            hand,
            selection.translationId(),
            selection.bookId(),
            selection.chapter(),
            selection.audioManifestId(),
            selection.playbackMode()
        ));
    }

    public static ListeningSessionSyncPayload activeSession() {
        return activeSession;
    }

    private static ClientAudioSessionController controller() {
        if (audioSessionController == null) {
            Minecraft minecraft = Minecraft.getInstance();
            Path cacheRoot = minecraft.gameDirectory.toPath().resolve("livingword").resolve("cache").resolve("audio");
            AudioCacheManager cacheManager = new AudioCacheManager(cacheRoot);
            CachedAudioDownloadService downloadService = new CachedAudioDownloadService(cacheManager, AUDIO_DOWNLOAD_EXECUTOR);
            audioSessionController = new ClientAudioSessionController(
                LivingWordClient::manifestFor,
                downloadService,
                new MinecraftAudioPlaybackService(cacheManager),
                LivingWordConfig.AUTOPLAY_JOINED_SESSIONS.get(),
                false,
                LivingWordConfig.SYNC_TOLERANCE_MILLIS.get()
            );
        }
        return audioSessionController;
    }

    private static ClientAudioSessionController bibleAudioController() {
        if (bibleAudioController == null) {
            Minecraft minecraft = Minecraft.getInstance();
            Path cacheRoot = minecraft.gameDirectory.toPath().resolve("livingword").resolve("cache").resolve("audio");
            AudioCacheManager cacheManager = new AudioCacheManager(cacheRoot);
            CachedAudioDownloadService downloadService = new CachedAudioDownloadService(cacheManager, AUDIO_DOWNLOAD_EXECUTOR);
            bibleAudioController = new ClientAudioSessionController(
                LivingWordClient::manifestFor,
                downloadService,
                new MinecraftAudioPlaybackService(cacheManager),
                true,
                false,
                LivingWordConfig.SYNC_TOLERANCE_MILLIS.get()
            );
        }
        return bibleAudioController;
    }

    private static ClientAudioSessionController scriptureDiscPreviewController() {
        if (scriptureDiscPreviewController == null) {
            Minecraft minecraft = Minecraft.getInstance();
            Path cacheRoot = minecraft.gameDirectory.toPath().resolve("livingword").resolve("cache").resolve("audio");
            AudioCacheManager cacheManager = new AudioCacheManager(cacheRoot);
            CachedAudioDownloadService downloadService = new CachedAudioDownloadService(cacheManager, AUDIO_DOWNLOAD_EXECUTOR);
            scriptureDiscPreviewController = ClientAudioSessionController.preview(
                LivingWordClient::manifestFor,
                downloadService,
                new MinecraftAudioPlaybackService(cacheManager),
                LivingWordConfig.SYNC_TOLERANCE_MILLIS.get()
            );
        }
        return scriptureDiscPreviewController;
    }

    private static AudioManifest manifestFor(String translationId, String audioManifestId) {
        String baseUrl = LivingWordConfig.CDN_BASE_URL.get();
        String normalized = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        return audioManifestRepository().manifestOrFallback(translationId, audioManifestId, URI.create(normalized).resolve(translationId + "/"));
    }

    private static AudioManifestRepository audioManifestRepository() {
        if (audioManifestRepository == null) {
            audioManifestRepository = new AudioManifestRepository(LivingWordClient.class.getClassLoader());
        }
        return audioManifestRepository;
    }

    private static boolean isActiveChapter(String translationId, String bookId, int chapter) {
        return activeSession != null
            && activeSession.state() == PlaybackState.PLAYING
            && activeSession.translationId().equals(translationId)
            && activeSession.bookId().equals(bookId)
            && activeSession.chapter() == chapter;
    }

    private static boolean isLocalBibleChapterActive(String translationId, String bookId, int chapter) {
        return localBibleSession != null
            && localBibleSession.state() == PlaybackState.PLAYING
            && localBibleSession.translationId().equals(translationId)
            && localBibleSession.bookId().equals(bookId)
            && localBibleSession.chapter() == chapter;
    }

    private static void reportDownloadState(DownloadState state) {
        if (state.status() == DownloadState.Status.CACHED || state.status() == DownloadState.Status.UNAVAILABLE) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                    Component.literal(playerFacingAudioFailure(state))
                        .withStyle(ChatFormatting.RED),
                    true
                );
            }
        });
        if (state.status() == DownloadState.Status.FAILED) {
            LivingWord.LOGGER.warn("Living Word audio download failed for {}: {}", state.chapterId(), state.message());
        }
    }

    private static String playerFacingAudioFailure(DownloadState state) {
        return switch (state.status()) {
            case HASH_MISMATCH -> "Downloaded chapter audio was corrupted. Trying again may repair it.";
            case FAILED -> "Unable to download chapter audio. Internet is required for first playback.";
            default -> state.message().isBlank() ? "Internet required for first playback." : state.message();
        };
    }
}
