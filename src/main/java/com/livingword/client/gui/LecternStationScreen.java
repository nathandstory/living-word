package com.livingword.client.gui;

import com.livingword.bible.BibleDataManager;
import com.livingword.bible.TranslationManifest;
import com.livingword.audio.AudioChapterId;
import com.livingword.audio.AudioManifestRepository;
import com.livingword.audio.AudioTimingRepository;
import com.livingword.client.BibleClientRepository;
import com.livingword.client.LivingWordClient;
import com.livingword.discs.ScriptureDiscAudioSource;
import com.livingword.discs.ScriptureDiscPlaybackMode;
import com.livingword.discs.ScriptureDiscSelection;
import com.livingword.lectern.LecternStationAction;
import com.livingword.network.payload.OpenLecternStationPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class LecternStationScreen extends Screen {
    private static final int BACKGROUND = 0xF0181510;
    private static final int PANEL = 0xF02B2118;
    private static final int PAGE = 0xFFE6D0A3;
    private static final int PAGE_DARK = 0xFFD8B97C;
    private static final int BORDER = 0xFF8C6A3E;
    private static final int TEXT = 0xFF3B2A18;
    private static final int MUTED_TEXT = 0xFF7C5426;
    private static final int TITLE_TEXT = 0xFFFFE3AD;

    private final BibleDataManager dataManager = BibleClientRepository.dataManager();
    private final AudioManifestRepository audioManifests = new AudioManifestRepository(LecternStationScreen.class.getClassLoader());
    private final AudioTimingRepository audioTimings = new AudioTimingRepository(LecternStationScreen.class.getClassLoader());
    private final BlockPos sourcePos;

    private String translationId;
    private String bookId;
    private int chapter;
    private String audioManifestId;
    private ScriptureDiscPlaybackMode playbackMode;
    private long resumePositionMillis;
    private boolean stationPlaying;
    private boolean displayEnabled;
    private String statusLine;

    private EditBox bookSearchBox;
    private Button translationButton;
    private Button bookButton;
    private Button chapterButton;
    private Button sourceButton;
    private Button modeButton;
    private Button playbackToggleButton;
    private Button displayToggleButton;

    public LecternStationScreen(OpenLecternStationPayload payload) {
        super(Component.translatable("gui.livingword.lectern.title"));
        this.sourcePos = payload.sourcePos();
        this.translationId = payload.translationId();
        this.bookId = payload.bookId();
        this.chapter = payload.chapter();
        this.audioManifestId = payload.audioManifestId();
        this.playbackMode = payload.playbackMode();
        this.resumePositionMillis = payload.resumePositionMillis();
        this.stationPlaying = payload.playing();
        this.displayEnabled = payload.displayEnabled();
        this.statusLine = payload.playing()
            ? Component.translatable("gui.livingword.lectern.status_playing").getString()
            : Component.translatable("gui.livingword.lectern.status_ready").getString();
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(440, this.width - 24);
        int panelHeight = Math.min(352, this.height - 24);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int centerX = left + panelWidth / 2;
        int controlWidth = Math.min(310, panelWidth - 48);
        int controlLeft = centerX - controlWidth / 2;

        bookSearchBox = new EditBox(this.font, controlLeft, top + 48, controlWidth, 20, Component.translatable("gui.livingword.disc.search_book"));
        bookSearchBox.setHint(Component.translatable("gui.livingword.disc.search_book"));
        bookSearchBox.setResponder(query -> statusLine = query.isBlank() ? stationStatus() : Component.translatable("gui.livingword.disc.search_status").getString());
        addRenderableWidget(bookSearchBox);

        translationButton = addCycleButton(controlLeft, top + 76, controlWidth, button -> navigateTranslation(1));
        bookButton = addCycleButton(controlLeft, top + 102, controlWidth, button -> navigateBook(1));
        chapterButton = addCycleButton(controlLeft, top + 128, controlWidth, button -> navigateChapter(1));
        sourceButton = addCycleButton(controlLeft, top + 154, controlWidth, button -> navigateSource(1));
        modeButton = addCycleButton(controlLeft, top + 180, controlWidth, button -> navigateMode(1));

        int bottomY = top + panelHeight - 104;
        int actionWidth = (controlWidth - 12) / 3;
        addRenderableWidget(Button.builder(Component.translatable("gui.livingword.lectern.save"), button -> configure(LecternStationAction.SAVE))
            .bounds(controlLeft, bottomY, actionWidth, 20)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.livingword.lectern.reset"), button -> resetStation())
            .bounds(controlLeft + actionWidth + 6, bottomY, actionWidth, 20)
            .build());
        playbackToggleButton = addRenderableWidget(Button.builder(Component.empty(), button -> togglePlayback())
            .bounds(controlLeft + (actionWidth + 6) * 2, bottomY, actionWidth, 20)
            .build());
        displayToggleButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleFloatingVerse())
            .bounds(controlLeft, bottomY + 26, controlWidth, 20)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.close"), button -> onClose())
            .bounds(controlLeft, bottomY + 52, controlWidth, 20)
            .build());
        refreshLabels();
    }

    private Button addCycleButton(int x, int y, int width, Button.OnPress onPress) {
        return addRenderableWidget(Button.builder(Component.empty(), onPress)
            .bounds(x, y, width, 20)
            .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int panelWidth = Math.min(440, this.width - 24);
        int panelHeight = Math.min(352, this.height - 24);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        graphics.fill(0, 0, this.width, this.height, BACKGROUND);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, PANEL);
        graphics.fill(left + 8, top + 8, left + panelWidth - 8, top + panelHeight - 8, PAGE_DARK);
        graphics.fill(left + 14, top + 14, left + panelWidth - 14, top + panelHeight - 14, PAGE);
        graphics.fill(left, top, left + panelWidth, top + 1, BORDER);
        graphics.fill(left, top + panelHeight - 1, left + panelWidth, top + panelHeight, BORDER);
        graphics.fill(left, top, left + 1, top + panelHeight, BORDER);
        graphics.fill(left + panelWidth - 1, top, left + panelWidth, top + panelHeight, BORDER);

        super.render(graphics, mouseX, mouseY, partialTick);
        renderTopLabels(graphics, left, top, panelWidth);
        renderInstructionPanel(graphics, left, top, panelWidth, panelHeight);
    }

    private void renderTopLabels(GuiGraphics graphics, int left, int top, int panelWidth) {
        int centerX = left + panelWidth / 2;
        drawCenteredPlain(graphics, this.title.getString(), centerX, top + 14, panelWidth - 56, TITLE_TEXT);
        drawCenteredPlain(graphics, formatSelection(), centerX, top + 30, panelWidth - 56, TEXT);
    }

    private void renderInstructionPanel(GuiGraphics graphics, int left, int top, int panelWidth, int panelHeight) {
        int controlWidth = Math.min(310, panelWidth - 48);
        int controlLeft = left + (panelWidth - controlWidth) / 2;
        int bottomY = top + panelHeight - 104;
        int infoTop = top + 208;
        int infoBottom = Math.min(infoTop + 58, bottomY - 10);
        if (infoBottom <= infoTop + 18) {
            return;
        }

        graphics.fill(controlLeft, infoTop, controlLeft + controlWidth, infoBottom, 0x66F1D79A);
        graphics.fill(controlLeft, infoTop, controlLeft + controlWidth, infoTop + 1, BORDER);
        graphics.fill(controlLeft, infoBottom - 1, controlLeft + controlWidth, infoBottom, BORDER);
        graphics.fill(controlLeft, infoTop, controlLeft + 1, infoBottom, BORDER);
        graphics.fill(controlLeft + controlWidth - 1, infoTop, controlLeft + controlWidth, infoBottom, BORDER);

        int y = infoTop + 6;
        y = drawCenteredWrapped(graphics, modeDescription(), left + panelWidth / 2, y, controlWidth - 18, MUTED_TEXT, 2);
        String secondary = statusLine == null || statusLine.isBlank()
            ? Component.translatable("gui.livingword.disc.reverse_hint").getString()
            : statusLine;
        drawCenteredWrapped(graphics, secondary, left + panelWidth / 2, y + 2, controlWidth - 18, MUTED_TEXT, 2);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            if (reverseClick(translationButton, mouseX, mouseY, () -> navigateTranslation(-1))) {
                return true;
            }
            if (reverseClick(bookButton, mouseX, mouseY, () -> navigateBook(-1))) {
                return true;
            }
            if (reverseClick(chapterButton, mouseX, mouseY, () -> navigateChapter(-1))) {
                return true;
            }
            if (reverseClick(sourceButton, mouseX, mouseY, () -> navigateSource(-1))) {
                return true;
            }
            if (reverseClick(modeButton, mouseX, mouseY, () -> navigateMode(-1))) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && bookSearchBox != null && bookSearchBox.isFocused()) {
            applyBookSearch();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean reverseClick(Button button, double mouseX, double mouseY, Runnable action) {
        if (button != null && button.isMouseOver(mouseX, mouseY)) {
            button.playDownSound(Minecraft.getInstance().getSoundManager());
            action.run();
            return true;
        }
        return false;
    }

    private void navigateTranslation(int direction) {
        List<String> translations = dataManager.translations().stream().map(TranslationManifest::id).toList();
        SelectionCycle.next(translations, translationId, direction).ifPresent(nextTranslationId -> {
            translationId = nextTranslationId;
            audioManifestId = ScriptureDiscAudioSource.defaultFor(translationId).manifestId();
        });
        ensureSelectedChapterExists();
        statusLine = stationStatus();
        refreshLabels();
    }

    private void navigateBook(int direction) {
        List<String> books = dataManager.bookIds(translationId);
        SelectionCycle.next(books, bookId, direction).ifPresent(nextBookId -> bookId = nextBookId);
        chapter = firstChapterOrOne(translationId, bookId);
        statusLine = stationStatus();
        refreshLabels();
    }

    private void navigateChapter(int direction) {
        List<Integer> chapters = dataManager.chapters(translationId, bookId);
        SelectionCycle.next(chapters, chapter, direction).ifPresent(nextChapter -> chapter = nextChapter);
        statusLine = stationStatus();
        refreshLabels();
    }

    private void navigateSource(int direction) {
        audioManifestId = ScriptureDiscAudioSource.cycle(translationId, audioManifestId, direction).manifestId();
        statusLine = stationStatus();
        refreshLabels();
    }

    private void navigateMode(int direction) {
        List<ScriptureDiscPlaybackMode> modes = Arrays.asList(ScriptureDiscPlaybackMode.values());
        SelectionCycle.next(modes, playbackMode, direction).ifPresent(nextMode -> playbackMode = nextMode);
        statusLine = stationStatus();
        refreshLabels();
    }

    private void applyBookSearch() {
        String query = bookSearchBox == null ? "" : bookSearchBox.getValue().strip().toLowerCase(Locale.ROOT);
        if (query.isBlank()) {
            statusLine = Component.translatable("gui.livingword.disc.search_empty").getString();
            return;
        }
        for (String candidateBookId : dataManager.bookIds(translationId)) {
            String displayName = formatBookId(candidateBookId).toLowerCase(Locale.ROOT);
            String compactId = candidateBookId.replace("_", "").toLowerCase(Locale.ROOT);
            String spacedId = candidateBookId.replace('_', ' ').toLowerCase(Locale.ROOT);
            if (displayName.contains(query) || compactId.contains(query.replace(" ", "")) || spacedId.contains(query)) {
                bookId = candidateBookId;
                chapter = firstChapterOrOne(translationId, bookId);
                statusLine = Component.translatable("gui.livingword.disc.search_found", formatBookId(bookId)).getString();
                refreshLabels();
                return;
            }
        }
        statusLine = Component.translatable("gui.livingword.disc.search_none").getString();
    }

    private void togglePlayback() {
        configure(stationPlaying ? LecternStationAction.PAUSE : LecternStationAction.PLAY);
    }

    private void resetStation() {
        chapter = firstChapterOrOne(translationId, bookId);
        resumePositionMillis = 0L;
        stationPlaying = false;
        LivingWordClient.configureLecternStation(sourcePos, selection(), LecternStationAction.RESET);
        statusLine = Component.translatable("gui.livingword.lectern.status_reset").getString();
        refreshLabels();
    }

    private void toggleFloatingVerse() {
        if (!verseTimingAvailable()) {
            statusLine = Component.translatable("gui.livingword.lectern.status_display_unavailable").getString();
            refreshPlaybackButtons();
            return;
        }
        displayEnabled = !displayEnabled;
        LivingWordClient.configureLecternStation(sourcePos, selection(), LecternStationAction.TOGGLE_DISPLAY);
        statusLine = Component.translatable(displayEnabled ? "gui.livingword.lectern.status_display_on" : "gui.livingword.lectern.status_display_off").getString();
        refreshPlaybackButtons();
    }

    private void configure(LecternStationAction action) {
        LivingWordClient.configureLecternStation(sourcePos, selection(), action);
        if (action == LecternStationAction.PLAY) {
            stationPlaying = true;
            statusLine = Component.translatable("gui.livingword.lectern.status_started").getString();
        } else if (action == LecternStationAction.PAUSE) {
            stationPlaying = false;
            statusLine = Component.translatable("gui.livingword.lectern.status_paused").getString();
        } else {
            statusLine = Component.translatable("gui.livingword.lectern.status_saved").getString();
        }
        refreshPlaybackButtons();
    }

    private ScriptureDiscSelection selection() {
        return new ScriptureDiscSelection(translationId, bookId, chapter, audioManifestId, playbackMode);
    }

    private void refreshLabels() {
        if (translationButton != null) {
            translationButton.setMessage(Component.literal("Translation: " + translationId.toUpperCase(Locale.ROOT)));
        }
        if (bookButton != null) {
            bookButton.setMessage(Component.literal("Book: " + formatBookId(bookId)));
        }
        if (chapterButton != null) {
            chapterButton.setMessage(Component.literal("Chapter: " + chapter));
        }
        if (sourceButton != null) {
            sourceButton.setMessage(Component.literal("Narrator: " + ScriptureDiscAudioSource.byManifestId(translationId, audioManifestId).displayName()));
        }
        if (modeButton != null) {
            modeButton.setMessage(Component.literal("Mode: " + playbackMode.displayName()));
        }
        refreshPlaybackButtons();
    }

    private void refreshPlaybackButtons() {
        if (playbackToggleButton != null) {
            playbackToggleButton.setMessage(Component.translatable(stationPlaying ? "gui.livingword.lectern.pause" : "gui.livingword.lectern.play"));
        }
        if (displayToggleButton != null) {
            boolean verseTimingAvailable = verseTimingAvailable();
            displayToggleButton.active = verseTimingAvailable;
            displayToggleButton.setMessage(Component.translatable(
                verseTimingAvailable
                    ? displayEnabled ? "gui.livingword.lectern.display_on" : "gui.livingword.lectern.display_off"
                    : "gui.livingword.lectern.display_unavailable"
            ));
        }
    }

    private boolean verseTimingAvailable() {
        return audioManifests.find(translationId, audioManifestId)
            .filter(manifest -> manifest.verseTimings())
            .flatMap(manifest -> audioTimings.timestamps(new AudioChapterId(translationId, bookId, chapter), audioManifestId))
            .filter(timings -> !timings.verseStartMillis().isEmpty())
            .isPresent();
    }

    private void ensureSelectedChapterExists() {
        if (dataManager.getChapter(translationId, bookId, chapter).isEmpty()) {
            dataManager.firstChapter(translationId).ifPresent(first -> {
                bookId = first.bookId();
                chapter = first.chapter();
            });
        }
    }

    private int firstChapterOrOne(String selectedTranslationId, String selectedBookId) {
        List<Integer> chapters = dataManager.chapters(selectedTranslationId, selectedBookId);
        return chapters.isEmpty() ? 1 : chapters.getFirst();
    }

    private String formatSelection() {
        ScriptureDiscAudioSource source = ScriptureDiscAudioSource.byManifestId(translationId, audioManifestId);
        return translationId.toUpperCase(Locale.ROOT)
            + " / "
            + formatBookId(bookId)
            + " "
            + chapter
            + " / "
            + source.displayName();
    }

    private String stationStatus() {
        if (stationPlaying) {
            return Component.translatable("gui.livingword.lectern.status_playing").getString();
        }
        if (resumePositionMillis > 0L) {
            return Component.translatable("gui.livingword.lectern.status_resume", resumePositionMillis / 1000L).getString();
        }
        return Component.translatable("gui.livingword.lectern.status_ready").getString();
    }

    private String modeDescription() {
        return Component.translatable(switch (playbackMode) {
            case SINGLE_CHAPTER -> "gui.livingword.disc.mode.single";
            case CONTINUE_BOOK -> "gui.livingword.disc.mode.continue";
            case LOOP_CHAPTER -> "gui.livingword.disc.mode.loop";
        }).getString();
    }

    private void drawCenteredPlain(GuiGraphics graphics, String text, int centerX, int y, int maxWidth, int color) {
        String trimmed = this.font.plainSubstrByWidth(text, maxWidth);
        graphics.drawString(this.font, trimmed, centerX - this.font.width(trimmed) / 2, y, color, false);
    }

    private int drawCenteredWrapped(GuiGraphics graphics, String text, int centerX, int y, int maxWidth, int color, int maxLines) {
        int lineY = y;
        for (String line : wrapText(text, maxWidth, maxLines)) {
            graphics.drawString(this.font, line, centerX - this.font.width(line) / 2, lineY, color, false);
            lineY += 11;
        }
        return lineY;
    }

    private List<String> wrapText(String text, int maxWidth, int maxLines) {
        if (text == null || text.isBlank() || maxLines <= 0) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (current.length() > 0 && this.font.width(candidate) > maxWidth) {
                lines.add(current.toString());
                current = new StringBuilder(word);
                if (lines.size() == maxLines) {
                    return lines;
                }
                continue;
            }
            current = new StringBuilder(candidate);
        }
        if (current.length() > 0 && lines.size() < maxLines) {
            lines.add(current.toString());
        }
        return List.copyOf(lines);
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
