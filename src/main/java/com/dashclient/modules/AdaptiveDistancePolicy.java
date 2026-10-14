package com.dashclient.modules;

/** Stateless hysteresis policy; avoids FPS-triggered render-distance thrashing. */
public final class AdaptiveDistancePolicy {
    private AdaptiveDistancePolicy() {}
    public static int next(int current, int min, int max, int lowSamples, int highSamples) {
        if (min > max) throw new IllegalArgumentException("Invalid render distance limits");
        if (lowSamples >= 2) return Math.max(min, current - 2);
        if (highSamples >= 3) return Math.min(max, current + 1);
        return current;
    }
}
