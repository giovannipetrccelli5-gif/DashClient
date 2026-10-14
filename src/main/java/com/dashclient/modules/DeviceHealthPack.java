package com.dashclient.modules;

import com.dashclient.Module;
import com.dashclient.DashClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Practical diagnostics and alerts using data the client already owns.
 * Every module is optional and is sampled through HudFrameCache.
 */
public final class DeviceHealthPack {
    private DeviceHealthPack() {}
    private static Module add(List<Module> list, String name, Function<Minecraft,String> value) {
        Module module = new Module(name) {
            @Override public String hudText(Minecraft c) { return value.apply(c); }
        };
        list.add(module);
        return module;
    }
    private static String ratio(double v) {
        return String.format(Locale.ROOT, "%.0f%%", v * 100);
    }
    private static int durability(ItemStack item) {
        if (item.isEmpty() || !item.isDamageableItem()) return 100;
        return (int) Math.round((item.getMaxDamage() - item.getDamageValue())
            * 100.0 / Math.max(1, item.getMaxDamage()));
    }
    private static long gcCount() {
        long count = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans())
            if (bean.getCollectionCount() >= 0) count += bean.getCollectionCount();
        return count;
    }
    private static long gcMillis() {
        long ms = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans())
            if (bean.getCollectionTime() >= 0) ms += bean.getCollectionTime();
        return ms;
    }

    public static void register(List<Module> modules) {
        add(modules, "JVM Heap Headroom", c -> {
            Runtime rt = Runtime.getRuntime();
            long used = rt.totalMemory() - rt.freeMemory();
            return "Available heap: " + Math.max(0,
                (rt.maxMemory() - used) / 1048576L) + " MiB";
        });
        add(modules, "JVM Heap Pressure", c -> {
            Runtime rt = Runtime.getRuntime();
            long used = rt.totalMemory() - rt.freeMemory();
            return "JVM heap used: " + ratio((double) used / Math.max(1,rt.maxMemory()));
        });
        add(modules, "JVM High Memory Alert", c -> {
            Runtime rt = Runtime.getRuntime();
            long used = rt.totalMemory() - rt.freeMemory();
            return used > rt.maxMemory() * 0.85
                ? "Memory warning: JVM heap over 85%" : null;
        });
        add(modules, "JVM Garbage Collections", c -> "GC collections: " + gcCount());
        add(modules, "JVM Garbage Pause Total", c -> "GC time (total): " + gcMillis() + "ms");
        add(modules, "JVM Live Threads", c -> {
            ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            return "JVM threads: " + bean.getThreadCount();
        });
        add(modules, "FPS Drop Warning 30", c -> c.player != null
            && c.getFps() > 0 && c.getFps() < 30 ? "FPS ALERT: under 30" : null);
        add(modules, "FPS Drop Warning 45", c -> c.player != null
            && c.getFps() > 0 && c.getFps() < 45 ? "FPS ALERT: under 45" : null);
        add(modules, "Active Optimization Profile", c ->
            "Profile: " + (DashClient.isEnabled("Battery Saver") ? "Battery Saver"
                : DashClient.isEnabled("Extreme FPS") ? "Extreme FPS"
                : DashClient.isEnabled("Performance Boost") ? "Performance Boost"
                : "Vanilla/custom"));
        add(modules, "HUD Update Period", c ->
            "HUD refresh: " + (DashClient.isEnabled("HUD Eco Refresh") ? "1.0s" :
                DashClient.isEnabled("HUD Fast Refresh") ? "0.05s" : "0.25s"));
        add(modules, "Network Latency Warning", c -> {
            if (c.player == null || c.getConnection() == null) return null;
            PlayerInfo info = c.getConnection().getPlayerInfo(c.player.getUUID());
            return info != null && info.getLatency() >= 200
                ? "High ping: " + info.getLatency() + "ms" : null;
        });
        add(modules, "Totem Backup Warning", c -> c.player == null ? null :
            c.player.getInventory().countItem(Items.TOTEM_OF_UNDYING) <= 1
                ? "Totem warning: low supply" : null);
        add(modules, "Elytra Durability Warning", c -> {
            if (c.player == null) return null;
            ItemStack armor = c.player.getItemBySlot(EquipmentSlot.CHEST);
            if (!armor.is(Items.ELYTRA)) return null;
            return durability(armor) <= 15 ? "Elytra durability: " +
                durability(armor) + "%" : null;
        });
        add(modules, "Armor Durability Warning 15", c -> {
            if (c.player == null) return null;
            int min = 100;
            for (EquipmentSlot slot : new EquipmentSlot[]{
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack item = c.player.getItemBySlot(slot);
                if (!item.isEmpty() && item.isDamageableItem())
                    min = Math.min(min, durability(item));
            }
            return min <= 15 ? "Armor durability warning: " + min + "%" : null;
        });
        add(modules, "Low Air Warning", c -> c.player != null
            && c.player.isInWater() && c.player.getAirSupply() < 90
                ? "AIR WARNING: " + c.player.getAirSupply() : null);
        add(modules, "Tool Durability Warning 10", c -> {
            if (c.player == null) return null;
            var tool = c.player.getMainHandItem();
            return tool.isDamageableItem() && durability(tool) <= 10
                ? "Tool durability warning: " + durability(tool) + "%" : null;
        });
    }
}
