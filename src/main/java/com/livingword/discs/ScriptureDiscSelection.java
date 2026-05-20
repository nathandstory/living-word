package com.livingword.discs;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public record ScriptureDiscSelection(String translationId, String bookId, int chapter, String audioManifestId, ScriptureDiscPlaybackMode playbackMode) {
    private static final String ROOT_KEY = "livingword";
    private static final String TRANSLATION_KEY = "scripture_translation";
    private static final String BOOK_KEY = "scripture_book";
    private static final String CHAPTER_KEY = "scripture_chapter";
    private static final String AUDIO_MANIFEST_KEY = "scripture_audio_manifest";
    private static final String PLAYBACK_MODE_KEY = "scripture_playback_mode";

    public ScriptureDiscSelection(String translationId, String bookId, int chapter) {
        this(translationId, bookId, chapter, "default", ScriptureDiscPlaybackMode.SINGLE_CHAPTER);
    }

    public ScriptureDiscSelection {
        if (translationId == null || translationId.isBlank()) {
            throw new IllegalArgumentException("translationId is required");
        }
        if (bookId == null || bookId.isBlank()) {
            throw new IllegalArgumentException("bookId is required");
        }
        if (chapter < 1) {
            throw new IllegalArgumentException("chapter must be positive");
        }
        if (audioManifestId == null || audioManifestId.isBlank()) {
            audioManifestId = "default";
        }
        if (playbackMode == null) {
            playbackMode = ScriptureDiscPlaybackMode.SINGLE_CHAPTER;
        }
    }

    public static ScriptureDiscSelection defaults() {
        return new ScriptureDiscSelection("bsb", "john", 1, "default", ScriptureDiscPlaybackMode.SINGLE_CHAPTER);
    }

    public static ScriptureDiscSelection from(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return fromRootTag(root);
    }

    static ScriptureDiscSelection fromRootTag(CompoundTag root) {
        if (!root.contains(ROOT_KEY, 10)) {
            return defaults();
        }
        CompoundTag data = root.getCompound(ROOT_KEY);
        String translationId = data.getString(TRANSLATION_KEY);
        String bookId = data.getString(BOOK_KEY);
        int chapter = data.getInt(CHAPTER_KEY);
        String audioManifestId = data.getString(AUDIO_MANIFEST_KEY);
        ScriptureDiscPlaybackMode playbackMode = ScriptureDiscPlaybackMode.fromId(data.getString(PLAYBACK_MODE_KEY));
        if (translationId.isBlank() || bookId.isBlank() || chapter < 1) {
            return defaults();
        }
        return new ScriptureDiscSelection(translationId, bookId, chapter, audioManifestId, playbackMode);
    }

    public static void write(ItemStack stack, ScriptureDiscSelection selection) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> writeToRootTag(root, selection));
    }

    static void writeToRootTag(CompoundTag root, ScriptureDiscSelection selection) {
        CompoundTag data = root.getCompound(ROOT_KEY);
        data.putString(TRANSLATION_KEY, selection.translationId());
        data.putString(BOOK_KEY, selection.bookId());
        data.putInt(CHAPTER_KEY, selection.chapter());
        data.putString(AUDIO_MANIFEST_KEY, selection.audioManifestId());
        data.putString(PLAYBACK_MODE_KEY, selection.playbackMode().name());
        root.put(ROOT_KEY, data);
    }
}
