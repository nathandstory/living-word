package com.livingword.client;

import com.livingword.client.gui.BibleScreen;
import net.minecraft.client.Minecraft;

public final class LivingWordClient {
    private LivingWordClient() {
    }

    public static void openBibleScreen() {
        Minecraft.getInstance().setScreen(new BibleScreen());
    }
}
