package com.livingword.client.gui;

import com.livingword.bible.BibleDataManager;
import com.livingword.bible.BibleReference;
import com.livingword.bible.ChapterData;
import com.livingword.bible.TranslationManifest;
import com.livingword.client.BibleClientPreferences;
import com.livingword.client.BibleClientRepository;
import com.livingword.client.LivingWordClient;
import com.livingword.client.gui.widgets.VerseListWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BibleScreen extends Screen {
    private static final int BACKGROUND = 0xF0181510;
    private static final int PANEL = 0xF02B2118;
    private static final int PAGE = 0xFFE6D0A3;
    private static final int PAGE_DARK = 0xFFD6B982;
    private static final int BORDER = 0xFF8C6A3E;
    private static final int TEXT = 0xFF3B2A18;
    private static final int TITLE_TEXT = 0xFFFFE3AD;

    private final BibleDataManager dataManager;
    private final BibleGuiState state;
    private final Path preferencesPath;
    private final VerseListWidget verseList = new VerseListWidget();
    private EditBox searchBox;
    private int verseListX;
    private int verseListY;
    private int verseListWidth;
    private int verseListHeight;
    private int verseScrollOffset;

    public BibleScreen() {
        super(Component.translatable("gui.livingword.bible.title"));
        this.dataManager = BibleClientRepository.dataManager();
        this.preferencesPath = Minecraft.getInstance().gameDirectory.toPath().resolve("livingword").resolve("bible_state.json");
        ChapterData initialChapter = dataManager.translations().stream()
            .map(TranslationManifest::id)
            .map(dataManager::firstChapter)
            .flatMap(Optional::stream)
            .findFirst()
            .orElseGet(BibleScreen::emptyFallbackChapter);
        this.state = BibleGuiState.initial(initialChapter.translationId(), initialChapter.bookId(), initialChapter.chapter());
        selectFirstVerse(initialChapter);
        recordCurrentHistory();
        restoreStoredState();
    }

    @Override
    protected void init() {
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int bottomButtonY = top + panelHeight - 28;
        int searchY = top + 30;
        int navY = top + 56;
        int searchWidth = Math.max(92, panelWidth - 184);
        int navButtonWidth = Math.max(44, (panelWidth - 40 - 18) / 4);
        int bottomButtonWidth = Math.max(54, (panelWidth - 40 - 18) / 4);

        searchBox = new EditBox(this.font, left + 20, searchY, searchWidth, 20, Component.translatable("gui.livingword.bible.search"));
        searchBox.setResponder(state::setSearchQuery);
        searchBox.setHint(Component.translatable("gui.livingword.bible.search"));
        addRenderableWidget(searchBox);

        addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.search_go"), button -> jumpToFirstSearchResult())
            .bounds(left + 26 + searchWidth, searchY, 42, 20)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.search_next"), button -> jumpToNextSearchResult())
            .bounds(left + 74 + searchWidth, searchY, 50, 20)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.close"), button -> onClose())
            .bounds(left + panelWidth - 62, top + 8, 42, 20)
            .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.previous_book"), button -> navigateBook(-1))
            .bounds(left + 20, navY, navButtonWidth, 20)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.previous_chapter"), button -> navigateChapter(-1))
            .bounds(left + 26 + navButtonWidth, navY, navButtonWidth, 20)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.next_chapter"), button -> navigateChapter(1))
            .bounds(left + 32 + navButtonWidth * 2, navY, navButtonWidth, 20)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.next_book"), button -> navigateBook(1))
            .bounds(left + 38 + navButtonWidth * 3, navY, navButtonWidth, 20)
            .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.bookmark"), button -> {
                state.addBookmark(state.selectedReference());
                persistState();
            })
            .bounds(left + 20, bottomButtonY, bottomButtonWidth, 20)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.version"), button -> navigateTranslation(1))
            .bounds(left + 26 + bottomButtonWidth, bottomButtonY, bottomButtonWidth, 20)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.listen"), button -> LivingWordClient.playLocalChapter(state.translationId(), state.bookId(), state.chapter()))
            .bounds(left + 32 + bottomButtonWidth * 2, bottomButtonY, bottomButtonWidth, 20)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.copy"), button -> copySelectedVerse())
            .bounds(left + 38 + bottomButtonWidth * 3, bottomButtonY, bottomButtonWidth, 20)
            .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        graphics.fill(0, 0, this.width, this.height, BACKGROUND);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, PANEL);
        graphics.fill(left + 8, top + 8, left + panelWidth - 8, top + panelHeight - 8, PAGE_DARK);
        graphics.fill(left + 14, top + 14, left + panelWidth - 14, top + panelHeight - 14, PAGE);
        int spineX = left + panelWidth / 2;
        graphics.fill(spineX - 1, top + 14, spineX + 1, top + panelHeight - 14, 0x55815F32);
        graphics.fill(left, top, left + panelWidth, top + 1, BORDER);
        graphics.fill(left, top + panelHeight - 1, left + panelWidth, top + panelHeight, BORDER);
        graphics.fill(left, top, left + 1, top + panelHeight, BORDER);
        graphics.fill(left + panelWidth - 1, top, left + panelWidth, top + panelHeight, BORDER);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 10, TITLE_TEXT);
        graphics.drawCenteredString(this.font, currentHeading(), this.width / 2, top + 84, TEXT);
        if (!state.searchResultSummary().isEmpty()) {
            graphics.drawString(this.font, state.searchResultSummary(), left + panelWidth - 24 - this.font.width(state.searchResultSummary()), top + 84, 0xFF7C5426, false);
        }
        currentChapter().ifPresent(chapter -> {
            verseListX = left + 24;
            verseListY = top + 104;
            verseListWidth = panelWidth - 48;
            verseListHeight = Math.max(32, panelHeight - 144);
            verseScrollOffset = clampScroll(chapter, verseScrollOffset);
            verseList.render(graphics, this.font, chapter, state, verseListX, verseListY, verseListWidth, verseListHeight, verseScrollOffset);
            renderScrollBar(graphics, chapter);
        });
        renderBibleWidgets(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        Optional<ChapterData> chapter = currentChapter();
        if (chapter.isPresent() && isInsideVerseList(mouseX, mouseY)) {
            var selectedVerse = verseList.verseAt(chapter.get(), this.font, verseListWidth, verseListY, mouseY, verseScrollOffset);
            if (selectedVerse.isPresent()) {
                state.selectVerse(selectedVerse.getAsInt());
                recordCurrentHistory();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isInsideVerseList(mouseX, mouseY)) {
            currentChapter().ifPresent(chapter -> {
                int nextOffset = verseScrollOffset - (int) Math.round(scrollY * 28);
                verseScrollOffset = clampScroll(chapter, nextOffset);
            });
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        persistState();
        super.onClose();
    }

    private void copySelectedVerse() {
        currentChapter().flatMap(chapter -> chapter.getVerse(state.selectedVerse())).ifPresent(text -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.keyboardHandler.setClipboard(formatBookId(state.bookId()) + " " + state.chapter() + ":" + state.selectedVerse() + " " + text);
        });
    }

    private void jumpToFirstSearchResult() {
        List<BibleReference> results = dataManager.search(state.translationId(), state.searchQuery(), 100);
        state.replaceSearchResults(results);
        state.currentSearchResult().ifPresent(this::navigateTo);
    }

    private void jumpToNextSearchResult() {
        if (state.currentSearchResult().isEmpty()) {
            jumpToFirstSearchResult();
            return;
        }
        state.advanceSearchResult(1);
        state.currentSearchResult().ifPresent(this::navigateTo);
    }

    private void navigateBook(int direction) {
        List<String> books = dataManager.bookIds(state.translationId());
        if (books.isEmpty()) {
            return;
        }
        int currentIndex = Math.max(0, books.indexOf(state.bookId()));
        int nextIndex = Math.floorMod(currentIndex + direction, books.size());
        String nextBookId = books.get(nextIndex);
        List<Integer> chapters = dataManager.chapters(state.translationId(), nextBookId);
        if (!chapters.isEmpty()) {
            setPassage(state.translationId(), nextBookId, chapters.getFirst());
        }
    }

    private void navigateChapter(int direction) {
        List<Integer> chapters = dataManager.chapters(state.translationId(), state.bookId());
        if (chapters.isEmpty()) {
            return;
        }
        int currentIndex = Math.max(0, chapters.indexOf(state.chapter()));
        int nextIndex = Math.floorMod(currentIndex + direction, chapters.size());
        setPassage(state.translationId(), state.bookId(), chapters.get(nextIndex));
    }

    private void navigateTranslation(int direction) {
        List<String> translationIds = dataManager.translations().stream().map(TranslationManifest::id).toList();
        if (translationIds.isEmpty()) {
            return;
        }
        int currentIndex = Math.max(0, translationIds.indexOf(state.translationId()));
        String nextTranslationId = translationIds.get(Math.floorMod(currentIndex + direction, translationIds.size()));
        if (dataManager.getChapter(nextTranslationId, state.bookId(), state.chapter()).isPresent()) {
            setPassage(nextTranslationId, state.bookId(), state.chapter());
            return;
        }
        dataManager.firstChapter(nextTranslationId).ifPresent(chapter -> setPassage(chapter.translationId(), chapter.bookId(), chapter.chapter()));
    }

    private void navigateTo(BibleReference reference) {
        setPassage(reference.translationId(), reference.bookId(), reference.chapter());
        state.selectVerse(reference.verse());
        recordCurrentHistory();
    }

    private void setPassage(String translationId, String bookId, int chapter) {
        state.setPassage(translationId, bookId, chapter);
        verseScrollOffset = 0;
        currentChapter().ifPresent(this::selectFirstVerse);
        recordCurrentHistory();
    }

    private Optional<ChapterData> currentChapter() {
        return dataManager.getChapter(state.translationId(), state.bookId(), state.chapter());
    }

    private String currentHeading() {
        String translationName = dataManager.getTranslation(state.translationId())
            .map(TranslationManifest::displayName)
            .orElse(state.translationId().toUpperCase(java.util.Locale.ROOT));
        return translationName + " / " + formatBookId(state.bookId()) + " " + state.chapter();
    }

    private void recordCurrentHistory() {
        state.recordHistory(state.selectedReference());
    }

    private void restoreStoredState() {
        BibleClientPreferences.StoredBibleState storedState = BibleClientPreferences.load(preferencesPath);
        state.replaceBookmarks(storedState.bookmarks());
        state.replaceRecentHistory(storedState.recentHistory());
        storedState.lastReference()
            .filter(reference -> dataManager.getChapter(reference.translationId(), reference.bookId(), reference.chapter()).isPresent())
            .ifPresent(this::navigateTo);
    }

    private void persistState() {
        BibleClientPreferences.save(
            preferencesPath,
            new BibleClientPreferences.StoredBibleState(Optional.of(state.selectedReference()), state.bookmarks(), state.recentHistory())
        );
    }

    private void selectFirstVerse(ChapterData chapter) {
        chapter.verses().keySet().stream().min(Integer::compareTo).ifPresentOrElse(state::selectVerse, () -> state.selectVerse(1));
    }

    private boolean isInsideVerseList(double mouseX, double mouseY) {
        return mouseX >= verseListX && mouseX <= verseListX + verseListWidth && mouseY >= verseListY && mouseY <= verseListY + verseListHeight;
    }

    private int clampScroll(ChapterData chapter, int scrollOffset) {
        int maxScroll = verseList.maxScroll(chapter, this.font, verseListWidth, verseListHeight);
        return Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    private void renderScrollBar(GuiGraphics graphics, ChapterData chapter) {
        int maxScroll = verseList.maxScroll(chapter, this.font, verseListWidth, verseListHeight);
        if (maxScroll <= 0) {
            return;
        }
        int barX = verseListX + verseListWidth - 4;
        int trackHeight = verseListHeight;
        int thumbHeight = Math.max(16, trackHeight * trackHeight / (trackHeight + maxScroll));
        int thumbY = verseListY + (trackHeight - thumbHeight) * verseScrollOffset / maxScroll;
        graphics.fill(barX, verseListY, barX + 2, verseListY + trackHeight, 0x664D3B27);
        graphics.fill(barX, thumbY, barX + 2, thumbY + thumbHeight, BORDER);
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

    private static ChapterData emptyFallbackChapter() {
        return new ChapterData("kjv", "john", 1, Map.of(1, "No Bible data is loaded."));
    }

    private void renderBibleWidgets(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        for (Renderable renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private int panelWidth() {
        return Math.min(640, Math.max(240, this.width - 16));
    }

    private int panelHeight() {
        return Math.min(420, Math.max(180, this.height - 16));
    }
}
