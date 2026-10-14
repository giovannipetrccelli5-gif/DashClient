package com.dashclient;

import com.dashclient.modules.AdaptiveDistancePolicy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class AdaptiveDistancePolicyTest {
    @Test void lowFpsRequiresTwoSamples() {
        assertEquals(8, AdaptiveDistancePolicy.next(8, 4, 12, 1, 0));
        assertEquals(6, AdaptiveDistancePolicy.next(8, 4, 12, 2, 0));
    }
    @Test void highFpsRequiresThreeSamples() {
        assertEquals(8, AdaptiveDistancePolicy.next(8, 4, 12, 0, 2));
        assertEquals(9, AdaptiveDistancePolicy.next(8, 4, 12, 0, 3));
    }
    @Test void respectsBounds() {
        assertEquals(4, AdaptiveDistancePolicy.next(4, 4, 8, 2, 0));
        assertEquals(8, AdaptiveDistancePolicy.next(8, 4, 8, 0, 3));
        assertThrows(IllegalArgumentException.class,
            () -> AdaptiveDistancePolicy.next(5, 7, 4, 0, 0));
    }
}
