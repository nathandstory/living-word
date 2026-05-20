package com.livingword.client.gui;

import com.livingword.bible.BibleDataManager;
import com.livingword.bible.TranslationManifest;
import com.livingword.client.BibleClientRepository;
import com.livingword.client.LivingWordClient;
import com.livingword.discs.ScriptureDiscSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

public final class ScriptureDiscSelectionScreen extends Screen {
    private static final int BACKGROUND = 0xF0181510;
    private static final int PANEL = 0xF02B2118;
    private static final int PAGE = 0xFFE6D0A3;
    private static final int BORDER = 0xFF8C6A3E;
    private static final int TEXT = 0xFF3B2A18;
    private static final int TITLE_TEXT = 0xFFFFE3AD;

    private final InteractionHand hand;
    private final BibleDataManager dataManager = BibleClientRepository.dataManager();

    private String translationId;
    private String bookId;
    private int chapter;

    private Button translationButton;
    private Button bookButton;
    private Button chapterButton;

    public ScriptureDiscSelectionScreen(InteractionHand hand) {
        super(Component.translatable("gui.livingword.disc.title"));
        this.hand = hand;
        ScriptureDiscSelection selection = currentSelection(hand);
        this.translationId = selection.translationId();
        this.bookId = selection.bookId();
        this.chapter = selection.chapter();
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(360, this.width - 24);
        int panelHeight = Math.min(210, this.height - 24);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int buttonWidth = Math.min(140, panelWidth - 40);
        int centerX = left + panelWidth / 2;

        translationButton = addRenderableWidget(Button.builder(Component.empty(), button -> navigateTranslation(1))
            .bounds(centerX - buttonWidth / 2, top + 52, buttonWidth, 20)
            .build());
        bookButton = addRenderableWidget(Button.builder(Component.empty(), button -> navigateBook(1))
            .bounds(centerX - buttonWidth / 2, top + 82, buttonWidth, 20)
            .build());
        chapterButton = addRenderableWidget(Button.builder(Component.empty(), button -> navigateChapter(1))
            .bounds(centerX - buttonWidth / 2, top + 112, buttonWidth, 20)
            .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.livingword.disc.save"), button -> saveAndClose())
            .bounds(centerX - buttonWidth / 2, top + panelHeight - 54, buttonWidth, 20)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.close"), button -> onClose())
            .bounds(centerX - buttonWidth / 2, top + panelHeight - 28, buttonWidth, 20)
            .build());
        refreshLabels();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int panelWidth = Math.min(360, this.width - 24);
        int panelHeight = Math.min(210, this.height - 24);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        graphics.fill(0, 0, this.width, this.height, BACKGROUND);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, PANEL);
        graphics.fill(left + 10, top + 10, left + panelWidth - 10, top + panelHeight - 10, PAGE);
        graphics.fill(left, top, left + panelWidth, top + 1, BORDER);
        graphics.fill(left, top + panelHeight - 1, left + panelWidth, top + panelHeight, BORDER);
        graphics.fill(left, top, left + 1, top + panelHeight, BORDER);
        graphics.fill(left + panelWidth - 1, top, left + panelWidth, top + panelHeight, BORDER);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 18, TITLE_TEXT);
        graphics.drawCenteredString(this.font, formatSelection(), this.width / 2, top + 36, TEXT);
        super.render(graphics, mouseX, mouseY, partialTick);
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
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
        SelectionCycle.next(translations, translationId, direction).ifPresent(nextTranslationId -> translationId = nextTranslationId);
        if (dataManager.getChapter(translationId, bookId, chapter).isEmpty()) {
            dataManager.firstChapter(translationId).ifPresent(first -> {
                bookId = first.bookId();
                chapter = first.chapter();
            });
        }
        refreshLabels();
    }

    private void navigateBook(int direction) {
        List<String> books = dataManager.bookIds(translationId);
        SelectionCycle.next(books, bookId, direction).ifPresent(nextBookId -> bookId = nextBookId);
        List<Integer> chapters = dataManager.chapters(translationId, bookId);
        chapter = chapters.isEmpty() ? 1 : chapters.getFirst();
        refreshLabels();
    }

    private void navigateChapter(int direction) {
        List<Integer> chapters = dataManager.chapters(translationId, bookId);
        SelectionCycle.next(chapters, chapter, direction).ifPresent(nextChapter -> chapter = nextChapter);
        refreshLabels();
    }

    private void saveAndClose() {
        LivingWordClient.configureScriptureDisc(hand, new ScriptureDiscSelection(translationId, bookId, chapter));
        onClose();
    }

    private void refreshLabels() {
        if (translationButton != null) {
            translationButton.setMessage(Component.literal(translationId.toUpperCase(Locale.ROOT)));
        }
        if (bookButton != null) {
            bookButton.setMessage(Component.literal(formatBookId(bookId)));
        }
        if (chapterButton != null) {
            chapterButton.setMessage(Component.literal("Chapter " + chapter));
        }
    }

    private String formatSelection() {
        return translationId.toUpperCase(Locale.ROOT) + " / " + formatBookId(bookId) + " " + chapter;
    }

    private static ScriptureDiscSelection currentSelection(InteractionHand hand) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return ScriptureDiscSelection.defaults();
        }
        ItemStack stack = minecraft.player.getItemInHand(hand);
        return ScriptureDiscSelection.from(stack);
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
