package com.livingword.lectern;

import com.livingword.bible.BibleDataManager;
import com.livingword.bible.BibleReference;
import com.livingword.bible.BibleResourceLoader;
import com.livingword.bible.ChapterData;
import com.livingword.audio.AudioChapterId;
import com.livingword.audio.AudioTimingRepository;
import com.livingword.audio.VerseTimestampMap;
import com.livingword.discs.ScriptureDiscEvents;
import com.livingword.discs.ScriptureDiscPlaybackSequencer;
import com.livingword.discs.ScriptureDiscSelection;
import com.livingword.items.LivingWordItems;
import com.livingword.network.LivingWordNetwork;
import com.livingword.network.payload.ConfigureLecternStationPayload;
import com.livingword.network.payload.OpenLecternStationPayload;
import com.livingword.sync.ListeningSession;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class LecternEvents {
    private static final double LECTERN_LISTENING_RADIUS = 48.0D;
    private static final double DISPLAY_LINE_SPACING = 0.27D;
    private static final int DISPLAY_UPDATE_INTERVAL_TICKS = 5;
    private static final String DISPLAY_TAG = "livingword_lectern_display";
    private static final String DISPLAY_LINE_TAG_PREFIX = "livingword_line_";
    private static final LecternListeningStationRegistry STATIONS = new LecternListeningStationRegistry();
    private static BibleDataManager bibleDataManager;
    private static AudioTimingRepository audioTimingRepository;
    private static int displayUpdateTick;

    private LecternEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(LecternEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(LecternEvents::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(LecternEvents::onServerTick);
    }

    public static void configureStation(ServerPlayer player, ConfigureLecternStationPayload payload) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = payload.sourcePos();
        if (!level.getBlockState(pos).is(Blocks.LECTERN) || !hasLivingWordBible(level, pos)) {
            player.displayClientMessage(Component.translatable("message.livingword.lectern.no_bible"), true);
            return;
        }

        ScriptureDiscSelection selection = normalizeSelection(payload.selection());
        LecternStationSavedData savedData = stationData(level);
        LecternListeningStation existing = savedData.get(pos).orElseGet(() -> new LecternListeningStation(pos, selection));
        LecternListeningStation station = existing.withSelection(selection);

        switch (payload.action()) {
            case SAVE -> {
                station = saveStationWithDisplay(level, savedData, station);
                STATIONS.remember(level.dimension().location(), station);
                player.displayClientMessage(Component.translatable("message.livingword.lectern.configured", formatSelection(selection)), true);
            }
            case START, PLAY -> startStation(player, level, savedData, station);
            case STOP -> stopStation(player, level, savedData, station);
            case PAUSE -> pauseStation(player, level, savedData, station);
            case RESET -> resetStation(player, level, savedData, station);
            case TOGGLE_DISPLAY -> toggleDisplay(player, level, savedData, station);
        }
    }

    public static boolean completeLecternChapter(ServerPlayer player, UUID sessionId) {
        return STATIONS.findPlaying(sessionId)
            .map(snapshot -> {
                ServerLevel level = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, snapshot.dimension()));
                if (level == null) {
                    return false;
                }
                if (!level.getBlockState(snapshot.sourcePos()).is(Blocks.LECTERN) || !hasLivingWordBible(level, snapshot.sourcePos())) {
                    LivingWordNetwork.stopListeningSession(sessionId);
                    STATIONS.removeSession(snapshot.dimension(), snapshot.sourcePos());
                    stationData(level).remove(snapshot.sourcePos()).ifPresent(station -> removeDisplayEntity(level, station));
                    return true;
                }

                LivingWordNetwork.stopListeningSession(sessionId);
                Optional<ScriptureDiscSelection> nextSelection = ScriptureDiscPlaybackSequencer.nextSelection(dataManager(), snapshot.selection());
                if (nextSelection.isEmpty()) {
                    STATIONS.pause(snapshot.dimension(), snapshot.sourcePos(), 0L);
                    stationData(level).get(snapshot.sourcePos()).ifPresent(station ->
                        stationData(level).put(station.withResumePosition(0L))
                    );
                    return true;
                }

                ScriptureDiscSelection next = nextSelection.orElseThrow();
                LecternStationSavedData savedData = stationData(level);
                LecternListeningStation nextStation = savedData.get(snapshot.sourcePos())
                    .orElseGet(() -> new LecternListeningStation(snapshot.sourcePos(), next))
                    .withSelection(next);
                nextStation = saveStationWithDisplay(level, savedData, nextStation);
                ListeningSession nextSession = LivingWordNetwork.startPositionedListeningSession(
                    level,
                    snapshot.sourcePos(),
                    next.translationId(),
                    next.bookId(),
                    next.chapter(),
                    next.audioManifestId(),
                    LECTERN_LISTENING_RADIUS,
                    0L
                );
                STATIONS.remember(level.dimension().location(), nextStation);
                STATIONS.rememberSession(snapshot.dimension(), snapshot.sourcePos(), next, nextSession.id(), 0L);
                return true;
            })
            .orElse(false);
    }

    public static void pauseSessionsForParticipant(ServerPlayer player) {
        long now = Util.getMillis();
        UUID playerId = player.getUUID();
        for (LecternListeningStationRegistry.PlayingSessionSnapshot snapshot : STATIONS.playingSessions()) {
            Optional<ListeningSession> session = LivingWordNetwork.currentListeningSession(snapshot.sessionId());
            if (session.isEmpty() || !session.orElseThrow().participants().contains(playerId)) {
                continue;
            }
            ServerLevel level = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, snapshot.dimension()));
            if (level == null) {
                continue;
            }
            long resumePosition = LivingWordNetwork.stopListeningSession(snapshot.sessionId())
                .map(stopped -> stopped.positionMillisAt(now))
                .orElseGet(() -> session.orElseThrow().positionMillisAt(now));
            pauseStationDisplay(level, snapshot, resumePosition);
        }
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Level clickedLevel = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = clickedLevel.getBlockState(pos);
        if (!state.is(Blocks.LECTERN)) {
            return;
        }

        ItemStack held = event.getItemStack();
        if (!held.isEmpty() && !held.is(LivingWordItems.BIBLE.get())) {
            return;
        }
        if (held.isEmpty() && !hasLivingWordBible(clickedLevel, pos)) {
            return;
        }
        if (held.is(LivingWordItems.BIBLE.get()) && state.getValue(LecternBlock.HAS_BOOK)) {
            return;
        }

        consume(event);
        if (clickedLevel.isClientSide() || !(clickedLevel instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (held.is(LivingWordItems.BIBLE.get())) {
            placeBibleOnLectern(serverPlayer, level, pos, state, held);
            return;
        }

        if (serverPlayer.isShiftKeyDown()) {
            removeBibleFromLectern(serverPlayer, level, pos, state);
        } else {
            openStation(serverPlayer, level, pos);
        }
    }

    private static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !event.getState().is(Blocks.LECTERN)) {
            return;
        }
        ResourceLocation dimension = level.dimension().location();
        BlockPos pos = event.getPos();
        STATIONS.removeSession(dimension, pos).ifPresent(LivingWordNetwork::stopListeningSession);
        stationData(level).remove(pos).ifPresent(station -> removeDisplayEntity(level, station));
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        displayUpdateTick = (displayUpdateTick + 1) % DISPLAY_UPDATE_INTERVAL_TICKS;
        if (displayUpdateTick != 0) {
            return;
        }
        long now = Util.getMillis();
        for (LecternListeningStationRegistry.PlayingSessionSnapshot snapshot : STATIONS.playingSessions()) {
            ServerLevel level = event.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, snapshot.dimension()));
            if (level == null) {
                continue;
            }
            LivingWordNetwork.currentListeningSession(snapshot.sessionId()).ifPresentOrElse(session -> {
                long positionMillis = session.positionMillisAt(now);
                updateFloatingVerseDisplay(level, snapshot, positionMillis);
            }, () -> STATIONS.removeSession(snapshot.dimension(), snapshot.sourcePos()));
        }
    }

    private static void placeBibleOnLectern(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, ItemStack held) {
        if (!LecternBlock.tryPlaceBook(player, level, pos, state, held)) {
            return;
        }
        LecternStationSavedData savedData = stationData(level);
        LecternListeningStation station = savedData.get(pos).orElseGet(() -> new LecternListeningStation(pos, ScriptureDiscSelection.defaults()));
        station = saveStationWithDisplay(level, savedData, station);
        STATIONS.remember(level.dimension().location(), station);
        player.displayClientMessage(Component.translatable("message.livingword.lectern.bible_placed", formatSelection(station.selection())), true);
    }

    private static void openStation(ServerPlayer player, ServerLevel level, BlockPos pos) {
        LecternStationSavedData savedData = stationData(level);
        LecternListeningStation station = savedData.get(pos).orElseGet(() -> {
            LecternListeningStation created = new LecternListeningStation(pos, ScriptureDiscSelection.defaults());
            savedData.put(created);
            return created;
        });
        STATIONS.remember(level.dimension().location(), station);
        boolean playing = STATIONS.getPlayingSession(level.dimension().location(), pos).isPresent();
        player.displayClientMessage(Component.translatable("message.livingword.lectern.station_ready", formatSelection(station.selection())), true);
        PacketDistributor.sendToPlayer(player, OpenLecternStationPayload.fromStation(station, playing));
    }

    private static void startStation(ServerPlayer player, ServerLevel level, LecternStationSavedData savedData, LecternListeningStation station) {
        ResourceLocation dimension = level.dimension().location();
        if (STATIONS.getPlayingSession(dimension, station.sourcePos()).isPresent()) {
            player.displayClientMessage(Component.translatable("message.livingword.lectern.session_started", formatSelection(station.selection())), true);
            return;
        }
        stopActiveSession(level, station.sourcePos(), false);
        pauseSessionsForParticipant(player);
        ScriptureDiscEvents.pauseSessionsForParticipant(player);

        long resumePosition = savedData.get(station.sourcePos())
            .filter(existing -> existing.selection().equals(station.selection()))
            .map(LecternListeningStation::resumePositionMillis)
            .orElse(0L);
        resumePosition = Math.max(resumePosition, STATIONS.resumePosition(dimension, station.sourcePos(), station.selection()));

        LecternListeningStation savedStation = saveStationWithDisplay(level, savedData, station.withResumePosition(resumePosition));
        ListeningSession session = LivingWordNetwork.startPositionedListeningSession(
            level,
            station.sourcePos(),
            station.selection().translationId(),
            station.selection().bookId(),
            station.selection().chapter(),
            station.selection().audioManifestId(),
            LECTERN_LISTENING_RADIUS,
            resumePosition
        );
        STATIONS.remember(dimension, savedStation);
        STATIONS.rememberSession(dimension, station.sourcePos(), station.selection(), session.id(), resumePosition);
        player.displayClientMessage(Component.translatable("message.livingword.lectern.session_started", formatSelection(station.selection())), true);
    }

    private static void stopStation(ServerPlayer player, ServerLevel level, LecternStationSavedData savedData, LecternListeningStation station) {
        long resumePosition = stopActiveSession(level, station.sourcePos(), true);
        LecternListeningStation savedStation = saveStationWithDisplay(level, savedData, station.withResumePosition(resumePosition));
        STATIONS.remember(level.dimension().location(), savedStation);
        player.displayClientMessage(Component.translatable("message.livingword.lectern.session_stopped"), true);
    }

    private static void pauseStation(ServerPlayer player, ServerLevel level, LecternStationSavedData savedData, LecternListeningStation station) {
        ResourceLocation dimension = level.dimension().location();
        boolean active = STATIONS.getPlayingSession(dimension, station.sourcePos()).isPresent();
        long resumePosition = active
            ? stopActiveSession(level, station.sourcePos(), true)
            : savedData.get(station.sourcePos()).map(LecternListeningStation::resumePositionMillis).orElse(station.resumePositionMillis());
        LecternListeningStation savedStation = saveStationWithDisplay(level, savedData, station.withResumePosition(resumePosition));
        STATIONS.remember(dimension, savedStation);
        player.displayClientMessage(Component.translatable("message.livingword.lectern.session_paused"), true);
    }

    private static void resetStation(ServerPlayer player, ServerLevel level, LecternStationSavedData savedData, LecternListeningStation station) {
        ResourceLocation dimension = level.dimension().location();
        STATIONS.removeSession(dimension, station.sourcePos()).ifPresent(LivingWordNetwork::stopListeningSession);

        ScriptureDiscSelection resetSelection = firstChapterSelection(station.selection());
        LecternListeningStation resetStation = saveStationWithDisplay(
            level,
            savedData,
            station.withSelection(resetSelection).withResumePosition(0L)
        );
        STATIONS.remember(dimension, resetStation);
        player.displayClientMessage(Component.translatable("message.livingword.lectern.session_reset", formatSelection(resetSelection)), true);
    }

    private static void toggleDisplay(ServerPlayer player, ServerLevel level, LecternStationSavedData savedData, LecternListeningStation station) {
        LecternListeningStation nextStation;
        if (station.displayEnabled()) {
            removeDisplayEntity(level, station);
            nextStation = station.withDisplayEnabled(false);
            savedData.put(nextStation);
            player.displayClientMessage(Component.translatable("message.livingword.lectern.display_disabled"), true);
        } else {
            nextStation = saveStationWithDisplay(level, savedData, station.withDisplayEnabled(true));
            player.displayClientMessage(Component.translatable("message.livingword.lectern.display_enabled"), true);
        }
        STATIONS.remember(level.dimension().location(), nextStation);
    }

    private static long stopActiveSession(ServerLevel level, BlockPos pos, boolean rememberPause) {
        ResourceLocation dimension = level.dimension().location();
        Optional<UUID> sessionId = STATIONS.getPlayingSession(dimension, pos);
        if (sessionId.isEmpty()) {
            return 0L;
        }
        long now = Util.getMillis();
        long resumePosition = LivingWordNetwork.stopListeningSession(sessionId.orElseThrow())
            .map(session -> session.positionMillisAt(now))
            .orElse(0L);
        if (rememberPause) {
            STATIONS.pause(dimension, pos, resumePosition);
        } else {
            STATIONS.removeSession(dimension, pos);
        }
        return resumePosition;
    }

    private static void pauseStationDisplay(ServerLevel level, LecternListeningStationRegistry.PlayingSessionSnapshot snapshot, long resumePosition) {
        STATIONS.pause(snapshot.dimension(), snapshot.sourcePos(), resumePosition);
        LecternStationSavedData savedData = stationData(level);
        savedData.get(snapshot.sourcePos()).ifPresent(station -> {
            LecternListeningStation paused = station.withSelection(snapshot.selection()).withResumePosition(resumePosition);
            if (paused.displayEnabled()) {
                paused = syncDisplayEntities(level, paused, resumePosition);
            }
            savedData.put(paused);
            STATIONS.remember(snapshot.dimension(), paused);
        });
    }

    private static void removeBibleFromLectern(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        ResourceLocation dimension = level.dimension().location();
        STATIONS.removeSession(dimension, pos).ifPresent(LivingWordNetwork::stopListeningSession);
        stationData(level).remove(pos).ifPresent(station -> removeDisplayEntity(level, station));

        if (level.getBlockEntity(pos) instanceof LecternBlockEntity lectern) {
            ItemStack book = lectern.getBook().copy();
            lectern.clearContent();
            LecternBlock.resetBookState(player, level, pos, state, false);
            if (!book.isEmpty() && !player.addItem(book)) {
                player.drop(book, false);
            }
        }
        player.displayClientMessage(Component.translatable("message.livingword.lectern.bible_removed"), true);
    }

    private static LecternListeningStation saveStationWithDisplay(ServerLevel level, LecternStationSavedData savedData, LecternListeningStation station) {
        if (!station.displayEnabled()) {
            removeDisplayEntity(level, station);
            LecternListeningStation hidden = station.withDisplayEntityId(Optional.empty());
            savedData.put(hidden);
            return hidden;
        }
        LecternListeningStation displayed = updateDisplayEntity(level, station);
        savedData.put(displayed);
        return displayed;
    }

    private static LecternListeningStation updateDisplayEntity(ServerLevel level, LecternListeningStation station) {
        if (!station.displayEnabled()) {
            removeDisplayEntity(level, station);
            return station.withDisplayEntityId(Optional.empty());
        }
        return syncDisplayEntities(level, station, station.resumePositionMillis());
    }

    private static void updateFloatingVerseDisplay(ServerLevel level, LecternListeningStationRegistry.PlayingSessionSnapshot snapshot, long positionMillis) {
        if (!level.getBlockState(snapshot.sourcePos()).is(Blocks.LECTERN) || !hasLivingWordBible(level, snapshot.sourcePos())) {
            return;
        }
        LecternStationSavedData savedData = stationData(level);
        savedData.get(snapshot.sourcePos()).ifPresent(station -> {
            if (!station.displayEnabled()) {
                removeDisplayEntity(level, station);
                savedData.put(station.withDisplayEntityId(Optional.empty()));
                return;
            }
            LecternListeningStation updated = station.withSelection(snapshot.selection()).withResumePosition(positionMillis);
            LecternListeningStation restored = syncDisplayEntities(level, updated, positionMillis);
            if (!restored.displayEntityId().equals(station.displayEntityId())) {
                savedData.put(restored);
                STATIONS.remember(level.dimension().location(), restored);
            }
        });
    }

    private static void removeDisplayEntity(ServerLevel level, LecternListeningStation station) {
        station.displayEntityId().map(level::getEntity).ifPresent(Entity::discard);
        for (ArmorStand display : displayEntities(level, station.sourcePos())) {
            display.discard();
        }
    }

    private static LecternListeningStation syncDisplayEntities(ServerLevel level, LecternListeningStation station, long positionMillis) {
        removeLegacyDisplayEntity(level, station);
        List<Component> components = displayComponents(station.selection(), positionMillis);
        List<ArmorStand> existing = displayEntities(level, station.sourcePos());
        List<ArmorStand> active = new ArrayList<>(components.size());

        for (int i = 0; i < components.size(); i++) {
            int lineIndex = i;
            ArmorStand display = findDisplayLine(existing, lineIndex).orElseGet(() -> createDisplayLine(level, station.sourcePos(), lineIndex));
            display.setCustomName(components.get(i));
            display.setPos(station.sourcePos().getX() + 0.5D, displayY(station.sourcePos(), lineIndex, components.size()), station.sourcePos().getZ() + 0.5D);
            active.add(display);
        }

        for (ArmorStand display : existing) {
            Optional<Integer> index = displayLineIndex(display);
            if (index.isEmpty() || index.orElseThrow() >= components.size()) {
                display.discard();
            }
        }

        return station.withDisplayEntityId(active.isEmpty() ? Optional.empty() : Optional.of(active.getFirst().getUUID()));
    }

    private static void removeLegacyDisplayEntity(ServerLevel level, LecternListeningStation station) {
        station.displayEntityId()
            .map(level::getEntity)
            .filter(entity -> !(entity instanceof ArmorStand armorStand && armorStand.getTags().contains(DISPLAY_TAG)))
            .ifPresent(Entity::discard);
    }

    private static List<ArmorStand> displayEntities(ServerLevel level, BlockPos pos) {
        String stationTag = displayStationTag(pos);
        return level.getEntitiesOfClass(
            ArmorStand.class,
            new AABB(pos).inflate(2.0D, 3.0D, 2.0D),
            display -> display.getTags().contains(DISPLAY_TAG) && display.getTags().contains(stationTag)
        );
    }

    private static Optional<ArmorStand> findDisplayLine(List<ArmorStand> displays, int lineIndex) {
        for (ArmorStand display : displays) {
            if (displayLineIndex(display).filter(index -> index == lineIndex).isPresent()) {
                return Optional.of(display);
            }
        }
        return Optional.empty();
    }

    private static Optional<Integer> displayLineIndex(ArmorStand display) {
        for (String tag : display.getTags()) {
            if (!tag.startsWith(DISPLAY_LINE_TAG_PREFIX)) {
                continue;
            }
            try {
                return Optional.of(Integer.parseInt(tag.substring(DISPLAY_LINE_TAG_PREFIX.length())));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static ArmorStand createDisplayLine(ServerLevel level, BlockPos pos, int lineIndex) {
        ArmorStand display = new ArmorStand(level, pos.getX() + 0.5D, displayY(pos, lineIndex, lineIndex + 1), pos.getZ() + 0.5D);
        display.setInvisible(true);
        display.setNoGravity(true);
        display.setSilent(true);
        display.setInvulnerable(true);
        display.setCustomNameVisible(true);
        display.setNoBasePlate(true);
        display.addTag(DISPLAY_TAG);
        display.addTag(displayStationTag(pos));
        display.addTag(DISPLAY_LINE_TAG_PREFIX + lineIndex);
        display.getEntityData().set(ArmorStand.DATA_CLIENT_FLAGS, (byte)(ArmorStand.CLIENT_FLAG_MARKER | 8));
        level.addFreshEntity(display);
        return display;
    }

    private static double displayY(BlockPos pos, int lineIndex, int lineCount) {
        double baseY = pos.getY() + 1.62D + (lineCount - 1) * DISPLAY_LINE_SPACING * 0.5D;
        return baseY - lineIndex * DISPLAY_LINE_SPACING;
    }

    private static String displayStationTag(BlockPos pos) {
        return "livingword_lectern_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
    }

    private static List<Component> displayComponents(ScriptureDiscSelection selection, long positionMillis) {
        Optional<ChapterData> chapter = dataManager().getChapter(selection.translationId(), selection.bookId(), selection.chapter());
        Optional<VerseTimestampMap> timings = timingRepository().timestamps(
            new AudioChapterId(selection.translationId(), selection.bookId(), selection.chapter()),
            selection.audioManifestId()
        );
        if (!LecternVerseDisplay.hasUsableVerseTiming(timings)) {
            return List.of();
        }
        int verse = chapter.map(value -> LecternVerseDisplay.verseAt(value, positionMillis, timings, selection.translationId(), selection.audioManifestId())).orElse(1);
        String verseText = chapter.map(value -> value.verseText(verse)).orElse("");
        int activeWordIndex = chapter.map(value -> LecternVerseDisplay.activeWordIndex(value, verse, positionMillis, timings, selection.translationId(), selection.audioManifestId())).orElse(-1);
        List<LecternVerseDisplay.DisplayLine> lines = LecternVerseDisplay.displayLines(formatSelection(selection), verse, verseText, activeWordIndex);
        List<Component> components = new ArrayList<>(lines.size());
        long now = Util.getMillis();
        for (int i = 0; i < lines.size(); i++) {
            int color = LecternVerseDisplay.colorAt(now + i * 450L);
            components.add(displayComponent(lines.get(i), color));
        }
        return components;
    }

    private static Component displayComponent(LecternVerseDisplay.DisplayLine line, int color) {
        MutableComponent component = Component.empty();
        for (LecternVerseDisplay.DisplayToken token : line.tokens()) {
            int tokenColor = token.active() ? 0xFFFFFF : color;
            component.append(Component.literal(token.text()).withStyle(style -> style
                .withColor(TextColor.fromRgb(tokenColor))
                .withBold(true)
                .withUnderlined(token.active())
            ));
        }
        return component;
    }

    private static boolean hasLivingWordBible(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof LecternBlockEntity lectern && lectern.getBook().is(LivingWordItems.BIBLE.get());
    }

    private static ScriptureDiscSelection normalizeSelection(ScriptureDiscSelection selection) {
        if (dataManager().getChapter(selection.translationId(), selection.bookId(), selection.chapter()).isPresent()) {
            return selection;
        }
        return dataManager().firstChapter(selection.translationId())
            .map(chapter -> new ScriptureDiscSelection(
                chapter.translationId(),
                chapter.bookId(),
                chapter.chapter(),
                selection.audioManifestId(),
                selection.playbackMode()
            ))
            .orElse(ScriptureDiscSelection.defaults());
    }

    private static ScriptureDiscSelection firstChapterSelection(ScriptureDiscSelection selection) {
        int firstChapter = dataManager().chapters(selection.translationId(), selection.bookId())
            .stream()
            .findFirst()
            .orElse(1);
        return new ScriptureDiscSelection(
            selection.translationId(),
            selection.bookId(),
            firstChapter,
            selection.audioManifestId(),
            selection.playbackMode()
        );
    }

    private static LecternStationSavedData stationData(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(LecternStationSavedData.factory(), LecternStationSavedData.fileId());
    }

    private static BibleDataManager dataManager() {
        if (bibleDataManager == null) {
            bibleDataManager = new BibleDataManager();
            new BibleResourceLoader(bibleDataManager, LecternEvents.class.getClassLoader()).reload();
        }
        return bibleDataManager;
    }

    private static AudioTimingRepository timingRepository() {
        if (audioTimingRepository == null) {
            audioTimingRepository = new AudioTimingRepository(LecternEvents.class.getClassLoader());
        }
        return audioTimingRepository;
    }

    private static void consume(PlayerInteractEvent.RightClickBlock event) {
        event.setCancellationResult(InteractionResult.CONSUME);
        event.setCanceled(true);
    }

    private static String formatSelection(ScriptureDiscSelection selection) {
        return selection.translationId().toUpperCase(Locale.ROOT)
            + " "
            + formatBookId(selection.bookId())
            + " "
            + selection.chapter();
    }

    private static String formatReference(BibleReference reference) {
        return reference.translationId().toUpperCase(Locale.ROOT)
            + " "
            + formatBookId(reference.bookId())
            + " "
            + reference.chapter();
    }

    private static String formatBookId(String bookId) {
        String[] words = bookId.replace('_', ' ').split(" ");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!formatted.isEmpty()) {
                formatted.append(' ');
            }
            formatted.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return formatted.toString();
    }
}
