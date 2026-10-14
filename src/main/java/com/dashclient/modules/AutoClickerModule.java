package com.dashclient.modules;

import com.dashclient.Module;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

/**
 * Native input auto-clicker: generates ordinary vanilla attack key presses and
 * keeps attack held for continuous block mining. No packet spoofing or
 * anti-cheat evasion. 10 attempts per second at a 20 TPS game tick rate.
 */
public final class AutoClickerModule extends Module {
    private static final int TICKS_PER_CLICK = 2; // 10 CPS at normal 20 TPS
    private int tickCount;
    private boolean pressed;

    public AutoClickerModule() { super("Auto Clicker"); }

    @Override public void onEnable() {
        tickCount = 0;
        pressed = false;
    }

    @Override public void onTick(Minecraft client) {
        if (client.player == null || !client.player.isAlive()
                || client.level == null || client.gameMode == null
                || client.options == null || client.gui.screen() != null) {
            release(client);
            tickCount = 0;
            return;
        }

        // Holding attack lets vanilla continue breaking blocks, while click
        // events are processed by its normal attack handler on subsequent ticks.
        client.options.keyAttack.setDown(true);
        pressed = true;

        if (++tickCount >= TICKS_PER_CLICK) {
            tickCount = 0;
            KeyMapping.click(InputConstants.getKey(client.options.keyAttack.saveString()));
        }
    }

    private void release(Minecraft client) {
        if (pressed && client.options != null)
            client.options.keyAttack.setDown(false);
        pressed = false;
    }

    @Override public void onDisable() {
        release(Minecraft.getInstance());
        tickCount = 0;
    }

    @Override public String hudText(Minecraft client) {
        return client.player == null ? null :
            "Auto Clicker: 10 CPS (toggle in Controls)";
    }
}
