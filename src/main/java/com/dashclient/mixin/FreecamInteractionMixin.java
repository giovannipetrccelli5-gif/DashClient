package com.dashclient.mixin;

import com.dashclient.DashClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevent attacking, mining, picking and using items from the detached camera. */
@Mixin(Minecraft.class)
public abstract class FreecamInteractionMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true, require = 0)
    private void dashclient$attack(CallbackInfoReturnable<Boolean> cir) {
        if (DashClient.isEnabled("Freecam")) cir.setReturnValue(false);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true, require = 0)
    private void dashclient$mine(CallbackInfo ci) {
        if (DashClient.isEnabled("Freecam")) ci.cancel();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void dashclient$use(CallbackInfo ci) {
        if (DashClient.isEnabled("Freecam")) ci.cancel();
    }

    @Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true, require = 0)
    private void dashclient$pick(CallbackInfo ci) {
        if (DashClient.isEnabled("Freecam")) ci.cancel();
    }
}
