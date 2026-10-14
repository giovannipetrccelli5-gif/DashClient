package com.dashclient.modules;

import com.dashclient.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import java.util.Locale;

/**
 * Locates dropped Elytras and Elytras in item frames in already loaded chunks.
 * Cannot infer undiscovered End Cities, unloaded chunks or unopened chest loot.
 */
public final class ElytraFinderModule extends Module {
    private static final double RADIUS = 64.0;

    public ElytraFinderModule() { super("Elytra Finder"); }

    @Override public String hudText(Minecraft c) {
        if (c.player == null || c.level == null) return null;
        if (c.player.getOffhandItem().is(Items.ELYTRA)
                || c.player.getInventory().countItem(Items.ELYTRA) > 0)
            return "Elytra Finder: Elytra in inventory";

        Entity nearest = null;
        double distance = Double.POSITIVE_INFINITY;
        var bounds = c.player.getBoundingBox().inflate(RADIUS);
        for (ItemEntity item : c.level.getEntitiesOfClass(ItemEntity.class, bounds,
                e -> e.getItem().is(Items.ELYTRA))) {
            double d = c.player.distanceTo(item);
            if (d < distance) { nearest = item; distance = d; }
        }
        for (ItemFrame frame : c.level.getEntitiesOfClass(ItemFrame.class, bounds,
                e -> e.getItem().is(Items.ELYTRA))) {
            double d = c.player.distanceTo(frame);
            if (d < distance) { nearest = frame; distance = d; }
        }
        if (nearest == null) return "Elytra Finder: None visible within 64 blocks";
        return String.format(Locale.ROOT,
            "Elytra Finder: XYZ %.0f %.0f %.0f (%.0fm)",
            nearest.getX(), nearest.getY(), nearest.getZ(), distance);
    }
}
