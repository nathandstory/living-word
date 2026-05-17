package com.livingword.discs;

import net.minecraft.world.item.Item;

public final class ScriptureDiscRegistry {
    private ScriptureDiscRegistry() {
    }

    public static ScriptureDisc johnDisc(Item.Properties properties) {
        return new ScriptureDisc(properties, "item.livingword.scripture_disc_john.desc", "kjv", "john", 1, 21, "kjv-default");
    }
}
