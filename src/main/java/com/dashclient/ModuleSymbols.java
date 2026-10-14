package com.dashclient;

import java.util.Locale;

/** Font-friendly symbols visible for every module, including HUD readouts. */
public final class ModuleSymbols {
    private ModuleSymbols() {}
    public static String symbol(Module m) {
        String n = m.name().toLowerCase(Locale.ROOT);
        if (n.contains("fps") || n.contains("frame") || n.contains("performance")
            || n.contains("battery") || n.contains("diagnostic")) return "⚡";
        if (n.contains("health") || n.contains("heart") || n.contains("absorption")) return "♥";
        if (n.contains("target") || n.contains("crosshair") || n.contains("aura")) return "◎";
        if (n.equals("auto totem")) return "✚";
        if (n.contains("elytra")) return "➤";
        if (n.contains("freecam") || n.contains("esp") || n.contains("glow") || n.contains("bright")
            || n.contains("x-ray")) return "✧";
        if (n.contains("jump") || n.contains("fly") || n.contains("vertical")) return "↑";
        if (n.contains("walk") || n.contains("sprint") || n.contains("speed")) return "➤";
        if (n.contains("village") || n.contains("compass") || n.contains("distance") || n.contains("radar")
            || n.contains("coordinate") || n.contains("direction")) return "⌖";
        if (n.contains("item") || n.contains("armor") || n.contains("inventory")
            || n.contains("durability") || n.contains("totem")) return "◆";
        if (n.contains("click") || n.contains("attack") || n.contains("combat")) return "⚔";
        if (n.contains("time") || n.contains("clock")) return "◷";
        if (n.contains("light") || n.contains("sun") || n.contains("day")) return "☀";
        if (n.contains("water") || n.contains("lava") || n.contains("weather")) return "☁";
        return "✦";
    }
}
