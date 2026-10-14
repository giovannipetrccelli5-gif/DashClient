package com.dashclient;

import net.fabricmc.loader.api.FabricLoader;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.Properties;

public final class Config {
    private static final Path PATH =
        FabricLoader.getInstance().getConfigDir().resolve("dashclient.properties");

    private Config() {}

    public static void load() {
        Properties props = new Properties();
        if (Files.isRegularFile(PATH)) {
            try (InputStream in = Files.newInputStream(PATH)) {
                props.load(in);
            } catch (Exception e) {
                DashClient.LOGGER.warn("Could not read DashClient config", e);
            }
        }
        // One-time upgrade for existing installations too. Later manual
        // changes must remain disabled when the user turns a preset off.
        boolean migrated = PerformanceDefaults.apply(props);
        for (Module module : DashClient.MODULES) {
            boolean defaultOn = module.name().equals("FPS Display")
                || module.name().equals("Coordinates")
                || module.name().equals("Performance Boost")
                || module.name().equals("Adaptive FPS")
                || module.name().equals("HUD Eco Refresh");
            boolean enabled = Boolean.parseBoolean(props.getProperty(key(module), "" + defaultOn));
            // Never resume an autonomous miner after startup or reconnect.
            if (module.name().equals("Auto Miner")) enabled = false;
            module.setEnabled(enabled);
        }
        if (migrated) save();
    }

    public static void save() {
        Properties props = new Properties();
        // Persist the migration marker so explicit OFF choices survive restart.
        props.setProperty(PerformanceDefaults.MARKER, "1");
        for (Module module : DashClient.MODULES)
            props.setProperty(key(module), Boolean.toString(module.enabled()));
        Path tmp = PATH.resolveSibling(PATH.getFileName() + ".tmp");
        try {
            Files.createDirectories(PATH.getParent());
            try (OutputStream out = Files.newOutputStream(tmp)) {
                props.store(out, "DashClient module switches");
            }
            try {
                Files.move(tmp, PATH, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            DashClient.LOGGER.warn("Could not save DashClient config", e);
        }
    }

    private static String key(Module module) {
        return module.name().toLowerCase(java.util.Locale.ROOT)
            .replace(' ', '_').replace('/', '_');
    }
}
