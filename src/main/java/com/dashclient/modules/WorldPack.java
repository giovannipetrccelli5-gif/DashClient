package com.dashclient.modules;

import com.dashclient.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** Environment, target and debug-like HUD information. */
public final class WorldPack {
    private WorldPack() {}
    private static void add(List<Module> list, String name, Function<Minecraft,String> display) {
        list.add(new Module(name) {
            @Override public String hudText(Minecraft c) { return display.apply(c); }
        });
    }
    private static BlockHitResult block(Minecraft c) {
        return c.hitResult instanceof BlockHitResult hit && c.level != null ? hit : null;
    }
    private static EntityHitResult entity(Minecraft c) {
        return c.hitResult instanceof EntityHitResult hit ? hit : null;
    }
    public static void register(List<Module> list) {
        add(list, "Target Name", c -> {
            var target = entity(c);
            return target == null ? null : "Target: " + target.getEntity().getName().getString();
        });
        add(list, "Target Health", c -> {
            var target = entity(c);
            if (target == null || !(target.getEntity() instanceof LivingEntity living)) return null;
            return String.format(Locale.ROOT, "Target HP: %.1f/%.1f",
                living.getHealth(), living.getMaxHealth());
        });
        add(list, "Target Range", c -> {
            var target = entity(c);
            if (target == null || c.player == null) return null;
            return String.format(Locale.ROOT, "Target distance: %.2f",
                c.player.distanceTo(target.getEntity()));
        });
        add(list, "Target Type", c -> {
            var target = entity(c);
            return target == null ? null : "Target type: " +
                target.getEntity().getType().getDescription().getString();
        });
        add(list, "Target Block XYZ", c -> {
            var hit = block(c);
            if (hit == null) return null;
            var pos = hit.getBlockPos();
            return "Looking XYZ: " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
        });
        add(list, "Target Block Range", c -> {
            var hit = block(c);
            return hit == null || c.player == null ? null :
                String.format(Locale.ROOT, "Block distance: %.2f",
                    c.player.position().distanceTo(hit.getLocation()));
        });
        add(list, "Target Block State", c -> {
            var hit = block(c);
            return hit == null ? null :
                "Block state: " + c.level.getBlockState(hit.getBlockPos()).toString();
        });
        add(list, "Block Hardness", c -> {
            var hit = block(c);
            return hit == null ? null :
                String.format(Locale.ROOT, "Hardness: %.1f",
                    c.level.getBlockState(hit.getBlockPos())
                        .getDestroySpeed(c.level, hit.getBlockPos()));
        });
        add(list, "Block Light", c -> {
            var hit = block(c);
            return hit == null ? null : "Block light: " +
                c.level.getBrightness(LightLayer.BLOCK, hit.getBlockPos());
        });
        add(list, "Sky Light", c -> {
            var hit = block(c);
            return hit == null ? null : "Sky light: " +
                c.level.getBrightness(LightLayer.SKY, hit.getBlockPos());
        });
        add(list, "Dimension", c -> c.level == null ? null :
            "Dimension: " + c.level.dimension().toString());
        add(list, "Weather", c -> c.level == null ? null :
            "Weather: " + (c.level.isThundering() ? "Thunder" :
                c.level.isRaining() ? "Rain" : "Clear"));
        add(list, "World Days", c -> c.level == null ? null :
            "World age: " + (c.level.getLevelData().getGameTime() / 24000L) + " days");
        add(list, "Online Players", c -> c.getConnection() == null ? null :
            "Online: " + c.getConnection().getOnlinePlayers().size());
        add(list, "Yaw", c -> c.player == null ? null :
            String.format(Locale.ROOT, "Yaw: %.1f", c.player.getYRot()));
        add(list, "Pitch", c -> c.player == null ? null :
            String.format(Locale.ROOT, "Pitch: %.1f", c.player.getXRot()));
        add(list, "Nether Coordinates", c -> {
            if (c.player == null || c.level == null) return null;
            String d = c.level.dimension().toString();
            if (d.contains("the_end")) return null;
            double scale = d.contains("the_nether") ? 8.0 : 0.125;
            return String.format(Locale.ROOT, "%s XYZ: %.0f / %.0f",
                scale == 8.0 ? "Overworld" : "Nether",
                c.player.getX() * scale, c.player.getZ() * scale);
        });
        add(list, "Chunk Offset", c -> c.player == null ? null :
            "In chunk: " + (c.player.blockPosition().getX() & 15) +
                " / " + (c.player.blockPosition().getZ() & 15));
    }
}
