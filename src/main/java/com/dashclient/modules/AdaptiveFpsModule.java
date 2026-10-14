package com.dashclient.modules;

import com.dashclient.DashClient;
import com.dashclient.Module;

/** Adaptive distance is usable even if no preset was previously enabled. */
public final class AdaptiveFpsModule extends Module {
    public AdaptiveFpsModule() { super("Adaptive FPS"); }
    @Override public void onEnable() {
        if (!DashClient.isEnabled("Performance Boost") && !DashClient.isEnabled("Extreme FPS")
                && !DashClient.isEnabled("Battery Saver")) {
            for (Module m : DashClient.MODULES) {
                if (m.name().equals("Performance Boost")) {
                    m.setEnabled(true);
                    break;
                }
            }
        }
    }
    @Override public void onDisable() { PerformanceModule.resetAdaptiveDistance(); }
}
