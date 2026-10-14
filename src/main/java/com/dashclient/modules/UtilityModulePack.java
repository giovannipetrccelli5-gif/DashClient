package com.dashclient.modules;

import com.dashclient.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Optional equipment, combat and performance indicators.
 * Uses local player data only; no renderer mixins or scanning world chunks.
 */
public final class UtilityModulePack {
    private UtilityModulePack() {}
    private static void hud(List<Module> modules, String name,
                            Function<Minecraft, String> value) {
        modules.add(new Module(name) {
            @Override public String hudText(Minecraft c) { return value.apply(c); }
        });
    }
    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
    private static String name(ItemStack stack) {
        return stack.isEmpty() ? "Empty" : stack.getHoverName().getString();
    }
    private static int percent(ItemStack stack) {
        if (!stack.isDamageableItem()) return stack.isEmpty() ? 0 : 100;
        return Math.max(0, Math.min(100, (int)Math.round(
            100.0 * (stack.getMaxDamage() - stack.getDamageValue())
                / Math.max(1,stack.getMaxDamage()))));
    }
    private static String gear(Minecraft c, EquipmentSlot slot, String label) {
        if (c.player == null) return null;
        ItemStack s = c.player.getItemBySlot(slot);
        return label + ": " + (s.isEmpty() ? "Empty" :
            percent(s) + "% (" + name(s) + ")");
    }

    public static void register(List<Module> modules) {
        // Key-hold assistance releases keys while a GUI is open or disabled.
        modules.add(new AutoSneak());
        modules.add(new AutoUse());

        hud(modules, "Gear: Helmet Condition", c -> gear(c, EquipmentSlot.HEAD, "Helmet"));
        hud(modules, "Gear: Chest Condition", c -> gear(c, EquipmentSlot.CHEST, "Chest"));
        hud(modules, "Gear: Leggings Condition", c -> gear(c, EquipmentSlot.LEGS, "Legs"));
        hud(modules, "Gear: Boots Condition", c -> gear(c, EquipmentSlot.FEET, "Boots"));
        hud(modules, "Gear: Equipped Pieces", c -> {
            if (c.player == null) return null;
            int count=0;
            for (EquipmentSlot slot : new EquipmentSlot[]{
                    EquipmentSlot.HEAD,EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS,EquipmentSlot.FEET}) {
                if (!c.player.getItemBySlot(slot).isEmpty()) count++;
            }
            return "Equipped pieces: " + count + "/4";
        });
        hud(modules, "Gear: Lowest Armor Condition", c -> {
            if (c.player == null) return null;
            int lowest = 100;
            int equipped = 0;
            for (EquipmentSlot slot : new EquipmentSlot[]{
                    EquipmentSlot.HEAD,EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS,EquipmentSlot.FEET}) {
                ItemStack item = c.player.getItemBySlot(slot);
                if (!item.isEmpty()) {
                    lowest = Math.min(lowest, percent(item));
                    equipped++;
                }
            }
            return equipped == 0 ? "Armor: none equipped" : "Armor minimum: " + lowest + "%";
        });
        hud(modules, "Gear: Held Item Name", c -> c.player == null ? null :
            "Main hand: " + name(c.player.getMainHandItem()));
        hud(modules, "Gear: Offhand Name", c -> c.player == null ? null :
            "Offhand: " + name(c.player.getOffhandItem()));
        hud(modules, "Gear: Mainhand Count", c -> c.player == null ? null :
            "Mainhand stack: " + c.player.getMainHandItem().getCount());
        hud(modules, "Gear: Offhand Count", c -> c.player == null ? null :
            "Offhand stack: " + c.player.getOffhandItem().getCount());
        hud(modules, "Gear: Mainhand Condition", c -> c.player == null ? null :
            "Held condition: " + percent(c.player.getMainHandItem()) + "%");
        hud(modules, "Gear: Mainhand Damage", c -> c.player == null ? null :
            "Held damage: " + c.player.getMainHandItem().getDamageValue());
        hud(modules, "Gear: Mainhand Remaining", c -> {
            if (c.player == null) return null;
            ItemStack item = c.player.getMainHandItem();
            return item.isDamageableItem() ? "Held uses remaining: " +
                Math.max(0, item.getMaxDamage() - item.getDamageValue()) : null;
        });
        hud(modules, "Gear: Offhand Condition", c -> c.player == null ? null :
            "Offhand condition: " + percent(c.player.getOffhandItem()) + "%");
        hud(modules, "Gear: Inventory Filled", c -> {
            if (c.player == null) return null;
            int filled=0;
            for (int i=0;i<36;i++)
                if (!c.player.getInventory().getItem(i).isEmpty()) filled++;
            return "Inventory occupied: " + filled + "/36";
        });
        hud(modules, "Gear: Free Slots", c -> {
            if (c.player == null) return null;
            int empty=0;
            for (int i=0;i<36;i++)
                if (c.player.getInventory().getItem(i).isEmpty()) empty++;
            return "Empty inventory slots: " + empty;
        });
        hud(modules, "Gear: Totem Offhand", c -> c.player == null ? null :
            "Totem in offhand: " + (c.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)
                ? "Yes" : "No"));
        hud(modules, "Gear: Elytra Equipped", c -> c.player == null ? null :
            "Elytra equipped: " + (c.player.getItemBySlot(EquipmentSlot.CHEST)
                .is(Items.ELYTRA) ? "Yes" : "No"));
        hud(modules, "PvP: Absorption Hearts", c -> c.player == null ? null :
            "Absorption hearts: " + fmt(c.player.getAbsorptionAmount() / 2.0));
        hud(modules, "PvP: Health Hearts", c -> c.player == null ? null :
            "Health hearts: " + fmt(c.player.getHealth()/2.0));
        hud(modules, "PvP: Attack Recharge", c -> c.player == null ? null :
            "Attack strength: " + Math.round(c.player.getAttackStrengthScale(0f)*100) + "%");
        hud(modules, "PvP: Damageable Offhand", c -> c.player == null ? null :
            "Offhand has durability: " + (c.player.getOffhandItem().isDamageableItem()
                ? "Yes" : "No"));
        hud(modules, "Flight: Glide Speed", c -> c.player == null ||
            !c.player.isFallFlying() ? null :
            "Glide horizontal: " + fmt(c.player.getDeltaMovement().horizontalDistance()*20) +
                " blocks/s");
        hud(modules, "Flight: Glide Altitude", c -> c.player == null ||
            !c.player.isFallFlying() ? null :
            "Glide altitude: Y " + fmt(c.player.getY()));
        hud(modules, "Flight: Falling Speed", c -> c.player == null ? null :
            "Vertical speed: " + fmt(c.player.getDeltaMovement().y*20) + " blocks/s");
        hud(modules, "Session: Experience Level", c -> c.player == null ? null :
            "XP level: " + c.player.experienceLevel);
        hud(modules, "Session: Experience Progress", c -> c.player == null ? null :
            "XP level progress: " + Math.round(c.player.experienceProgress*100) + "%");
        hud(modules, "Session: Food Saturation", c -> c.player == null ? null :
            "Saturation: " + fmt(c.player.getFoodData().getSaturationLevel()));
        hud(modules, "Session: Air Percent", c -> c.player == null ? null :
            "Breath: " + Math.max(0,Math.round(c.player.getAirSupply()*100.0/300.0)) + "%");
        hud(modules, "Perf: Visible Chunk Area", c -> c.options == null ? null :
            "Approx view-area chunks: " +
                ((2*c.options.renderDistance().get()+1)*(2*c.options.renderDistance().get()+1)));
        hud(modules, "Perf: Render Distance Limit", c -> c.options == null ? null :
            "Local view radius: " + c.options.renderDistance().get() + " chunks");
    }

    private static final class AutoSneak extends Module {
        private boolean pressed;
        AutoSneak() { super("Auto Sneak"); }
        @Override public void onTick(Minecraft c) {
            if (c.options == null) return;
            boolean shouldPress = c.player != null && c.player.isAlive()
                && c.gui.screen() == null;
            if (shouldPress) {
                c.options.keyShift.setDown(true);
                pressed = true;
            } else if (pressed) release(c);
        }
        private void release(Minecraft c) {
            if (pressed && c.options != null) c.options.keyShift.setDown(false);
            pressed = false;
        }
        @Override public void onDisable() { release(Minecraft.getInstance()); }
        @Override public String hudText(Minecraft c) {
            return c.player == null ? null : "Auto Sneak: holding crouch";
        }
    }

    private static final class AutoUse extends Module {
        private boolean pressed;
        AutoUse() { super("Auto Use"); }
        @Override public void onTick(Minecraft c) {
            if (c.options == null) return;
            boolean shouldPress = c.player != null && c.player.isAlive()
                && c.gui.screen() == null;
            if (shouldPress) {
                c.options.keyUse.setDown(true);
                pressed = true;
            } else if (pressed) release(c);
        }
        private void release(Minecraft c) {
            if (pressed && c.options != null) c.options.keyUse.setDown(false);
            pressed = false;
        }
        @Override public void onDisable() { release(Minecraft.getInstance()); }
        @Override public String hudText(Minecraft c) {
            return c.player == null ? null : "Auto Use: holding use";
        }
    }
}
