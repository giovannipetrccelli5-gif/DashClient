package com.dashclient.modules;

import com.dashclient.Module;
import net.minecraft.client.Minecraft;

public final class SprintModule extends Module {
    public SprintModule() { super("Toggle Sprint"); }
    @Override public void onTick(Minecraft c) {
        if (c.player != null && c.gui.screen() == null &&
            c.options.keyUp.isDown() && !c.player.isShiftKeyDown()) {
            c.player.setSprinting(true);
        }
    }
}
