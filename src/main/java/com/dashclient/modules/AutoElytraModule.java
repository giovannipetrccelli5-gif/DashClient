package com.dashclient.modules;

import com.dashclient.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Searches the real player inventory for an Elytra, equips it without
 * dropping the replaced chestplate, and starts ordinary vanilla gliding.
 * The server must permit the flight; this does not bypass server checks.
 */
public final class AutoElytraModule extends Module {
    private static final int CHEST_SLOT = 6;
    private int cooldown;

    public AutoElytraModule() { super("Auto Elytra"); }

    @Override public void onEnable() { cooldown = 0; }
    @Override public void onDisable() { cooldown = 0; }

    @Override public void onTick(Minecraft c) {
        if (c.player == null || c.gameMode == null || c.level == null || !c.player.isAlive()) {
            cooldown = 0;
            return;
        }
        if (cooldown > 0) cooldown--;
        ItemStack chest = c.player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.is(Items.ELYTRA)) {
            if (cooldown > 0 || !safeToEquip(c)) return;
            int sourceIndex = findElytra(c);
            if (sourceIndex < 0) return;

            int sourceSlot = inventoryMenuSlot(sourceIndex);
            if (sourceSlot < 0) return;
            // Standard PICKUP sequence swaps Elytra and chestplate and returns
            // the displaced chestplate to the original Elytra slot.
            click(c, sourceSlot);
            click(c, CHEST_SLOT);
            if (!c.player.inventoryMenu.getCarried().isEmpty())
                click(c, sourceSlot);
            cooldown = 12;
            return;
        }

        if (cooldown > 0 || c.gui.screen() != null
                || c.player.onGround() || c.player.isFallFlying()
                || c.player.isPassenger() || c.player.isInWater()
                || c.player.fallDistance <= 0.5F) return;

        if (c.player.tryToStartFallFlying()) {
            c.player.connection.send(new ServerboundPlayerCommandPacket(
                c.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            cooldown = 12;
        }
    }

    private static boolean safeToEquip(Minecraft c) {
        return c.gui.screen() == null
            && c.player.containerMenu == c.player.inventoryMenu
            && c.player.inventoryMenu.getCarried().isEmpty();
    }

    private static void click(Minecraft c, int slot) {
        c.gameMode.handleContainerInput(
            c.player.inventoryMenu.containerId, slot, 0, ContainerInput.PICKUP, c.player);
    }

    private static int findElytra(Minecraft c) {
        // Storage first, then hotbar. Do not inspect private chest contents.
        for (int i = 9; i < 36; i++)
            if (c.player.getInventory().getItem(i).is(Items.ELYTRA)) return i;
        for (int i = 0; i < 9; i++)
            if (c.player.getInventory().getItem(i).is(Items.ELYTRA)) return i;
        return -1;
    }

    public static int inventoryMenuSlot(int index) {
        if (index < 0 || index >= 36) return -1;
        return index < 9 ? 36 + index : index;
    }

    @Override public String hudText(Minecraft c) {
        if (c.player == null) return null;
        if (c.player.isFallFlying()) return "Auto Elytra: Gliding";
        if (c.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA))
            return "Auto Elytra: Equipped";
        return c.player.getInventory().countItem(Items.ELYTRA) > 0
            ? "Auto Elytra: Equipping" : "Auto Elytra: No Elytra in inventory";
    }
}
