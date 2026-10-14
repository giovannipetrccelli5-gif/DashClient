package com.dashclient.modules;

import com.dashclient.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Items;
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

    /** Real equipment icons, not a text representation of armor durability. */
    public static final class ArmorItems extends Module {
        private static final int SLOT_WIDTH = 22;
        public ArmorItems() { super("Armor/Item HUD"); }

        @Override public boolean customHud() { return true; }
        @Override public int hudWidth(Minecraft c) { return 120; }
        @Override public int hudHeight(Minecraft c) { return SLOT_WIDTH; }
        @Override public String hudText(Minecraft c) { return null; }

        @Override public void renderHud(Minecraft c, GuiGraphicsExtractor g, int x, int y) {
            ItemStack helmet = c.player == null ? ItemStack.EMPTY :
                c.player.getItemBySlot(EquipmentSlot.HEAD);
            ItemStack chest = c.player == null ? ItemStack.EMPTY :
                c.player.getItemBySlot(EquipmentSlot.CHEST);
            ItemStack legs = c.player == null ? ItemStack.EMPTY :
                c.player.getItemBySlot(EquipmentSlot.LEGS);
            ItemStack boots = c.player == null ? ItemStack.EMPTY :
                c.player.getItemBySlot(EquipmentSlot.FEET);

            drawSlot(c, g, helmet, x, y, null);
            drawSlot(c, g, chest, x + 23, y, null);
            drawSlot(c, g, legs, x + 46, y, null);
            drawSlot(c, g, boots, x + 69, y, null);

            // Draw the real totem icon with a count including stored + offhand.
            int totems = countTotems(c);
            drawSlot(c, g, totems > 0 ? new ItemStack(Items.TOTEM_OF_UNDYING)
                : ItemStack.EMPTY, x + 98, y, totems > 1 ? Integer.toString(totems) : null);
        }

        private static void drawSlot(Minecraft c, GuiGraphicsExtractor g, ItemStack stack,
                                     int x, int y, String count) {
            // Simple hotbar-style frame; color remains visible for empty slots.
            g.fill(x, y, x + SLOT_WIDTH, y + SLOT_WIDTH, 0xFF636773);
            g.fill(x + 1, y + 1, x + SLOT_WIDTH - 1, y + SLOT_WIDTH - 1, 0xDD171A22);
            if (!stack.isEmpty()) {
                g.item(stack, x + 3, y + 3);
                g.itemDecorations(c.font, stack, x + 3, y + 3, count);
            }
        }

        private static int countTotems(Minecraft c) {
            if (c.player == null) return 0;
            int total = 0;
            var inventory = c.player.getInventory();
            for (int i = 0; i < Math.min(36, inventory.getContainerSize()); i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.is(Items.TOTEM_OF_UNDYING)) total += stack.getCount();
            }
            ItemStack offhand = c.player.getOffhandItem();
            if (offhand.is(Items.TOTEM_OF_UNDYING)) total += offhand.getCount();
            return total;
        }
    }
}
