package com.dashclient.modules;

import com.dashclient.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** Lightweight, independent optional HUD modules, without rendering mixins. */
public final class AdvancedHudPack {
    private AdvancedHudPack() {}
    private static final int[] FPS_HISTORY = new int[10];
    private static int count, cursor, sampleTicks;
    private static int lastFps;

    public static void sample(Minecraft c) {
        lastFps = Math.max(0, c.getFps());
        if (++sampleTicks < 20) return;
        sampleTicks = 0;
        FPS_HISTORY[cursor] = lastFps;
        cursor = (cursor + 1) % FPS_HISTORY.length;
        if (count < FPS_HISTORY.length) count++;
    }

    private static int average() {
        if (count == 0) return lastFps;
        int sum = 0;
        for (int i = 0; i < count; i++) sum += FPS_HISTORY[i];
        return sum / count;
    }
    private static int minimum() {
        if (count == 0) return lastFps;
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < count; i++) min = Math.min(min, FPS_HISTORY[i]);
        return min;
    }
    private static int maximum() {
        if (count == 0) return lastFps;
        int max = 0;
        for (int i = 0; i < count; i++) max = Math.max(max, FPS_HISTORY[i]);
        return max;
    }
    private static void add(List<Module> list, String name, Function<Minecraft, String> text) {
        list.add(new Module(name) {
            @Override public String hudText(Minecraft c) { return text.apply(c); }
        });
    }
    private static void item(List<Module> list, String name, Item item) {
        add(list, name, c -> c.player == null ? null :
            name + ": " + c.player.getInventory().countItem(item));
    }
    private static String durability(ItemStack item) {
        if (item.isEmpty()) return "Empty";
        if (!item.isDamageableItem()) return "N/A";
        return (item.getMaxDamage() - item.getDamageValue()) + "/" + item.getMaxDamage();
    }
    private static String armor(Minecraft c, String name, EquipmentSlot slot) {
        return c.player == null ? null : name + ": " + durability(c.player.getItemBySlot(slot));
    }

    public static void register(List<Module> list) {
        // PvP, mining, survival and building item counters (18).
        item(list, "Netherite Ingots", Items.NETHERITE_INGOT);
        item(list, "Ancient Debris", Items.ANCIENT_DEBRIS);
        item(list, "Netherite Scrap", Items.NETHERITE_SCRAP);
        item(list, "Iron Ingots", Items.IRON_INGOT);
        item(list, "Gold Ingots", Items.GOLD_INGOT);
        item(list, "Coal", Items.COAL);
        item(list, "Redstone Dust", Items.REDSTONE);
        item(list, "Lapis Lazuli", Items.LAPIS_LAZULI);
        item(list, "Obsidian", Items.OBSIDIAN);
        item(list, "Cobwebs", Items.COBWEB);
        item(list, "XP Bottles", Items.EXPERIENCE_BOTTLE);
        item(list, "End Crystals", Items.END_CRYSTAL);
        item(list, "TNT Count", Items.TNT);
        item(list, "String Count", Items.STRING);
        item(list, "Cobblestone", Items.COBBLESTONE);
        item(list, "Water Buckets", Items.WATER_BUCKET);
        item(list, "Lava Buckets", Items.LAVA_BUCKET);
        item(list, "Nether Quartz", Items.QUARTZ);

        // FPS monitoring, native graphics-option visibility and troubleshooting.
        add(list, "FPS Average", c -> "FPS 10s avg: " + average());
        add(list, "FPS Minimum", c -> "FPS 10s min: " + minimum());
        add(list, "FPS Maximum", c -> "FPS 10s max: " + maximum());
        add(list, "Frame Interval", c -> lastFps == 0 ? "Frame interval: --" :
            String.format(Locale.ROOT, "Approx frame: %.1fms", 1000.0 / lastFps));
        add(list, "Low FPS Alert", c -> c.player != null && lastFps > 0
            && lastFps < 40 ? "LOW FPS: " + lastFps : null);
        add(list, "Render Distance", c -> "Render: " + c.options.renderDistance().get() + " chunks");
        add(list, "Simulation Distance", c -> "Simulation: " +
            c.options.simulationDistance().get() + " chunks");
        add(list, "Particle Setting", c -> "Particles: " + c.options.particles().get());
        add(list, "Cloud Setting", c -> "Clouds: " + c.options.cloudStatus().get());
        add(list, "Entity Shadows", c -> "Entity shadows: " +
            (c.options.entityShadows().get() ? "On" : "Off"));
        add(list, "Biome Blend", c -> "Biome blend: " + c.options.biomeBlendRadius().get());
        add(list, "Field of View", c -> "FOV: " + c.options.fov().get());
        add(list, "GUI Scale", c -> "GUI scale: " + c.options.guiScale().get());
        add(list, "HUD Resolution", c -> "HUD: " + c.getWindow().getGuiScaledWidth()
            + "x" + c.getWindow().getGuiScaledHeight());

        // Player, item and input readouts.
        add(list, "Player Username", c -> c.player == null ? null :
            "IGN: " + c.player.getName().getString());
        add(list, "Held Item Type", c -> c.player == null ? null :
            "Holding: " + c.player.getMainHandItem().getItem().getName().getString());
        add(list, "Offhand Durability", c -> c.player == null ? null :
            "Offhand durability: " + durability(c.player.getOffhandItem()));
        add(list, "Helmet Durability", c -> armor(c, "Helmet durability", EquipmentSlot.HEAD));
        add(list, "Chestplate Durability", c -> armor(c, "Chest durability", EquipmentSlot.CHEST));
        add(list, "Leggings Durability", c -> armor(c, "Leggings durability", EquipmentSlot.LEGS));
        add(list, "Boots Durability", c -> armor(c, "Boots durability", EquipmentSlot.FEET));
        add(list, "Food Saturation", c -> c.player == null ? null :
            String.format(Locale.ROOT, "Saturation: %.1f", c.player.getFoodData().getSaturationLevel()));
        add(list, "Jump Key State", c -> "Jump: " + (c.options.keyJump.isDown() ? "Held" : "Released"));
        add(list, "Sneak Key State", c -> "Sneak key: " + (c.options.keyShift.isDown() ? "Held" : "Released"));
        add(list, "Attack Key State", c -> "Attack: " + (c.options.keyAttack.isDown() ? "Held" : "Released"));
        add(list, "Use Key State", c -> "Use: " + (c.options.keyUse.isDown() ? "Held" : "Released"));
    }
}
