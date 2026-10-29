package com.dashclient;

import net.fabricmc.loader.api.FabricLoader;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Independent DashClient module profiles.
 * Only toggle states are copied; keybinds, HUD positions and Minecraft settings
 * remain user-controlled. Automatic mining never starts during a profile switch.
 */
public final class ClientProfiles {
    public static final String[] SLOTS = {"Main", "PvP", "FPS", "Survival"};
    private static final Path PATH = FabricLoader.getInstance().getConfigDir()
        .resolve("dashclient-profiles.properties");
    private static final Map<String, Properties> snapshots = new HashMap<>();
    private static String active = "Main";

    private ClientProfiles() {}

    public static String active() { return active; }

    public static void load() {
        Properties stored = new Properties();
        if (Files.isRegularFile(PATH)) {
            try (InputStream in = Files.newInputStream(PATH)) {
                stored.load(in);
            } catch (Exception ex) {
                DashClient.LOGGER.warn("Could not load DashClient profiles", ex);
            }
        }
        snapshots.clear();
        for (String slot : SLOTS) {
            Properties p = new Properties();
            String prefix = "profile." + slot + ".";
            for (Module module : DashClient.MODULES) {
                String key = prefix + module.name();
                if (stored.containsKey(key)) p.setProperty(module.name(), stored.getProperty(key));
            }
            if (!p.isEmpty()) snapshots.put(slot, p);
        }
        String saved = stored.getProperty("active", "Main");
        active = valid(saved) ? saved : "Main";
        // Config.load() already loaded the authoritative, last-used module
        // switches. Capture them so old installations migrate without loss.
        capture();
        persist();
    }

    private static boolean valid(String slot) {
        for (String value : SLOTS) if (value.equals(slot)) return true;
        return false;
    }

    private static void capture() {
        Properties p = new Properties();
        for (Module module : DashClient.MODULES) {
            boolean enabled = module.enabled() && !module.name().equals("Auto Miner");
            p.setProperty(module.name(), Boolean.toString(enabled));
        }
        snapshots.put(active, p);
    }

    public static void saveActive() {
        capture();
        persist();
    }

    public static void switchTo(String slot) {
        if (!valid(slot) || slot.equals(active)) return;
        capture();
        Properties target = snapshots.get(slot);
        if (target == null) {
            // New profiles start from current preferences, without guessing
            // what the user wants enabled on multiplayer servers.
            target = new Properties();
            target.putAll(snapshots.get(active));
            snapshots.put(slot, target);
        }
        for (Module module : DashClient.MODULES) {
            boolean enabled = Boolean.parseBoolean(target.getProperty(module.name(), "false"));
            if (module.name().equals("Auto Miner")) enabled = false;
            module.setEnabled(enabled);
        }
        active = slot;
        Config.save();
        persist();
    }

    private static void persist() {
        Properties all = new Properties();
        all.setProperty("active", active);
        for (Map.Entry<String, Properties> entry : snapshots.entrySet()) {
            for (String name : entry.getValue().stringPropertyNames()) {
                all.setProperty("profile." + entry.getKey() + "." + name,
                    entry.getValue().getProperty(name));
            }
        }
        try {
            Files.createDirectories(PATH.getParent());
            Path tmp = PATH.resolveSibling(PATH.getFileName() + ".tmp");
            try (OutputStream out = Files.newOutputStream(tmp)) {
                all.store(out, "DashClient independent module profiles");
            }
            Files.move(tmp, PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            DashClient.LOGGER.warn("Could not save DashClient profiles", ex);
        }
    }
}
