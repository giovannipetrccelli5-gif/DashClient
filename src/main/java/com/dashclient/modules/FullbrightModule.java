package com.dashclient.modules;

import com.dashclient.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class FullbrightModule extends Module {
    private LocalPlayer modified;

    public FullbrightModule() { super("Fullbright"); }

    @Override public void onTick(Minecraft c) {
        if (modified != null && modified != c.player) modified = null;
        if (c.player == null) return;
        MobEffectInstance existing = c.player.getEffect(MobEffects.NIGHT_VISION);
        if (modified == c.player && existing != null && existing.getDuration() > 60) return;
        if (modified == null && existing != null) return; // Preserve ordinary night vision.
        c.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, true, false));
        modified = c.player;
    }

    @Override public void onDisable() {
        if (modified != null) modified.removeEffect(MobEffects.NIGHT_VISION);
        modified = null;
    }
}
