package com.livingword.client.gui;

import com.livingword.bible.BibleDataManager;
import com.livingword.bible.BibleReference;
import com.livingword.bible.ChapterData;
import com.livingword.bible.TranslationManifest;
import com.livingword.client.BibleClientPreferences;
import com.livingword.client.BibleClientRepository;
import com.livingword.client.LivingWordClient;
import com.livingword.client.gui.widgets.VerseListWidget;
import com.livingword.client.study.AudioQueueEntry;
import com.livingword.client.study.VerseCollection;
import com.livingword.client.study.VerseNote;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
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
    private EditBox searchBox;
    private EditBox noteBox;
    private Button searchGoButton;
    private Button searchNextButton;
    private Button viewButton;
    private Button previousBookButton;
    private Button previousChapterButton;
    private Button nextChapterButton;
    private Button nextBookButton;
    private Button versionButton;
    private Button previousAudioButton;
    private Button listenButton;
    private Button stopAudioButton;
    private Button nextAudioButton;
    private Button highlightButton;
    private Button collectionButton;
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
        state.replaceAudioQueue(audioQueueForCurrentBook());
    }

    @Override
    protected void init() {
        BibleScreenLayout layout = BibleScreenLayout.compute(this.width, this.height, searchRowVisible(), toolsExpanded);

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
        viewButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
                cycleReaderView();
                verseScrollOffset = 0;
                rebuildWidgets();
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

        noteBox = new EditBox(this.font, layout.searchBox().x(), layout.searchBox().y(), layout.searchBox().width(), layout.searchBox().height(), Component.translatable("gui.livingword.bible.note"));
        noteBox.setHint(Component.translatable("gui.livingword.bible.note"));
        noteBox.setValue(state.noteFor(state.selectedReference()).orElse(""));
        noteBox.setResponder(value -> state.setNote(state.selectedReference(), value));
        addRenderableWidget(noteBox);

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
        previousAudioButton = addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.audio_previous"), button -> previousQueuedAudio())
            .bounds(layout.previousAudio().x(), layout.previousAudio().y(), layout.previousAudio().width(), layout.previousAudio().height())
            .build());
        listenButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
                playQueuedAudio();
                refreshActionLabels();
            })
            .bounds(layout.listen().x(), layout.listen().y(), layout.listen().width(), layout.listen().height())
            .build());
        stopAudioButton = addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.audio_stop"), button -> {
                LivingWordClient.stopLocalPlayback();
                refreshActionLabels();
            })
            .bounds(layout.stopAudio().x(), layout.stopAudio().y(), layout.stopAudio().width(), layout.stopAudio().height())
            .build());
        nextAudioButton = addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.audio_next"), button -> nextQueuedAudio())
            .bounds(layout.nextAudio().x(), layout.nextAudio().y(), layout.nextAudio().width(), layout.nextAudio().height())
            .build());
        highlightButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
                state.toggleHighlight(state.selectedReference());
                refreshActionLabels();
            })
            .bounds(layout.highlight().x(), layout.highlight().y(), layout.highlight().width(), layout.highlight().height())
            .build());
        collectionButton = addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.add_collection"), button -> {
                state.addToCollection(Component.translatable("gui.livingword.bible.default_collection").getString(), state.selectedReference());
                state.showCollections();
                verseScrollOffset = 0;
                rebuildWidgets();
            })
            .bounds(layout.collection().x(), layout.collection().y(), layout.collection().width(), layout.collection().height())
            .build());
        copyButton = addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.copy"), button -> copySelectedVerse())
            .bounds(layout.copy().x(), layout.copy().y(), layout.copy().width(), layout.copy().height())
            .build());
        setSearchControlsVisible(searchExpanded);
        setToolControlsVisible(toolsExpanded);
        refreshNoteBox();
        refreshActionLabels();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        BibleScreenLayout layout = BibleScreenLayout.compute(this.width, this.height, searchRowVisible(), toolsExpanded);
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
        switch (state.readerView()) {
            case SEARCH -> {
                int maxScroll = referenceRowsMaxScroll(state.searchResults().size());
                verseScrollOffset = Math.max(0, Math.min(verseScrollOffset, maxScroll));
                renderSearchResults(graphics);
                renderScrollBar(graphics, maxScroll);
            }
            case HIGHLIGHTED -> {
                int maxScroll = referenceRowsMaxScroll(state.highlights().size());
                verseScrollOffset = Math.max(0, Math.min(verseScrollOffset, maxScroll));
                renderHighlightedVerses(graphics);
                renderScrollBar(graphics, maxScroll);
            }
            case NOTES -> {
                int maxScroll = referenceRowsMaxScroll(state.notes().size());
                verseScrollOffset = Math.max(0, Math.min(verseScrollOffset, maxScroll));
                renderNotes(graphics);
                renderScrollBar(graphics, maxScroll);
            }
            case COLLECTIONS -> {
                int maxScroll = referenceRowsMaxScroll(collectionReferences().size());
                verseScrollOffset = Math.max(0, Math.min(verseScrollOffset, maxScroll));
                renderCollections(graphics);
                renderScrollBar(graphics, maxScroll);
            }
            case READING -> currentChapter().ifPresent(chapter -> {
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
        if (state.readerView() != BibleGuiState.ReaderView.READING && isInsideVerseList(mouseX, mouseY)) {
            Optional<BibleReference> reference = referenceRowAt(mouseY);
            reference.ifPresent(selected -> {
                state.showReading();
                navigateTo(selected);
                rebuildWidgets();
            });
            return reference.isPresent();
        }
        Optional<ChapterData> chapter = currentChapter();
        if (chapter.isPresent() && isInsideVerseList(mouseX, mouseY)) {
            var selectedVerse = verseList.verseAt(chapter.get(), this.font, verseListWidth, verseListY, mouseY, verseScrollOffset);
            if (selectedVerse.isPresent()) {
                state.selectVerse(selectedVerse.getAsInt());
                recordCurrentHistory();
                refreshNoteBox();
                refreshActionLabels();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isInsideVerseList(mouseX, mouseY)) {
            if (state.readerView() != BibleGuiState.ReaderView.READING) {
                int nextOffset = verseScrollOffset - (int) Math.round(scrollY * 28);
                verseScrollOffset = Math.max(0, Math.min(nextOffset, referenceRowsMaxScroll(referencesForCurrentView().size())));
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
        state.showSearchResults();
        verseScrollOffset = 0;
        refreshNoteBox();
        refreshActionLabels();
    }

    private void jumpToNextSearchResult() {
        if (state.currentSearchResult().isEmpty()) {
            jumpToFirstSearchResult();
            return;
        }
        state.advanceSearchResult(1);
        state.currentSearchResult().ifPresent(reference -> {
            state.showReading();
            navigateTo(reference);
            rebuildWidgets();
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
        state.replaceAudioQueue(audioQueueForCurrentBook());
        recordCurrentHistory();
        refreshNoteBox();
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
        return switch (state.readerView()) {
            case SEARCH -> Component.translatable("gui.livingword.bible.search_results").getString();
            case HIGHLIGHTED -> Component.translatable("gui.livingword.bible.highlighted").getString();
            case NOTES -> Component.translatable("gui.livingword.bible.notes").getString();
            case COLLECTIONS -> Component.translatable("gui.livingword.bible.collections").getString();
            case READING -> formatBookId(state.bookId()) + " " + state.chapter();
        };
    }

    private String currentTranslationHeading() {
        switch (state.readerView()) {
            case SEARCH -> {
                if (!state.searchResultSummary().isEmpty()) {
                    return Component.translatable("gui.livingword.bible.search_count", state.searchResultSummary()).getString();
                }
                return Component.translatable("gui.livingword.bible.search_prompt").getString();
            }
            case HIGHLIGHTED -> {
                return Component.translatable("gui.livingword.bible.highlighted_count", state.highlightCount()).getString();
            }
            case NOTES -> {
                return Component.translatable("gui.livingword.bible.notes_count", state.notes().size()).getString();
            }
            case COLLECTIONS -> {
                return Component.translatable("gui.livingword.bible.collections_count", state.collections().size()).getString();
            }
            case READING -> {
                String translationName = dataManager.getTranslation(state.translationId())
                    .map(TranslationManifest::displayName)
                    .orElse(state.translationId().toUpperCase(java.util.Locale.ROOT));
                String queueSummary = state.audioQueueSummary();
                return queueSummary.isBlank() ? translationName : translationName + "  Audio " + queueSummary;
            }
        }
        throw new IllegalStateException("Unhandled reader view: " + state.readerView());
    }

    private void recordCurrentHistory() {
        state.recordHistory(state.selectedReference());
    }

    private void restoreStoredState() {
        BibleClientPreferences.StoredBibleState storedState = BibleClientPreferences.load(preferencesPath);
        state.replaceBookmarks(storedState.bookmarks());
        state.replaceHighlights(storedState.highlights());
        state.replaceNotes(storedState.notes());
        state.replaceCollections(storedState.collections());
        state.replaceRecentHistory(storedState.recentHistory());
        storedState.lastReference()
            .filter(reference -> dataManager.getChapter(reference.translationId(), reference.bookId(), reference.chapter()).isPresent())
            .ifPresent(this::navigateTo);
    }

    private void persistState() {
        BibleClientPreferences.save(
            preferencesPath,
            new BibleClientPreferences.StoredBibleState(
                Optional.of(state.selectedReference()),
                state.bookmarks(),
                state.recentHistory(),
                state.highlights(),
                state.notes(),
                state.collections()
            )
        );
    }

    private void selectFirstVerse(ChapterData chapter) {
        chapter.verses().keySet().stream().min(Integer::compareTo).ifPresentOrElse(state::selectVerse, () -> state.selectVerse(1));
    }

    private void refreshActionLabels() {
        if (viewButton != null) {
            viewButton.setMessage(Component.translatable("gui.livingword.bible.view_button", readerViewName(state.readerView())));
        }
        if (versionButton != null) {
            versionButton.setMessage(Component.literal(state.translationId().toUpperCase(java.util.Locale.ROOT)));
        }
        if (listenButton != null) {
            AudioQueueEntry queued = state.currentQueuedChapter().orElse(new AudioQueueEntry(state.translationId(), state.bookId(), state.chapter()));
            listenButton.setMessage(Component.translatable(LivingWordClient.isLocalBibleChapterActive(queued.translationId(), queued.bookId(), queued.chapter())
                ? "gui.livingword.bible.stop_listen"
                : "gui.livingword.bible.listen"));
        }
        if (highlightButton != null) {
            highlightButton.setMessage(Component.translatable(state.isSelectedVerseHighlighted()
                ? "gui.livingword.bible.unhighlight"
                : "gui.livingword.bible.highlight"));
        }
        refreshNoteBox();
    }

    private boolean searchRowVisible() {
        return searchExpanded || state.readerView() == BibleGuiState.ReaderView.NOTES;
    }

    private void cycleReaderView() {
        switch (state.readerView()) {
            case READING -> state.showSearchResults();
            case SEARCH -> state.showHighlighted();
            case HIGHLIGHTED -> state.showNotes();
            case NOTES -> state.showCollections();
            case COLLECTIONS -> state.showReading();
        }
        refreshNoteBox();
    }

    private void playQueuedAudio() {
        AudioQueueEntry entry = ensureQueuedAudio().orElse(new AudioQueueEntry(state.translationId(), state.bookId(), state.chapter()));
        LivingWordClient.toggleLocalChapter(entry.translationId(), entry.bookId(), entry.chapter());
        refreshActionLabels();
    }

    private void previousQueuedAudio() {
        ensureQueuedAudio();
        state.advanceAudioQueue(-1);
        state.currentQueuedChapter().ifPresent(entry -> {
            LivingWordClient.playLocalChapter(entry.translationId(), entry.bookId(), entry.chapter());
            setPassageWithoutResettingAudioQueue(entry.translationId(), entry.bookId(), entry.chapter());
        });
    }

    private void nextQueuedAudio() {
        ensureQueuedAudio();
        state.advanceAudioQueue(1);
        state.currentQueuedChapter().ifPresent(entry -> {
            LivingWordClient.playLocalChapter(entry.translationId(), entry.bookId(), entry.chapter());
            setPassageWithoutResettingAudioQueue(entry.translationId(), entry.bookId(), entry.chapter());
        });
    }

    private Optional<AudioQueueEntry> ensureQueuedAudio() {
        if (state.currentQueuedChapter().isEmpty()) {
            state.replaceAudioQueue(audioQueueForCurrentContext());
        }
        return state.currentQueuedChapter();
    }

    private List<AudioQueueEntry> audioQueueForCurrentContext() {
        List<BibleReference> references = referencesForCurrentView();
        if (state.readerView() != BibleGuiState.ReaderView.READING && !references.isEmpty()) {
            List<AudioQueueEntry> entries = new ArrayList<>();
            for (BibleReference reference : references) {
                AudioQueueEntry entry = new AudioQueueEntry(reference.translationId(), reference.bookId(), reference.chapter());
                if (!entries.contains(entry)) {
                    entries.add(entry);
                }
            }
            return entries;
        }
        return audioQueueForCurrentBook();
    }

    private List<AudioQueueEntry> audioQueueForCurrentBook() {
        List<AudioQueueEntry> entries = new ArrayList<>();
        for (int chapterNumber : dataManager.chapters(state.translationId(), state.bookId())) {
            if (chapterNumber >= state.chapter()) {
                entries.add(new AudioQueueEntry(state.translationId(), state.bookId(), chapterNumber));
            }
        }
        if (entries.isEmpty()) {
            entries.add(new AudioQueueEntry(state.translationId(), state.bookId(), state.chapter()));
        }
        return entries;
    }

    private void setPassageWithoutResettingAudioQueue(String translationId, String bookId, int chapter) {
        state.setPassage(translationId, bookId, chapter);
        verseScrollOffset = 0;
        currentChapter().ifPresent(this::selectFirstVerse);
        recordCurrentHistory();
        refreshNoteBox();
        refreshActionLabels();
    }

    private void refreshNoteBox() {
        if (noteBox == null) {
            return;
        }
        boolean visible = state.readerView() == BibleGuiState.ReaderView.NOTES && !searchExpanded;
        noteBox.visible = visible;
        noteBox.active = visible;
        if (!visible && getFocused() == noteBox) {
            setFocused(null);
        }
        String currentValue = state.noteFor(state.selectedReference()).orElse("");
        if (!noteBox.getValue().equals(currentValue)) {
            noteBox.setValue(currentValue);
        }
    }

    private static String readerViewName(BibleGuiState.ReaderView view) {
        return switch (view) {
            case READING -> Component.translatable("gui.livingword.bible.reading").getString();
            case SEARCH -> Component.translatable("gui.livingword.bible.search_results").getString();
            case HIGHLIGHTED -> Component.translatable("gui.livingword.bible.highlighted").getString();
            case NOTES -> Component.translatable("gui.livingword.bible.notes").getString();
            case COLLECTIONS -> Component.translatable("gui.livingword.bible.collections").getString();
        };
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
        refreshNoteBox();
    }

    private void setToolControlsVisible(boolean visible) {
        setButtonVisible(versionButton, visible);
        setButtonVisible(previousBookButton, visible);
        setButtonVisible(previousChapterButton, visible);
        setButtonVisible(nextChapterButton, visible);
        setButtonVisible(nextBookButton, visible);
        setButtonVisible(previousAudioButton, visible);
        setButtonVisible(listenButton, visible);
        setButtonVisible(stopAudioButton, visible);
        setButtonVisible(nextAudioButton, visible);
        setButtonVisible(highlightButton, visible);
        setButtonVisible(collectionButton, visible);
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
        renderReferenceRows(graphics, state.highlights(), Component.translatable("gui.livingword.bible.no_highlights").getString(), true);
    }

    private void renderSearchResults(GuiGraphics graphics) {
        renderReferenceRows(graphics, state.searchResults(), Component.translatable("gui.livingword.bible.no_search_results").getString(), false);
    }

    private void renderNotes(GuiGraphics graphics) {
        List<BibleReference> references = state.notes().stream().map(VerseNote::reference).toList();
        renderReferenceRows(graphics, references, Component.translatable("gui.livingword.bible.no_notes").getString(), false);
    }

    private void renderCollections(GuiGraphics graphics) {
        renderReferenceRows(graphics, collectionReferences(), Component.translatable("gui.livingword.bible.no_collections").getString(), false);
    }

    private void renderReferenceRows(GuiGraphics graphics, List<BibleReference> references, String emptyMessage, boolean highlightRows) {
        graphics.enableScissor(verseListX, verseListY, verseListX + verseListWidth, verseListY + verseListHeight);
        int lineY = verseListY - verseScrollOffset;
        for (BibleReference reference : references) {
            if (lineY + 14 >= verseListY && lineY <= verseListY + verseListHeight) {
                if (highlightRows || state.isHighlighted(reference)) {
                    graphics.fill(verseListX + 2, lineY - 1, verseListX + verseListWidth - 6, lineY + 13, 0x55E7B844);
                }
                drawTrimmed(
                    graphics,
                    rowText(reference),
                    verseListX + 8,
                    lineY,
                    verseListWidth - 20,
                    TEXT
                );
            }
            lineY += 16;
        }
        if (references.isEmpty()) {
            drawCenteredTrimmed(graphics, emptyMessage, this.width / 2, verseListY + 18, verseListWidth - 16, 0xFF7C5426);
        }
        graphics.disableScissor();
    }

    private Optional<BibleReference> referenceRowAt(double mouseY) {
        int index = (((int) mouseY - verseListY) + verseScrollOffset) / 16;
        List<BibleReference> references = referencesForCurrentView();
        if (index < 0 || index >= references.size()) {
            return Optional.empty();
        }
        return Optional.of(references.get(index));
    }

    private int referenceRowsMaxScroll(int rows) {
        return Math.max(0, rows * 16 - verseListHeight);
    }

    private List<BibleReference> referencesForCurrentView() {
        return switch (state.readerView()) {
            case SEARCH -> state.searchResults();
            case HIGHLIGHTED -> state.highlights();
            case NOTES -> state.notes().stream().map(VerseNote::reference).toList();
            case COLLECTIONS -> collectionReferences();
            case READING -> List.of();
        };
    }

    private List<BibleReference> collectionReferences() {
        List<BibleReference> references = new ArrayList<>();
        for (VerseCollection collection : state.collections()) {
            for (BibleReference reference : collection.references()) {
                references.add(reference);
            }
        }
        return List.copyOf(references);
    }

    private String rowText(BibleReference reference) {
        String baseText = formatReference(reference) + "  " + dataManager.getVerse(reference).orElse("");
        if (state.readerView() == BibleGuiState.ReaderView.NOTES) {
            return state.noteFor(reference).map(note -> baseText + "  -  " + note).orElse(baseText);
        }
        if (state.readerView() == BibleGuiState.ReaderView.COLLECTIONS) {
            String collectionNames = state.collections().stream()
                .filter(collection -> collection.references().contains(reference))
                .map(VerseCollection::name)
                .distinct()
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
            if (!collectionNames.isBlank()) {
                return "[" + collectionNames + "] " + baseText;
            }
        }
        return baseText;
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
