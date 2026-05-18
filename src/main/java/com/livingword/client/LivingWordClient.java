package com.livingword.client;

import com.livingword.client.gui.BibleScreen;
import com.livingword.network.payload.ListeningSessionSyncPayload;
import com.livingword.network.payload.PlaybackControlPayload;
import com.livingword.network.payload.TimestampCorrectionPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class LivingWordClient {
    private static ListeningSessionSyncPayload activeSession;

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
    }

    public static void handlePlaybackControl(PlaybackControlPayload payload) {
        // The concrete streamed audio source will consume this once client playback is wired in.
    }

    public static void handleTimestampCorrection(TimestampCorrectionPayload payload) {
        // The concrete streamed audio source will consume this once client playback is wired in.
    }

    public static ListeningSessionSyncPayload activeSession() {
        return activeSession;
    }
}
