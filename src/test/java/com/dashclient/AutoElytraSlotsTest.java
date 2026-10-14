package com.dashclient;

import com.dashclient.modules.AutoElytraModule;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoElytraSlotsTest {
    @Test void mapsStorageAndHotbarWithoutTouchingArmor() {
        assertEquals(36, AutoElytraModule.inventoryMenuSlot(0));
        assertEquals(44, AutoElytraModule.inventoryMenuSlot(8));
        assertEquals(9, AutoElytraModule.inventoryMenuSlot(9));
        assertEquals(35, AutoElytraModule.inventoryMenuSlot(35));
        assertEquals(-1, AutoElytraModule.inventoryMenuSlot(-1));
        assertEquals(-1, AutoElytraModule.inventoryMenuSlot(36));
    }
}
