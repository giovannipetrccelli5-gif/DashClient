package com.dashclient;

import net.fabricmc.loader.api.FabricLoader;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
        for (Module module : DashClient.MODULES) {
            boolean defaultOn = module.name().equals("FPS Display")
                || module.name().equals("Coordinates");
            boolean enabled = Boolean.parseBoolean(props.getProperty(key(module), "" + defaultOn));
            module.setEnabled(enabled);
        }
    }

    public static void save() {
        Properties props = new Properties();
        for (Module module : DashClient.MODULES)
            props.setProperty(key(module), Boolean.toString(module.enabled()));
        try {
            Files.createDirectories(PATH.getParent());
            try (OutputStream out = Files.newOutputStream(PATH)) {
                props.store(out, "DashClient module switches");
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
