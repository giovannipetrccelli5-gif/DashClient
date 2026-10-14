package com.dashclient;

import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;

/** Avoid calculating dozens of HUD readouts every rendered frame. */
public final class HudFrameCache {
    public record Line(Module module, String text, int width) {}
    private static List<Line> lines = List.of();
    private static Object world, player;
    private static int ticks;
    private static boolean dirty = true;

    private HudFrameCache() {}
    public static void invalidate() { dirty = true; }
    public static List<Line> lines() { return lines; }

    public static void sample(Minecraft c) {
        if (c.player == null || c.level == null) {
            lines = List.of();
            world = player = null;
            ticks = 0;
            dirty = true;
            return;
        }
        if (world != c.level || player != c.player) {
            world = c.level;
            player = c.player;
            dirty = true;
        }

        int period = DashClient.isEnabled("HUD Eco Refresh") ? 20 :
            DashClient.isEnabled("HUD Fast Refresh") ? 1 : 5;
        if (!dirty && ++ticks < period) return;
        ticks = 0;
        dirty = false;

        int maxWidth = Math.min(190,
            Math.max(100, c.getWindow().getGuiScaledWidth() - 12)) - 12;
        int limit = DashClient.isEnabled("HUD Minimal Mode") ? 8 : Integer.MAX_VALUE;
        ArrayList<Line> next = new ArrayList<>();
        for (Module m : DashClient.MODULES) {
            if (!m.enabled()) continue;
            String value = m.hudText(c);
            if (value == null || value.isBlank()) continue;
            value = ModuleSymbols.symbol(m) + " " + value;
            while (!value.isEmpty() && c.font.width(value) > maxWidth)
                value = value.substring(0, value.length() - 1);
            if (value.isEmpty()) continue;
            next.add(new Line(m, value, c.font.width(value)));
            if (next.size() >= limit) break;
        }
        lines = List.copyOf(next);
    }
}
