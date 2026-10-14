package com.dashclient;

import com.dashclient.modules.AutoTotemModule;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoTotemSlotsTest {
    @Test void mapsInventorySlotsToPlayerMenu() {
        assertEquals(36, AutoTotemModule.inventoryMenuSlot(0));
        assertEquals(44, AutoTotemModule.inventoryMenuSlot(8));
        assertEquals(9, AutoTotemModule.inventoryMenuSlot(9));
        assertEquals(35, AutoTotemModule.inventoryMenuSlot(35));
        assertEquals(-1, AutoTotemModule.inventoryMenuSlot(-1));
        assertEquals(-1, AutoTotemModule.inventoryMenuSlot(36));
        assertEquals(-1, AutoTotemModule.inventoryMenuSlot(40));
    }
}
