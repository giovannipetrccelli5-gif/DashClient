package com.dashclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public abstract class Module {
    private final String name;
    private boolean enabled;

    protected Module(String name) {
        this.name = name;
    }

    public final String name() { return name; }
    public final boolean enabled() { return enabled; }

    public final void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        if (value) onEnable();
        else onDisable();
        HudFrameCache.invalidate();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick(Minecraft client) {}
    public String hudText(Minecraft client) { return null; }

    // Non-text modules have a rendered icon area with their own dimensions.
    public boolean customHud() { return false; }
    public int hudWidth(Minecraft client) { return 0; }
    public int hudHeight(Minecraft client) { return 12; }
    public void renderHud(Minecraft client, GuiGraphicsExtractor graphics, int x, int y) {}

}
