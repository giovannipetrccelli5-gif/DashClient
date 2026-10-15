package com.dashclient.modules;

/**
 * Fractional click scheduler for Minecraft's normal 20 client ticks/second.
 * Avoids rounding 15 CPS to a 1-tick (20 CPS) or 2-tick (10 CPS) interval.
 */
public final class ClickCadence {
    private static final int TICKS_PER_SECOND = 20;
    private final int clicksPerSecond;
    private int budget;

    public ClickCadence(int clicksPerSecond) {
        if (clicksPerSecond < 1 || clicksPerSecond > TICKS_PER_SECOND)
            throw new IllegalArgumentException("CPS must be between 1 and 20");
        this.clicksPerSecond = clicksPerSecond;
    }

    public boolean advance() {
        budget += clicksPerSecond;
        if (budget < TICKS_PER_SECOND) return false;
        budget -= TICKS_PER_SECOND;
        return true;
    }

    public void reset() {
        budget = 0;
    }
}
