package com.dashclient.modules;

import com.dashclient.DashClient;
import com.dashclient.Module;
import net.minecraft.client.Minecraft;

public final class ZoomModule extends Module {
    private Integer originalFov;

    public ZoomModule() { super("Zoom"); }

    @Override public void onTick(Minecraft c) {
        boolean held = c.gui.screen() == null && DashClient.ZOOM_KEY.isDown();
        if (held && originalFov == null) {
            originalFov = c.options.fov().get();
            c.options.fov().set(30);
        } else if (!held) {
            restore(c);
        }
    }

    private void restore(Minecraft c) {
        if (originalFov == null) return;
        c.options.fov().set(originalFov);
        originalFov = null;
    }

    @Override public void onDisable() { restore(Minecraft.getInstance()); }
}
