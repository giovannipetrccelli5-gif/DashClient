package com.dashclient.modules;

import com.dashclient.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

/**
 * Maintains a totem in the offhand using one vanilla, server-synchronized
 * inventory SWAP click (button 40). Does not fabricate or duplicate items.
 */
public final class AutoTotemModule extends Module {
    private static final int ATTEMPT_INTERVAL_TICKS = 5;
    private int cooldown;

    public AutoTotemModule() {
        super("Auto Totem");
    }

    @Override
    public void onEnable() {
        cooldown = 0; // Refill at the next safe client tick.
    }

    @Override
    public void onDisable() {
        cooldown = 0;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.gameMode == null || client.level == null) {
            cooldown = 0;
            return;
        }

        // A different container (chest, trade, etc.) uses different slot IDs.
        // Never move items in an unrelated menu or while carrying an item
        // with the mouse; the inventory can be handled safely afterwards.
        if (client.player.containerMenu != client.player.inventoryMenu
                || !client.player.containerMenu.getCarried().isEmpty()) {
            return;
        }

        if (client.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            cooldown = 0;
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        // Prefer storage slots; preserve hotbar items whenever possible.
        int inventoryIndex = findTotem(client);
        if (inventoryIndex < 0) return;
        int menuSlot = inventoryMenuSlot(inventoryIndex);
        if (menuSlot < 0) return;

        // SWAP 40 is Minecraft's offhand swap: displaced offhand item
        // returns to the source inventory slot without ever being dropped.
        client.gameMode.handleInventoryMouseClick(
            client.player.inventoryMenu.containerId,
            menuSlot, 40, ClickType.SWAP, client.player);
        cooldown = ATTEMPT_INTERVAL_TICKS;
    }

    private static int findTotem(Minecraft client) {
        // Main inventory 9..35 before hotbar 0..8.
        for (int i = 9; i < 36; i++) {
            if (isTotem(client.player.getInventory().getItem(i))) return i;
        }
        for (int i = 0; i < 9; i++) {
            if (isTotem(client.player.getInventory().getItem(i))) return i;
        }
        return -1;
    }

    private static boolean isTotem(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.TOTEM_OF_UNDYING);
    }

    // Player inventory menu: main 9..35, hotbar 36..44; offhand swap button 40.
    public static int inventoryMenuSlot(int inventoryIndex) {
        if (inventoryIndex < 0 || inventoryIndex >= 36) return -1;
        return inventoryIndex < 9 ? inventoryIndex + 36 : inventoryIndex;
    }

    @Override
    public String hudText(Minecraft client) {
        if (client.player == null) return null;
        boolean equipped = client.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
        int available = client.player.getInventory().countItem(Items.TOTEM_OF_UNDYING);
        return "Auto Totem: " + (equipped ? "Equipped" :
            available > 0 ? "Equipping" : "No totem in inventory");
    }
}
