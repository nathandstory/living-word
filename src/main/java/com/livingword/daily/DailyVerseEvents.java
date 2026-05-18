package com.livingword.daily;

import com.livingword.bible.BibleDataManager;
import com.livingword.bible.BibleReference;
import com.livingword.bible.BibleResourceLoader;
import com.livingword.config.LivingWordConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class DailyVerseEvents {
    private static final DailyVerseSelector SELECTOR = new DailyVerseSelector(DailyVerseSelector.DEFAULT_POOL);
    private static final long SUNRISE_WINDOW_TICKS = 20L;

    private static BibleDataManager bibleDataManager;
    private static long lastAnnouncedDay = Long.MIN_VALUE;

    private DailyVerseEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(DailyVerseEvents::onServerTick);
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        if (!LivingWordConfig.DAILY_VERSE_ENABLED.get()) {
            return;
        }
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null || server.getPlayerList().getPlayerCount() == 0) {
            return;
        }

        long dayTime = overworld.getDayTime();
        long day = dayTime / 24_000L;
        long timeOfDay = dayTime % 24_000L;
        if (day == lastAnnouncedDay || timeOfDay > SUNRISE_WINDOW_TICKS) {
            return;
        }

        SELECTOR.select(dataManager(), java.time.LocalDate.ofEpochDay(day), overworld.getSeed()).ifPresent(verse -> announce(server, verse));
        lastAnnouncedDay = day;
    }

    private static BibleDataManager dataManager() {
        if (bibleDataManager == null) {
            bibleDataManager = new BibleDataManager();
            new BibleResourceLoader(bibleDataManager, DailyVerseEvents.class.getClassLoader()).reload();
        }
        return bibleDataManager;
    }

    private static void announce(MinecraftServer server, DailyVerse verse) {
        server.getPlayerList().broadcastSystemMessage(
            Component.translatable("message.livingword.daily_verse", formatReference(verse.reference()), verse.text())
                .withStyle(ChatFormatting.GOLD),
            false
        );
    }

    private static String formatReference(BibleReference reference) {
        return reference.bookId().replace('_', ' ') + " " + reference.chapter() + ":" + reference.verse();
    }
}
