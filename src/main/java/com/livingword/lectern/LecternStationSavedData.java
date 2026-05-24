package com.livingword.lectern;

import com.livingword.discs.ScriptureDiscPlaybackMode;
import com.livingword.discs.ScriptureDiscSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LecternStationSavedData extends SavedData {
    private static final String FILE_ID = "livingword_lectern_stations";
    private static final String STATIONS_KEY = "stations";
    private static final String X_KEY = "x";
    private static final String Y_KEY = "y";
    private static final String Z_KEY = "z";
    private static final String TRANSLATION_KEY = "translation";
    private static final String BOOK_KEY = "book";
    private static final String CHAPTER_KEY = "chapter";
    private static final String AUDIO_MANIFEST_KEY = "audio_manifest";
    private static final String PLAYBACK_MODE_KEY = "playback_mode";
    private static final String RESUME_POSITION_KEY = "resume_position";
    private static final String DISPLAY_ENTITY_KEY = "display_entity";
    private static final String DISPLAY_ENABLED_KEY = "display_enabled";

    private final Map<BlockPos, LecternListeningStation> stations = new ConcurrentHashMap<>();

    public static SavedData.Factory<LecternStationSavedData> factory() {
        return new SavedData.Factory<>(LecternStationSavedData::new, LecternStationSavedData::load);
    }

    public static String fileId() {
        return FILE_ID;
    }

    public Optional<LecternListeningStation> get(BlockPos sourcePos) {
        return Optional.ofNullable(stations.get(sourcePos.immutable()));
    }

    public void put(LecternListeningStation station) {
        stations.put(station.sourcePos().immutable(), station);
        setDirty();
    }

    public Optional<LecternListeningStation> remove(BlockPos sourcePos) {
        Optional<LecternListeningStation> removed = Optional.ofNullable(stations.remove(sourcePos.immutable()));
        if (removed.isPresent()) {
            setDirty();
        }
        return removed;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag stationTags = new ListTag();
        for (LecternListeningStation station : stations.values()) {
            stationTags.add(writeStation(station));
        }
        tag.put(STATIONS_KEY, stationTags);
        return tag;
    }

    public static LecternStationSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LecternStationSavedData data = new LecternStationSavedData();
        ListTag stationTags = tag.getList(STATIONS_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < stationTags.size(); index++) {
            readStation(stationTags.getCompound(index)).ifPresent(station -> data.stations.put(station.sourcePos().immutable(), station));
        }
        return data;
    }

    private static CompoundTag writeStation(LecternListeningStation station) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(X_KEY, station.sourcePos().getX());
        tag.putInt(Y_KEY, station.sourcePos().getY());
        tag.putInt(Z_KEY, station.sourcePos().getZ());
        tag.putString(TRANSLATION_KEY, station.selection().translationId());
        tag.putString(BOOK_KEY, station.selection().bookId());
        tag.putInt(CHAPTER_KEY, station.selection().chapter());
        tag.putString(AUDIO_MANIFEST_KEY, station.selection().audioManifestId());
        tag.putString(PLAYBACK_MODE_KEY, station.selection().playbackMode().name());
        tag.putLong(RESUME_POSITION_KEY, station.resumePositionMillis());
        tag.putBoolean(DISPLAY_ENABLED_KEY, station.displayEnabled());
        station.displayEntityId().ifPresent(uuid -> tag.putUUID(DISPLAY_ENTITY_KEY, uuid));
        return tag;
    }

    private static Optional<LecternListeningStation> readStation(CompoundTag tag) {
        String translationId = tag.getString(TRANSLATION_KEY);
        String bookId = tag.getString(BOOK_KEY);
        int chapter = tag.getInt(CHAPTER_KEY);
        if (translationId.isBlank() || bookId.isBlank() || chapter < 1) {
            return Optional.empty();
        }
        Optional<UUID> displayEntityId = tag.hasUUID(DISPLAY_ENTITY_KEY) ? Optional.of(tag.getUUID(DISPLAY_ENTITY_KEY)) : Optional.empty();
        boolean displayEnabled = !tag.contains(DISPLAY_ENABLED_KEY) || tag.getBoolean(DISPLAY_ENABLED_KEY);
        return Optional.of(new LecternListeningStation(
            new BlockPos(tag.getInt(X_KEY), tag.getInt(Y_KEY), tag.getInt(Z_KEY)),
            new ScriptureDiscSelection(
                translationId,
                bookId,
                chapter,
                tag.getString(AUDIO_MANIFEST_KEY),
                ScriptureDiscPlaybackMode.fromId(tag.getString(PLAYBACK_MODE_KEY))
            ),
            tag.getLong(RESUME_POSITION_KEY),
            displayEntityId,
            displayEnabled
        ));
    }
}
