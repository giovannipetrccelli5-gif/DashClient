package com.dashclient.modules;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ClickCadenceTest {
    @Test void schedulesFifteenClicksPerSecond() {
        ClickCadence cadence = new ClickCadence(15);
        int clicks = 0;
        for (int tick = 0; tick < 20; tick++)
            if (cadence.advance()) clicks++;
        assertEquals(15, clicks);
    }

    @Test void preservesRateAcrossMultipleSeconds() {
        ClickCadence cadence = new ClickCadence(15);
        int clicks = 0;
        for (int tick = 0; tick < 200; tick++)
            if (cadence.advance()) clicks++;
        assertEquals(150, clicks);
    }

    @Test void resetClearsPartialClickBudget() {
        ClickCadence cadence = new ClickCadence(15);
        assertFalse(cadence.advance());
        cadence.reset();
        assertFalse(cadence.advance());
        assertTrue(cadence.advance());
    }

    @Test void rejectsInvalidRates() {
        assertThrows(IllegalArgumentException.class, () -> new ClickCadence(0));
        assertThrows(IllegalArgumentException.class, () -> new ClickCadence(21));
    }
}
