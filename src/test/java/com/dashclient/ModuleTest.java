package com.dashclient;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ModuleTest {
    private static class CountingModule extends Module {
        int enabledCalls;
        int disabledCalls;
        CountingModule() { super("Test Module"); }
        @Override public void onEnable() { enabledCalls++; }
        @Override public void onDisable() { disabledCalls++; }
    }

    @Test void togglesAreIdempotent() {
        CountingModule m = new CountingModule();
        assertEquals("Test Module", m.name());
        assertFalse(m.enabled());
        m.setEnabled(true);
        m.setEnabled(true);
        assertTrue(m.enabled());
        assertEquals(1, m.enabledCalls);
        m.setEnabled(false);
        m.setEnabled(false);
        assertFalse(m.enabled());
        assertEquals(1, m.disabledCalls);
    }
}
