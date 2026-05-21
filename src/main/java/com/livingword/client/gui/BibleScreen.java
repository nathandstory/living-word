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
    private boolean searchExpanded;
    private boolean toolsExpanded;
    private boolean highlightedView;
    private EditBox searchBox;
    private Button searchGoButton;
    private Button searchNextButton;
    private Button highlightedTabButton;
    private Button previousBookButton;
    private Button previousChapterButton;
    private Button nextChapterButton;
    private Button nextBookButton;
    private Button versionButton;
    private Button listenButton;
    private Button highlightButton;
    private Button copyButton;
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
        BibleScreenLayout layout = BibleScreenLayout.compute(this.width, this.height, searchExpanded, toolsExpanded);

        addRenderableWidget(Button.builder(Component.translatable(searchExpanded
                ? "gui.livingword.bible.hide_search"
                : "gui.livingword.bible.show_search"), button -> {
                searchExpanded = !searchExpanded;
                rebuildWidgets();
            })
            .bounds(layout.searchToggle().x(), layout.searchToggle().y(), layout.searchToggle().width(), layout.searchToggle().height())
            .build());
        addRenderableWidget(Button.builder(Component.translatable(toolsExpanded
                ? "gui.livingword.bible.hide_tools"
                : "gui.livingword.bible.show_tools"), button -> {
                toolsExpanded = !toolsExpanded;
                rebuildWidgets();
            })
            .bounds(layout.toolsToggle().x(), layout.toolsToggle().y(), layout.toolsToggle().width(), layout.toolsToggle().height())
            .build());
        highlightedTabButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
                highlightedView = !highlightedView;
                verseScrollOffset = 0;
                refreshActionLabels();
            })
            .bounds(layout.highlightedToggle().x(), layout.highlightedToggle().y(), layout.highlightedToggle().width(), layout.highlightedToggle().height())
            .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.close"), button -> onClose())
            .bounds(layout.close().x(), layout.close().y(), layout.close().width(), layout.close().height())
            .build());

        searchBox = new EditBox(this.font, layout.searchBox().x(), layout.searchBox().y(), layout.searchBox().width(), layout.searchBox().height(), Component.translatable("gui.livingword.bible.search"));
        searchBox.setResponder(state::setSearchQuery);
        searchBox.setHint(Component.translatable("gui.livingword.bible.search"));
        searchBox.setValue(state.searchQuery());
        addRenderableWidget(searchBox);

        searchGoButton = addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.search_go"), button -> jumpToFirstSearchResult())
            .bounds(layout.searchGo().x(), layout.searchGo().y(), layout.searchGo().width(), layout.searchGo().height())
            .build());
        searchNextButton = addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.search_next"), button -> jumpToNextSearchResult())
            .bounds(layout.searchNext().x(), layout.searchNext().y(), layout.searchNext().width(), layout.searchNext().height())
            .build());

        versionButton = addRenderableWidget(Button.builder(Component.empty(), button -> navigateTranslation(1))
            .bounds(layout.version().x(), layout.version().y(), layout.version().width(), layout.version().height())
            .build());
        previousBookButton = addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.previous_book"), button -> navigateBook(-1))
            .bounds(layout.previousBook().x(), layout.previousBook().y(), layout.previousBook().width(), layout.previousBook().height())
            .build());
        previousChapterButton = addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.previous_chapter"), button -> navigateChapter(-1))
            .bounds(layout.previousChapter().x(), layout.previousChapter().y(), layout.previousChapter().width(), layout.previousChapter().height())
            .build());
        nextChapterButton = addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.next_chapter"), button -> navigateChapter(1))
            .bounds(layout.nextChapter().x(), layout.nextChapter().y(), layout.nextChapter().width(), layout.nextChapter().height())
            .build());
        nextBookButton = addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.next_book"), button -> navigateBook(1))
            .bounds(layout.nextBook().x(), layout.nextBook().y(), layout.nextBook().width(), layout.nextBook().height())
            .build());
        listenButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
                LivingWordClient.toggleLocalChapter(state.translationId(), state.bookId(), state.chapter());
                refreshActionLabels();
            })
            .bounds(layout.listen().x(), layout.listen().y(), layout.listen().width(), layout.listen().height())
            .build());
        highlightButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
                state.toggleHighlight(state.selectedReference());
                refreshActionLabels();
            })
            .bounds(layout.highlight().x(), layout.highlight().y(), layout.highlight().width(), layout.highlight().height())
            .build());
        copyButton = addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.copy"), button -> copySelectedVerse())
            .bounds(layout.copy().x(), layout.copy().y(), layout.copy().width(), layout.copy().height())
            .build());
        setSearchControlsVisible(searchExpanded);
        setToolControlsVisible(toolsExpanded);
        refreshActionLabels();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        BibleScreenLayout layout = BibleScreenLayout.compute(this.width, this.height, searchExpanded, toolsExpanded);
        BibleScreenLayout.Rect panel = layout.panel();

        graphics.fill(0, 0, this.width, this.height, BACKGROUND);
        graphics.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), PANEL);
        graphics.fill(panel.x() + 8, panel.y() + 8, panel.right() - 8, panel.bottom() - 8, PAGE_DARK);
        graphics.fill(panel.x() + 14, panel.y() + 14, panel.right() - 14, panel.bottom() - 14, PAGE);
        int spineX = panel.x() + panel.width() / 2;
        graphics.fill(spineX - 1, panel.y() + 14, spineX + 1, panel.bottom() - 14, 0x55815F32);
        graphics.fill(panel.x(), panel.y(), panel.right(), panel.y() + 1, BORDER);
        graphics.fill(panel.x(), panel.bottom() - 1, panel.right(), panel.bottom(), BORDER);
        graphics.fill(panel.x(), panel.y(), panel.x() + 1, panel.bottom(), BORDER);
        graphics.fill(panel.right() - 1, panel.y(), panel.right(), panel.bottom(), BORDER);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, panel.y() + 10, TITLE_TEXT);
        renderPassageHeading(graphics, layout);
        if (!state.searchResultSummary().isEmpty()) {
            drawCenteredTrimmed(graphics, state.searchResultSummary(), this.width / 2, layout.statusY(), panel.width() - 80, 0xFF7C5426);
        }
        verseListX = layout.verseList().x();
        verseListY = layout.verseList().y();
        verseListWidth = layout.verseList().width();
        verseListHeight = layout.verseList().height();
        if (highlightedView) {
            int maxScroll = highlightedMaxScroll();
            verseScrollOffset = Math.max(0, Math.min(verseScrollOffset, maxScroll));
            renderHighlightedVerses(graphics);
            renderScrollBar(graphics, maxScroll);
        } else {
            currentChapter().ifPresent(chapter -> {
                verseScrollOffset = clampScroll(chapter, verseScrollOffset);
                verseList.render(graphics, this.font, chapter, state, verseListX, verseListY, verseListWidth, verseListHeight, verseScrollOffset);
                renderScrollBar(graphics, verseList.maxScroll(chapter, this.font, verseListWidth, verseListHeight));
            });
        }
        refreshActionLabels();
        renderBibleWidgets(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (highlightedView && isInsideVerseList(mouseX, mouseY)) {
            Optional<BibleReference> highlighted = highlightedReferenceAt(mouseY);
            highlighted.ifPresent(reference -> {
                highlightedView = false;
                navigateTo(reference);
            });
            return highlighted.isPresent();
        }
        Optional<ChapterData> chapter = currentChapter();
        if (chapter.isPresent() && isInsideVerseList(mouseX, mouseY)) {
            var selectedVerse = verseList.verseAt(chapter.get(), this.font, verseListWidth, verseListY, mouseY, verseScrollOffset);
            if (selectedVerse.isPresent()) {
                state.selectVerse(selectedVerse.getAsInt());
                recordCurrentHistory();
                refreshActionLabels();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isInsideVerseList(mouseX, mouseY)) {
            if (highlightedView) {
                int nextOffset = verseScrollOffset - (int) Math.round(scrollY * 28);
                verseScrollOffset = Math.max(0, Math.min(nextOffset, highlightedMaxScroll()));
            } else {
                currentChapter().ifPresent(chapter -> {
                    int nextOffset = verseScrollOffset - (int) Math.round(scrollY * 28);
                    verseScrollOffset = clampScroll(chapter, nextOffset);
                });
            }
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
        state.currentSearchResult().ifPresent(reference -> {
            highlightedView = false;
            navigateTo(reference);
        });
    }

    private void jumpToNextSearchResult() {
        if (state.currentSearchResult().isEmpty()) {
            jumpToFirstSearchResult();
            return;
        }
        state.advanceSearchResult(1);
        state.currentSearchResult().ifPresent(reference -> {
            highlightedView = false;
            navigateTo(reference);
        });
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
        refreshActionLabels();
    }

    private void setPassage(String translationId, String bookId, int chapter) {
        state.setPassage(translationId, bookId, chapter);
        verseScrollOffset = 0;
        currentChapter().ifPresent(this::selectFirstVerse);
        recordCurrentHistory();
        refreshActionLabels();
    }

    private Optional<ChapterData> currentChapter() {
        return dataManager.getChapter(state.translationId(), state.bookId(), state.chapter());
    }

    private void renderPassageHeading(GuiGraphics graphics, BibleScreenLayout layout) {
        BibleScreenLayout.Rect panel = layout.panel();
        drawCenteredTrimmed(graphics, currentBookChapterHeading(), this.width / 2, layout.headingBookY(), panel.width() - 80, TEXT);
        drawCenteredTrimmed(graphics, currentTranslationHeading(), this.width / 2, layout.headingTranslationY(), panel.width() - 100, 0xFF7C5426);
    }

    private String currentBookChapterHeading() {
        if (highlightedView) {
            return Component.translatable("gui.livingword.bible.highlighted").getString();
        }
        return formatBookId(state.bookId()) + " " + state.chapter();
    }

    private String currentTranslationHeading() {
        if (highlightedView) {
            return Component.translatable("gui.livingword.bible.highlighted_count", state.highlightCount()).getString();
        }
        String translationName = dataManager.getTranslation(state.translationId())
            .map(TranslationManifest::displayName)
            .orElse(state.translationId().toUpperCase(java.util.Locale.ROOT));
        return translationName;
    }

    private void recordCurrentHistory() {
        state.recordHistory(state.selectedReference());
    }

    private void restoreStoredState() {
        BibleClientPreferences.StoredBibleState storedState = BibleClientPreferences.load(preferencesPath);
        state.replaceBookmarks(storedState.bookmarks());
        state.replaceHighlights(storedState.highlights());
        state.replaceRecentHistory(storedState.recentHistory());
        storedState.lastReference()
            .filter(reference -> dataManager.getChapter(reference.translationId(), reference.bookId(), reference.chapter()).isPresent())
            .ifPresent(this::navigateTo);
    }

    private void persistState() {
        BibleClientPreferences.save(
            preferencesPath,
            new BibleClientPreferences.StoredBibleState(Optional.of(state.selectedReference()), state.bookmarks(), state.recentHistory(), state.highlights())
        );
    }

    private void selectFirstVerse(ChapterData chapter) {
        chapter.verses().keySet().stream().min(Integer::compareTo).ifPresentOrElse(state::selectVerse, () -> state.selectVerse(1));
    }

    private void refreshActionLabels() {
        if (highlightedTabButton != null) {
            highlightedTabButton.setMessage(Component.translatable(highlightedView
                ? "gui.livingword.bible.show_reading"
                : "gui.livingword.bible.show_highlighted"));
        }
        if (versionButton != null) {
            versionButton.setMessage(Component.literal(state.translationId().toUpperCase(java.util.Locale.ROOT)));
        }
        if (listenButton != null) {
            listenButton.setMessage(Component.translatable(LivingWordClient.isLocalBibleChapterActive(state.translationId(), state.bookId(), state.chapter())
                ? "gui.livingword.bible.stop_listen"
                : "gui.livingword.bible.listen"));
        }
        if (highlightButton != null) {
            highlightButton.setMessage(Component.translatable(state.isSelectedVerseHighlighted()
                ? "gui.livingword.bible.unhighlight"
                : "gui.livingword.bible.highlight"));
        }
    }

    private void setSearchControlsVisible(boolean visible) {
        if (searchBox != null) {
            searchBox.visible = visible;
            searchBox.active = visible;
            if (!visible && getFocused() == searchBox) {
                setFocused(null);
            }
        }
        setButtonVisible(searchGoButton, visible);
        setButtonVisible(searchNextButton, visible);
    }

    private void setToolControlsVisible(boolean visible) {
        setButtonVisible(versionButton, visible);
        setButtonVisible(previousBookButton, visible);
        setButtonVisible(previousChapterButton, visible);
        setButtonVisible(nextChapterButton, visible);
        setButtonVisible(nextBookButton, visible);
        setButtonVisible(listenButton, visible);
        setButtonVisible(highlightButton, visible);
        setButtonVisible(copyButton, visible);
    }

    private static void setButtonVisible(Button button, boolean visible) {
        if (button != null) {
            button.visible = visible;
            button.active = visible;
        }
    }

    private void drawCenteredTrimmed(GuiGraphics graphics, String text, int centerX, int y, int maxWidth, int color) {
        String rendered = trimToWidth(text, maxWidth);
        graphics.drawString(this.font, rendered, centerX - this.font.width(rendered) / 2, y, color, false);
    }

    private String trimToWidth(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int available = Math.max(0, maxWidth - this.font.width(suffix));
        return this.font.plainSubstrByWidth(text, available) + suffix;
    }

    private boolean isInsideVerseList(double mouseX, double mouseY) {
        return mouseX >= verseListX && mouseX <= verseListX + verseListWidth && mouseY >= verseListY && mouseY <= verseListY + verseListHeight;
    }

    private int clampScroll(ChapterData chapter, int scrollOffset) {
        int maxScroll = verseList.maxScroll(chapter, this.font, verseListWidth, verseListHeight);
        return Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    private void renderHighlightedVerses(GuiGraphics graphics) {
        graphics.enableScissor(verseListX, verseListY, verseListX + verseListWidth, verseListY + verseListHeight);
        int lineY = verseListY - verseScrollOffset;
        for (BibleReference reference : state.highlights()) {
            if (lineY + 14 >= verseListY && lineY <= verseListY + verseListHeight) {
                graphics.fill(verseListX + 2, lineY - 1, verseListX + verseListWidth - 6, lineY + 13, 0x55E7B844);
                drawTrimmed(
                    graphics,
                    formatReference(reference) + "  " + dataManager.getVerse(reference).orElse(""),
                    verseListX + 8,
                    lineY,
                    verseListWidth - 20,
                    TEXT
                );
            }
            lineY += 16;
        }
        if (state.highlights().isEmpty()) {
            drawCenteredTrimmed(graphics, Component.translatable("gui.livingword.bible.no_highlights").getString(), this.width / 2, verseListY + 18, verseListWidth - 16, 0xFF7C5426);
        }
        graphics.disableScissor();
    }

    private Optional<BibleReference> highlightedReferenceAt(double mouseY) {
        int index = (((int) mouseY - verseListY) + verseScrollOffset) / 16;
        if (index < 0 || index >= state.highlights().size()) {
            return Optional.empty();
        }
        return Optional.of(state.highlights().get(index));
    }

    private int highlightedMaxScroll() {
        return Math.max(0, state.highlights().size() * 16 - verseListHeight);
    }

    private void renderScrollBar(GuiGraphics graphics, int maxScroll) {
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

    private void drawTrimmed(GuiGraphics graphics, String text, int x, int y, int maxWidth, int color) {
        graphics.drawString(this.font, trimToWidth(text, maxWidth), x, y, color, false);
    }

    private String formatReference(BibleReference reference) {
        return formatBookId(reference.bookId()) + " " + reference.chapter() + ":" + reference.verse();
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

}
