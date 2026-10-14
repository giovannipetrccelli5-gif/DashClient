package com.dashclient;

import net.minecraft.client.Minecraft;

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
}
