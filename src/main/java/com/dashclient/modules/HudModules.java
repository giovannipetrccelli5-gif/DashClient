package com.dashclient.modules;

import com.dashclient.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

public final class HudModules {
    private HudModules() {}

    public static final class Fps extends Module {
        public Fps() { super("FPS Display"); }
        @Override public String hudText(Minecraft c) { return "FPS: " + c.getFps(); }
    }

    public static final class Coordinates extends Module {
        public Coordinates() { super("Coordinates"); }
        @Override public String hudText(Minecraft c) {
            if (c.player == null) return null;
            return String.format(Locale.ROOT, "XYZ: %.0f / %.0f / %.0f",
                c.player.getX(), c.player.getY(), c.player.getZ());
        }
    }

    public static final class Direction extends Module {
        public Direction() { super("Direction"); }
        @Override public String hudText(Minecraft c) {
            return c.player == null ? null :
                "Facing: " + c.player.getDirection().getName().toUpperCase(Locale.ROOT);
        }
    }

    public static final class Clock extends Module {
        private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
        public Clock() { super("Clock"); }
        @Override public String hudText(Minecraft c) {
            return "Time: " + LocalTime.now().format(FORMAT);
        }
    }

    // Samples the attack key every client tick, independently of HUD visibility.
    // Very short clicks between ticks may still be missed.
    public static final class Cps extends Module {
        private final Deque<Long> clicks = new ArrayDeque<>();
        private boolean pressed;
        public Cps() { super("CPS Counter"); }

        @Override public void onTick(Minecraft c) {
            if (c.player == null || c.gui.screen() != null) {
                clicks.clear();
                pressed = false;
                return;
            }
            boolean down = c.options.keyAttack.isDown();
            long now = System.nanoTime();
            if (down && !pressed) clicks.addLast(now);
            pressed = down;
            while (!clicks.isEmpty() && now - clicks.peekFirst() > 1_000_000_000L)
                clicks.removeFirst();
        }

        @Override public String hudText(Minecraft c) {
            return "CPS: " + clicks.size();
        }

        @Override public void onDisable() {
            clicks.clear();
            pressed = false;
        }
    }

    public static final class ArmorItems extends Module {
        public ArmorItems() { super("Armor/Item HUD"); }
        @Override public String hudText(Minecraft c) {
            if (c.player == null) return null;
            StringBuilder sb = new StringBuilder("Held: ");
            ItemStack held = c.player.getMainHandItem();
            sb.append(held.isEmpty() ? "Empty" : held.getHoverName().getString());
            sb.append(" | Armor: ");
            for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
            }) {
                ItemStack stack = c.player.getItemBySlot(slot);
                sb.append(stack.isEmpty() ? "-" :
                    (stack.isDamageableItem() ? Math.max(0,stack.getMaxDamage()-stack.getDamageValue()) : "OK"));
                sb.append(' ');
            }
            return sb.toString().trim();
        }
    }
}
