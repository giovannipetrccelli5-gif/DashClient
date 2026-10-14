package com.dashclient.mixin;

import com.dashclient.DashClient;
import com.dashclient.modules.FreecamModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Route mouse/controller look to the detached camera instead of the player. */
@Mixin(Entity.class)
public abstract class FreecamLookMixin {
    @Inject(method = "turn(DD)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void dashclient$look(double yaw, double pitch, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if ((Object) this != client.player || !DashClient.isEnabled("Freecam")) return;
        if (!(DashClient.module("Freecam") instanceof FreecamModule freecam)
                || freecam.cameraEntity() == null) return;
        freecam.cameraEntity().turn(yaw, pitch);
        ci.cancel();
    }
}
