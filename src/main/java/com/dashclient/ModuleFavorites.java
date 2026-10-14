package com.dashclient;

import net.fabricmc.loader.api.FabricLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

/** Saved favorites for the touchscreen-friendly module browser. */
public final class ModuleFavorites {
    private static final Path PATH = FabricLoader.getInstance().getConfigDir()
        .resolve("dashclient-favorites.txt");
    private static final Set<String> NAMES = new HashSet<>();
    private ModuleFavorites() {}

    public static void load() {
        NAMES.clear();
        if (!Files.isRegularFile(PATH)) return;
        try {
            for (String line : Files.readAllLines(PATH, StandardCharsets.UTF_8))
                if (!line.isBlank() && line.length() <= 120) NAMES.add(line);
        } catch (Exception e) {
            DashClient.LOGGER.warn("Failed to load DashClient favorites", e);
        }
    }

    public static boolean contains(Module module) {
        return NAMES.contains(module.name());
    }

    public static void toggle(Module module) {
        if (!NAMES.remove(module.name())) NAMES.add(module.name());
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Path temp = PATH.resolveSibling(PATH.getFileName() + ".tmp");
            Files.write(temp, NAMES.stream().sorted().toList(), StandardCharsets.UTF_8);
            Files.move(temp, PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            DashClient.LOGGER.warn("Failed to save DashClient favorites", e);
        }
    }
}
