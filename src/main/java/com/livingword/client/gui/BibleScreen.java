package com.livingword.client.gui;

import com.livingword.bible.ChapterData;
import com.livingword.client.gui.widgets.VerseListWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Map;

public final class BibleScreen extends Screen {
    private static final int BACKGROUND = 0xF0181510;
    private static final int PANEL = 0xF02B2118;
    private static final int BORDER = 0xFF8C6A3E;
    private static final int TEXT = 0xFFE8D7B5;

    private final BibleGuiState state = BibleGuiState.initial("kjv", "john", 3);
    private final VerseListWidget verseList = new VerseListWidget();
    private final ChapterData sampleChapter = new ChapterData(
        "kjv",
        "john",
        3,
        Map.of(
            16,
            "For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life.",
            17,
            "For God sent not his Son into the world to condemn the world; but that the world through him might be saved."
        )
    );
    private EditBox searchBox;

    public BibleScreen() {
        super(Component.translatable("gui.livingword.bible.title"));
        state.selectVerse(16);
        state.recordHistory(state.selectedReference());
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(360, this.width - 32);
        int left = (this.width - panelWidth) / 2;
        int top = 24;

        searchBox = new EditBox(this.font, left + 16, top + 34, panelWidth - 32, 20, Component.translatable("gui.livingword.bible.search"));
        searchBox.setResponder(state::setSearchQuery);
        searchBox.setHint(Component.translatable("gui.livingword.bible.search"));
        addRenderableWidget(searchBox);

        addRenderableWidget(Button.builder(Component.translatable("gui.livingword.bible.copy"), button -> copySelectedVerse())
            .bounds(left + panelWidth - 96, this.height - 40, 80, 20)
            .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int panelWidth = Math.min(360, this.width - 32);
        int panelHeight = Math.min(240, this.height - 48);
        int left = (this.width - panelWidth) / 2;
        int top = 24;

        graphics.fill(0, 0, this.width, this.height, BACKGROUND);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, PANEL);
        graphics.fill(left, top, left + panelWidth, top + 1, BORDER);
        graphics.fill(left, top + panelHeight - 1, left + panelWidth, top + panelHeight, BORDER);
        graphics.fill(left, top, left + 1, top + panelHeight, BORDER);
        graphics.fill(left + panelWidth - 1, top, left + panelWidth, top + panelHeight, BORDER);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 10, TEXT);
        graphics.drawString(this.font, "KJV / John 3", left + 16, top + 62, TEXT, false);
        verseList.render(graphics, this.font, sampleChapter, state, left + 16, top + 82, panelWidth - 32);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void copySelectedVerse() {
        sampleChapter.getVerse(state.selectedVerse()).ifPresent(text -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.keyboardHandler.setClipboard("John 3:" + state.selectedVerse() + " " + text);
        });
    }
}
