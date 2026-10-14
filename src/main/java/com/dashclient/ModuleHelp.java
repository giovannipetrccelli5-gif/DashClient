package com.dashclient;

import java.util.Locale;

/** Lightweight inline descriptions shown without opening popups on Android. */
public final class ModuleHelp {
    private ModuleHelp() {}

    public static String describe(Module module) {
        if (module == null) return "Tap a module to toggle | Star to favorite | Search above";
        return switch (module.name()) {
            case "Performance Boost" -> "Reduces GPU/CPU work using saved, reversible graphics settings";
            case "Extreme FPS" -> "Minimal graphics and short view distance for demanding worlds";
            case "Battery Saver" -> "Lower power draw with a 45 FPS cap and minimal effects";
            case "Adaptive FPS" -> "Adjusts render distance based on recent FPS";
            case "HUD Eco Refresh" -> "Refreshes HUD text once per second instead of every frame";
            case "HUD Fast Refresh" -> "Samples enabled HUD readouts every game tick";
            case "Auto Clicker" -> "K to toggle | 10 CPS native vanilla attack input";
            case "X-Ray" -> "X to toggle | Highlights ores through walls in loaded chunks (20-block radius)";
            case "Freecam" -> "F6 to toggle | Fly a local spectator camera (WASD, Space, Shift, Sprint)";
            case "Module List HUD" -> "Draggable list of up to twelve active modules";
            case "Clean Screenshot HUD" -> "Hides DashClient overlays while enabled";
            case "Streamer Privacy HUD" -> "Hides common names, coords and server readouts";
            case "Auto Totem" -> "Moves spare totems into the offhand when available";
            case "Auto Elytra" -> "Equips an Elytra from inventory and starts normal gliding";
            case "Elytra Finder" -> "Finds dropped or displayed Elytras in loaded chunks";
            case "Village Finder" -> "V to toggle | Shows coordinates near loaded villagers and village clues";
            case "Armor/Item HUD" -> "Draggable helmet, chestplate, leggings, boots and totem icons";
            case "Auto Sneak" -> "Holds crouch while playing; releases it in menus";
            case "Auto Use" -> "Holds use while playing; releases it in menus";
            default -> {
                String n = module.name().toLowerCase(Locale.ROOT);
                if (n.startsWith("gear:")) yield "Equipment and inventory information";
                if (n.startsWith("pvp:")) yield "Local combat status and readiness information";
                if (n.startsWith("flight:")) yield "Elytra and movement information";
                if (n.startsWith("jvm ") || n.startsWith("diagnostic:"))
                    yield "Diagnostic information; not a guaranteed FPS boost";
                if (n.startsWith("count:")) yield "Shows the count of this item in your inventory";
                yield module.name() + (module.enabled() ? " is ON" : " is OFF");
            }
        };
    }
}
