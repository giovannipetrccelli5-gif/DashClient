package com.dashclient;

import java.util.Properties;

/**
 * Applies fast, reversible graphics defaults once per installation.
 *
 * The new defaults also apply to users upgrading from an older DashClient,
 * but any subsequent manual toggle is persisted and respected.
 */
public final class PerformanceDefaults {
    public static final String MARKER = "automatic_performance_v1";
    private PerformanceDefaults() {}

    public static boolean apply(Properties settings) {
        if ("1".equals(settings.getProperty(MARKER))) return false;

        boolean extreme = Boolean.parseBoolean(settings.getProperty("extreme_fps"));
        boolean battery = Boolean.parseBoolean(settings.getProperty("battery_saver"));
        boolean extendedView = Boolean.parseBoolean(settings.getProperty("extended_view"));

        // Do not override an existing, deliberately selected graphics preset.
        // An active Extended View setting likewise takes precedence.
        if (!extreme && !battery && !extendedView)
            settings.setProperty("performance_boost", "true");
        if (!extendedView)
            settings.setProperty("adaptive_fps", "true");

        // Only HUD refresh is throttled; this does not throttle game ticks.
        settings.setProperty("hud_eco_refresh", "true");

        settings.setProperty(MARKER, "1");
        return true;
    }
}
