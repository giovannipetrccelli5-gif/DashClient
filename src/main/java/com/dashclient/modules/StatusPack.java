package com.dashclient.modules;

import com.dashclient.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** Movement states, session tracking and HUD styling switches. */
public final class StatusPack {
    private StatusPack() {}
    private static void add(List<Module> list, String name, Function<Minecraft,String> display) {
        list.add(new Module(name) {
            @Override public String hudText(Minecraft c) { return display.apply(c); }
        });
    }
    private static void style(List<Module> list, String name) {
        list.add(new Module(name) {});
    }
    public static void register(List<Module> list) {
        add(list, "Sprint State", c -> c.player == null ? null :
            "Sprinting: " + (c.player.isSprinting() ? "YES" : "NO"));
        add(list, "Sneak State", c -> c.player == null ? null :
            "Sneaking: " + (c.player.isShiftKeyDown() ? "YES" : "NO"));
        add(list, "Swim State", c -> c.player == null ? null :
            "Swimming: " + (c.player.isSwimming() ? "YES" : "NO"));
        add(list, "Elytra State", c -> c.player == null ? null :
            "Elytra flying: " + (c.player.isFallFlying() ? "YES" : "NO"));
        add(list, "Creative Flight", c -> c.player == null ? null :
            "Creative flying: " + (c.player.getAbilities().flying ? "YES" : "NO"));
        add(list, "Ground State", c -> c.player == null ? null :
            "On ground: " + (c.player.onGround() ? "YES" : "NO"));
        add(list, "Fire Warning", c ->
            c.player != null && c.player.isOnFire() ? "ON FIRE!" : null);
        add(list, "Vertical Speed", c -> c.player == null ? null :
            String.format(Locale.ROOT, "Vertical: %+.2f blocks/s",
                c.player.getDeltaMovement().y * 20.0));
        list.add(new SessionDistance());
        list.add(new PeakSpeed());
        add(list, "Low Health Warning", c ->
            c.player != null && c.player.getHealth() <= 8.0f ?
                String.format(Locale.ROOT, "LOW HEALTH: %.1f HP", c.player.getHealth()) : null);
        add(list, "Low Durability Warning", c -> {
            if (c.player == null) return null;
            var hand = c.player.getMainHandItem();
            if (hand.isEmpty() || !hand.isDamageableItem()) return null;
            int remaining = hand.getMaxDamage() - hand.getDamageValue();
            return remaining <= 25 ? "LOW DURABILITY: " + remaining : null;
        });
        // HUD presentation modules are switches, not empty placeholder readouts.
        style(list, "HUD Rainbow");
        style(list, "HUD Right Align");
        style(list, "HUD Transparent");
        style(list, "HUD Gameplay Only");
        style(list, "HUD Compact");
    }

    private static final class SessionDistance extends Module {
        private Vec3 last;
        private Object world;
        private double blocks;
        private SessionDistance() { super("Session Distance"); }
        @Override public void onEnable() { blocks = 0; last = null; world = null; }
        @Override public void onDisable() { last = null; world = null; }
        @Override public void onTick(Minecraft c) {
            if (c.player == null || c.level == null) { last = null; world = null; return; }
            Vec3 now = c.player.position();
            if (world != c.level) { world = c.level; last = now; blocks = 0; }
            if (last != null) {
                double traveled = now.distanceTo(last);
                if (traveled <= 15.0) blocks += traveled; // Exclude teleports.
            }
            last = now;
        }
        @Override public String hudText(Minecraft c) {
            return c.player == null ? null :
                String.format(Locale.ROOT, "Session travel: %.1f blocks", blocks);
        }
    }

    private static final class PeakSpeed extends Module {
        private double peak;
        private Object world;
        private PeakSpeed() { super("Peak Speed"); }
        @Override public void onEnable() { peak = 0; world = null; }
        @Override public void onTick(Minecraft c) {
            if (c.player == null || c.level == null) { world = null; return; }
            if (world != c.level) { world = c.level; peak = 0; }
            double speed = c.player.getDeltaMovement().horizontalDistance() * 20.0;
            if (speed < 300 && speed > peak) peak = speed;
        }
        @Override public String hudText(Minecraft c) {
            return c.player == null ? null :
                String.format(Locale.ROOT, "Peak speed: %.2f blocks/s", peak);
        }
    }
}
