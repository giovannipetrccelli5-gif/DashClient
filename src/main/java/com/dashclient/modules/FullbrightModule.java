package com.dashclient.modules;

import com.dashclient.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class FullbrightModule extends Module {
    private LocalPlayer modified;
    private MobEffectInstance injected;

    public FullbrightModule() { super("Fullbright"); }

    @Override public void onTick(Minecraft c) {
        if (modified != c.player) {
            // Do not change a previous player's effects after switching worlds.
            modified = null;
            injected = null;
        }
        if (c.player == null) return;

        MobEffectInstance existing = c.player.getEffect(MobEffects.NIGHT_VISION);

        // The server or a potion replaced our effect. Never override or remove it.
        if (existing != null && existing != injected) {
            modified = null;
            injected = null;
            return;
        }
        if (existing == injected && injected != null && existing.getDuration() > 60) {
            return;
        }
        // Only create or refresh the client-side instance that we own.
        MobEffectInstance effect = new MobEffectInstance(
            MobEffects.NIGHT_VISION, 240, 0, true, false);
        c.player.addEffect(effect);
        modified = c.player;
        injected = c.player.getEffect(MobEffects.NIGHT_VISION);
    }

    @Override public void onDisable() {
        if (modified != null && injected != null
                && modified.getEffect(MobEffects.NIGHT_VISION) == injected) {
            modified.removeEffect(MobEffects.NIGHT_VISION);
        }
        modified = null;
        injected = null;
    }
}
