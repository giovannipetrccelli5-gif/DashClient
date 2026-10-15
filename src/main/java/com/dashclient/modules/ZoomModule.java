package com.dashclient.modules;

import com.dashclient.DashClient;
import com.dashclient.Module;
import net.minecraft.client.Minecraft;

public final class ZoomModule extends Module {
    private static final int ZOOM_FOV = 30;
    private Integer originalFov;

    public ZoomModule() { super("Zoom"); }

    @Override public void onTick(Minecraft c) {
        // Only modify the view in-game. Z is a second binding for platforms
        // (including Android desktop modes) that intercept the original C key.
        boolean held = c.player != null && c.level != null && c.options != null
            && c.gui.screen() == null
            && (DashClient.ZOOM_KEY.isDown() || DashClient.ZOOM_ALTERNATE_KEY.isDown());

        if (!held) {
            restore(c);
            return;
        }

        if (originalFov == null)
            originalFov = c.options.fov().get();
        if (c.options.fov().get() != ZOOM_FOV)
            c.options.fov().set(ZOOM_FOV);
    }

    private void restore(Minecraft c) {
        if (originalFov == null) return;
        // Do not overwrite a player's manual FOV change made while zoomed.
        if (c.options != null && c.options.fov().get() == ZOOM_FOV)
            c.options.fov().set(originalFov);
        originalFov = null;
    }

    @Override public void onDisable() { restore(Minecraft.getInstance()); }
}
