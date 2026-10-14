package com.dashclient;

import org.junit.jupiter.api.Test;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class PerformanceDefaultsTest {
    @Test void enablesOptimizationOnFreshInstall() {
        Properties p = new Properties();
        assertTrue(PerformanceDefaults.apply(p));
        assertEquals("true", p.getProperty("performance_boost"));
        assertEquals("true", p.getProperty("adaptive_fps"));
        assertEquals("true", p.getProperty("hud_eco_refresh"));
    }

    @Test void upgradesOldInstalledConfigExactlyOnce() {
        Properties p = new Properties();
        p.setProperty("performance_boost", "false");
        assertTrue(PerformanceDefaults.apply(p));
        assertEquals("true", p.getProperty("performance_boost"));
        p.setProperty("performance_boost", "false");
        assertFalse(PerformanceDefaults.apply(p));
        assertEquals("false", p.getProperty("performance_boost"));
    }

    @Test void preservesSelectedExtremeAndBatteryPresets() {
        Properties extreme = new Properties();
        extreme.setProperty("extreme_fps", "true");
        assertTrue(PerformanceDefaults.apply(extreme));
        assertEquals("false", extreme.getProperty("performance_boost"));

        Properties battery = new Properties();
        battery.setProperty("battery_saver", "true");
        assertTrue(PerformanceDefaults.apply(battery));
        assertEquals("false", battery.getProperty("performance_boost"));
    }

    @Test void doesNotFightUserExtendedView() {
        Properties p = new Properties();
        p.setProperty("extended_view", "true");
        PerformanceDefaults.apply(p);
        assertEquals("false", p.getProperty("performance_boost"));
        assertEquals("false", p.getProperty("adaptive_fps"));
    }
}
