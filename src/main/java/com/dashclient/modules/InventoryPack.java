package com.dashclient.modules;

import com.dashclient.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** Inventory and survival readouts. Nothing is changed server-side. */
public final class InventoryPack {
    private InventoryPack() {}
    private static void add(List<Module> list, String name, Function<Minecraft,String> display) {
        list.add(new Module(name) {
            @Override public String hudText(Minecraft c) { return display.apply(c); }
        });
    }
    private static int count(Minecraft c, Item item) {
        return c.player == null ? 0 : c.player.getInventory().countItem(item);
    }
    private static String countText(Minecraft c, String label, Item item) {
        return c.player == null ? null : label + ": " + count(c,item);
    }
    private static int used(Minecraft c) {
        if (c.player == null) return 0;
        int n = 0;
        var inv = c.player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            if (!inv.getItem(slot).isEmpty()) n++;
        }
        return n;
    }
    public static void register(List<Module> list) {
        add(list, "Arrows", c -> countText(c, "Arrows", Items.ARROW));
        add(list, "Spectral Arrows", c -> countText(c, "Spectral", Items.SPECTRAL_ARROW));
        add(list, "Ender Pearls", c -> countText(c, "Pearls", Items.ENDER_PEARL));
        add(list, "Firework Rockets", c -> countText(c, "Rockets", Items.FIREWORK_ROCKET));
        add(list, "Golden Apples", c -> countText(c, "Golden apples", Items.GOLDEN_APPLE));
        add(list, "Enchanted Apples", c -> countText(c, "Enchanted apples", Items.ENCHANTED_GOLDEN_APPLE));
        add(list, "Diamonds", c -> countText(c, "Diamonds", Items.DIAMOND));
        add(list, "Emeralds", c -> countText(c, "Emeralds", Items.EMERALD));
        add(list, "Experience Level", c -> c.player == null ? null :
            "XP level: " + c.player.experienceLevel);
        add(list, "Experience Progress", c -> c.player == null ? null :
            String.format(Locale.ROOT, "XP bar: %.0f%%", c.player.experienceProgress * 100));
        add(list, "Armor Points", c -> c.player == null ? null :
            "Armor points: " + c.player.getArmorValue());
        add(list, "Absorption Hearts", c -> c.player == null ? null :
            String.format(Locale.ROOT, "Absorption: %.1f hearts", c.player.getAbsorptionAmount() / 2));
        add(list, "Air Supply", c -> c.player == null ? null :
            "Air: " + c.player.getAirSupply() + "/" + c.player.getMaxAirSupply());
        add(list, "Offhand Item", c -> {
            if (c.player == null) return null;
            ItemStack s = c.player.getOffhandItem();
            return "Offhand: " + (s.isEmpty() ? "Empty" : s.getHoverName().getString());
        });
        add(list, "Offhand Count", c -> c.player == null ? null :
            "Offhand stack: " + c.player.getOffhandItem().getCount());
        add(list, "Inventory Free Slots", c -> c.player == null ? null :
            "Free inventory slots: " + (c.player.getInventory().getContainerSize() - used(c)));
        add(list, "Inventory Fill", c -> c.player == null ? null :
            "Inventory filled: " + used(c) + "/" + c.player.getInventory().getContainerSize());
    }
}
