package com.dashclient.modules;

import com.dashclient.Module;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

/**
 * Native input auto-clicker: generates ordinary vanilla attack key presses and
 * keeps attack held for continuous block mining. No packet spoofing or
 * anti-cheat evasion. Schedules 15 inputs per 20 game ticks (15 CPS at 20 TPS).
 * Actual server-side hit rate still depends on vanilla and server cooldowns.
 */
public final class AutoClickerModule extends Module {
    private static final int TARGET_CPS = 15;
    private final ClickCadence cadence = new ClickCadence(TARGET_CPS);
    private boolean pressed;

    public AutoClickerModule() { super("Auto Clicker"); }

    @Override public void onEnable() {
        cadence.reset();
        pressed = false;
    }

    @Override public void onTick(Minecraft client) {
        if (client.player == null || !client.player.isAlive()
                || client.level == null || client.gameMode == null
                || client.options == null || client.gui.screen() != null) {
            release(client);
            cadence.reset();
            return;
        }

        // Holding attack preserves vanilla block mining; paced click events
        // are consumed by the regular attack handler in subsequent ticks.
        client.options.keyAttack.setDown(true);
        pressed = true;

        if (cadence.advance()) {
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
        cadence.reset();
    }

    @Override public String hudText(Minecraft client) {
        return client.player == null ? null :
            "Auto Clicker: 15 CPS (toggle in Controls)";
    }
}
