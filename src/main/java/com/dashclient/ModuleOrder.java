package com.dashclient;

import net.fabricmc.loader.api.FabricLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.StandardCopyOption;

/** Persisted visual ordering of module cards (does not mutate module registration). */
public final class ModuleOrder {
    private static final Path PATH = FabricLoader.getInstance().getConfigDir()
        .resolve("dashclient-module-order.txt");
    private static final List<String> ORDER = new ArrayList<>();
    private static final Map<String,Integer> INDEX = new HashMap<>();
    private static void rebuildIndex() {
        INDEX.clear();
        for (int i = 0; i < ORDER.size(); i++) INDEX.put(ORDER.get(i), i);
    }
    private ModuleOrder() {}

    public static void load(List<Module> modules) {
        ORDER.clear();
        HashSet<String> known = new HashSet<>();
        for (Module m : modules) known.add(m.name());
        if (Files.isRegularFile(PATH)) {
            try {
                for (String name : Files.readAllLines(PATH, StandardCharsets.UTF_8))
                    if (known.contains(name) && !ORDER.contains(name)) ORDER.add(name);
            } catch (Exception e) {
                DashClient.LOGGER.warn("Could not load DashClient module order", e);
            }
        }
        for (Module m : modules) if (!ORDER.contains(m.name())) ORDER.add(m.name());
        rebuildIndex();
    }

    public static int indexOf(Module module) {
        return INDEX.getOrDefault(module.name(), Integer.MAX_VALUE);
    }

    public static void move(Module source, Module target) {
        if (source == target || !ORDER.remove(source.name())) return;
        int idx = ORDER.indexOf(target.name());
        ORDER.add(idx < 0 ? ORDER.size() : idx, source.name());
        rebuildIndex();
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Path temp = PATH.resolveSibling(PATH.getFileName() + ".tmp");
            Files.write(temp, ORDER, StandardCharsets.UTF_8);
            Files.move(temp, PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            DashClient.LOGGER.warn("Could not save DashClient module order", e);
        }
    }
}
