package com.livingword.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class LivingWordConfig {
    public static final ModConfigSpec COMMON_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;

    public static final ModConfigSpec.ConfigValue<String> CDN_BASE_URL;
    public static final ModConfigSpec.IntValue CACHE_LIMIT_MEGABYTES;
    public static final ModConfigSpec.IntValue SYNC_TOLERANCE_MILLIS;
    public static final ModConfigSpec.BooleanValue AUTOPLAY_JOINED_SESSIONS;
    public static final ModConfigSpec.BooleanValue DAILY_VERSE_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> VERSE_DISPLAY_STYLE;

    public static final ModConfigSpec.BooleanValue SUBTITLE_ENABLED;
    public static final ModConfigSpec.DoubleValue NARRATION_VOLUME;
    public static final ModConfigSpec.BooleanValue AUDIO_STATUS_MESSAGES;
    public static final ModConfigSpec.IntValue SEARCH_RESULT_LIMIT;

    static {
        ModConfigSpec.Builder common = new ModConfigSpec.Builder();
        common.push("audio");
        CDN_BASE_URL = common
            .comment("Base URL used for audio manifests and chapter downloads.")
            .define("cdnBaseUrl", "https://cdn.example.com/livingword");
        CACHE_LIMIT_MEGABYTES = common
            .comment("Maximum local audio cache size in megabytes.")
            .defineInRange("cacheLimitMegabytes", 1024, 64, 65536);
        common.pop();

        common.push("sync");
        SYNC_TOLERANCE_MILLIS = common
            .comment("Allowed multiplayer listening drift before a correction packet is applied.")
            .defineInRange("syncToleranceMillis", 250, 25, 5000);
        AUTOPLAY_JOINED_SESSIONS = common
            .comment("Whether clients should begin playback automatically after joining a listening session.")
            .define("autoplayJoinedSessions", true);
        common.pop();

        common.push("dailyVerse");
        DAILY_VERSE_ENABLED = common
            .comment("Whether servers announce a daily verse at sunrise.")
            .define("enabled", true);
        common.pop();

        common.push("display");
        VERSE_DISPLAY_STYLE = common
            .comment("Default shared verse display style. Supported values will expand in later builds.")
            .define("verseDisplayStyle", "actionbar");
        common.pop();
        COMMON_SPEC = common.build();

        ModConfigSpec.Builder client = new ModConfigSpec.Builder();
        client.push("subtitles");
        SUBTITLE_ENABLED = client
            .comment("Whether narrated chapter playback should show verse subtitles.")
            .define("enabled", true);
        client.pop();

        client.push("audio");
        NARRATION_VOLUME = client
            .comment("Client-side narration volume multiplier.")
            .defineInRange("narrationVolume", 1.0D, 0.0D, 1.0D);
        AUDIO_STATUS_MESSAGES = client
            .comment("Whether the client should show actionbar messages when Bible, preview, and shared listening audio hand off to each other.")
            .define("audioStatusMessages", true);
        client.pop();

        client.push("bible");
        SEARCH_RESULT_LIMIT = client
            .comment("Maximum number of verses shown in the Bible search results list.")
            .defineInRange("searchResultLimit", 500, 10, 1000);
        client.pop();
        CLIENT_SPEC = client.build();
    }

    private LivingWordConfig() {
    }

    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }
}
