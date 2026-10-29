package com.dashclient;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayDeque;
import java.util.Locale;

/**
 * Lightweight client-only statistics. Never transmits player information or
 * reads server packets outside normal Minecraft APIs.
 */
public final class LiveMetrics {
    private static final ArrayDeque<Long> leftClicks = new ArrayDeque<>();
    private static final ArrayDeque<Long> rightClicks = new ArrayDeque<>();
    private static boolean previousAttack, previousUse;
    private static final int[] fps = new int[120];
    private static int count, index;

    private LiveMetrics() {}

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.gui.screen() != null) {
            previousAttack = previousUse = false;
            leftClicks.clear();
            rightClicks.clear();
            return;
        }
        long now = System.currentTimeMillis();
        boolean attacking = mc.options.keyAttack.isDown();
        boolean using = mc.options.keyUse.isDown();
        if (attacking && !previousAttack) leftClicks.addLast(now);
        if (using && !previousUse) rightClicks.addLast(now);
        previousAttack = attacking;
        previousUse = using;
        expire(leftClicks, now);
        expire(rightClicks, now);
        fps[index] = Math.max(0, mc.getFps());
        index = (index + 1) % fps.length;
        count = Math.min(count + 1, fps.length);
    }

    private static void expire(ArrayDeque<Long> q, long now) {
        while (!q.isEmpty() && now - q.peekFirst() >= 1000L) q.removeFirst();
    }

    private static String itemName(ItemStack stack) {
        if (stack.isEmpty()) return "Empty";
        return stack.getHoverName().getString();
    }

    public static String readout(String id, Minecraft mc) {
        if (mc.player == null) return "";
        var player = mc.player;
        var pos = player.blockPosition();
        return switch (id) {
            case "cps" -> "Left CPS  " + leftClicks.size();
            case "right_cps" -> "Right CPS  " + rightClicks.size();
            case "keys" -> "W " + (mc.options.keyUp.isDown() ? "*" : "-")
                + " A " + (mc.options.keyLeft.isDown() ? "*" : "-")
                + " S " + (mc.options.keyDown.isDown() ? "*" : "-")
                + " D " + (mc.options.keyRight.isDown() ? "*" : "-");
            case "sprinting" -> player.isSprinting() ? "Sprint  ON" : "Sprint  OFF";
            case "sneaking" -> player.isShiftKeyDown() ? "Sneak  ON" : "Sneak  OFF";
            case "hunger" -> "Food  " + player.getFoodData().getFoodLevel() + "/20";
            case "armor" -> "Armor  " + player.getArmorValue();
            case "xp" -> "XP level  " + player.experienceLevel;
            case "chunk" -> "Chunk  " + (pos.getX() >> 4) + ", " + (pos.getZ() >> 4);
            case "altitude" -> "Y level  " + pos.getY();
            case "block" -> "Standing on  " + itemNameOfBlock(mc);
            case "biome" -> "Biome  " + mc.level.getBiome(pos).getRegisteredName();
            case "weather" -> mc.level.isRaining() ? "Weather  Rain" : "Weather  Clear";
            case "light" -> "Light  " + mc.level.getMaxLocalRawBrightness(pos);
            case "held" -> "Held  " + itemName(player.getMainHandItem());
            case "offhand" -> "Offhand  " + itemName(player.getOffhandItem());
            case "held_count" -> "Stack  " + player.getMainHandItem().getCount();
            case "durability" -> {
                ItemStack stack = player.getMainHandItem();
                yield stack.isDamageableItem() ?
                    "Durability  " + (stack.getMaxDamage() - stack.getDamageValue())
                        + "/" + stack.getMaxDamage() : "Durability  N/A";
            }
            case "totems" -> "Totems  " + countItems(mc, Items.TOTEM_OF_UNDYING);
            case "arrows" -> "Arrows  " + countItems(mc, Items.ARROW);
            case "effects" -> "Potion Effects  " + player.getActiveEffects().size();
            case "server" -> "Server  " + (mc.getCurrentServer() == null ?
                "Singleplayer" : mc.getCurrentServer().ip);
            case "playername" -> "Player  " + player.getName().getString();
            case "fps_average" -> "Avg FPS  " + (count == 0 ? 0 : sum() / count);
            case "fps_low" -> "Lowest FPS  " + minimum();
            case "fps_high" -> "Highest FPS  " + maximum();
            case "memory_percent" -> {
                Runtime rt=Runtime.getRuntime();
                yield "Heap  " + Math.round(100.0 * (rt.totalMemory()-rt.freeMemory())
                    / Math.max(1,rt.maxMemory())) + "%";
            }
            case "nethercoords" -> {
                boolean nether = mc.level.dimension().toString().contains("the_nether");
                int scale = nether ? 8 : 1;
                if (nether) yield "Overworld  " + (pos.getX()*scale) + ", " + (pos.getZ()*scale);
                yield "Nether  " + (pos.getX()/8) + ", " + (pos.getZ()/8);
            }
            case "waypoint" -> Waypoints.nearest(mc);
            default -> "";
        };
    }
    private static String itemNameOfBlock(Minecraft mc) {
        return mc.level.getBlockState(mc.player.blockPosition().below())
            .getBlock().getName().getString();
    }
    private static int countItems(Minecraft mc, net.minecraft.world.item.Item item) {
        int total = 0;
        var inventory = mc.player.getInventory();
        for (int i=0; i<inventory.getContainerSize();i++) {
            ItemStack stack=inventory.getItem(i);
            if (stack.is(item)) total+=stack.getCount();
        }
        return total;
    }
    private static int sum() { int s=0; for(int i=0;i<count;i++)s+=fps[i];return s; }
    private static int minimum() {
        if(count==0)return 0;
        int s=Integer.MAX_VALUE;for(int i=0;i<count;i++)s=Math.min(s,fps[i]);return s;
    }
    private static int maximum() {
        int s=0;for(int i=0;i<count;i++)s=Math.max(s,fps[i]);return s;
    }
}
