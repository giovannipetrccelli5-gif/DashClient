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

/** Screen-relative positions: survive GUI scaling/resolution changes. */
public final class HudLayout {
    public record Point(int x, int y) {}
    private record Normalized(double x, double y) {}
    private static final Path PATH = FabricLoader.getInstance().getConfigDir()
        .resolve("dashclient-hud-layout.properties");
    private static final Map<String,Normalized> CUSTOM = new HashMap<>();

    private HudLayout() {}

    public static boolean custom(Module module) {
        return CUSTOM.containsKey(module.name());
    }

    public static Point position(Module module, int visibleIndex, int screenW, int screenH,
                                 int textW, int lineHeight, boolean right) {
        Normalized saved = CUSTOM.get(module.name());
        int x, y;
        if (saved != null) {
            x = (int)Math.round(saved.x * screenW);
            y = (int)Math.round(saved.y * screenH);
        } else {
            int cellWidth = Math.min(190, Math.max(110, screenW - 12));
            int rows = Math.max(1, (screenH - 14) / lineHeight);
            int column = visibleIndex / rows;
            int row = visibleIndex % rows;
            x = right ? screenW - 25 - textW - column * (cellWidth + 4)
                : 25 + column * (cellWidth + 4);
            y = 18 + row * lineHeight;
            if (x + textW + 4 > screenW || x < 0) return null;
        }
        return new Point(clamp(x, 4, Math.max(4, screenW - textW - 4)),
            clamp(y, 4, Math.max(4, screenH - 14)));
    }

    public static void move(Module module, int x, int y, int screenW, int screenH,
                            int textWidth) {
        int clampedX = clamp(x, 4, Math.max(4, screenW - textWidth - 4));
        int clampedY = clamp(y, 4, Math.max(4, screenH - 14));
        CUSTOM.put(module.name(), new Normalized(
            (double)clampedX / Math.max(1, screenW),
            (double)clampedY / Math.max(1, screenH)));
    }

    private static int clamp(int n, int min, int max) {
        return Math.max(min, Math.min(max, n));
    }

    public static void reset() {
        CUSTOM.clear();
        save();
    }

    public static void load() {
        CUSTOM.clear();
        if (!Files.isRegularFile(PATH)) return;
        Properties props = new Properties();
        try (InputStream input = Files.newInputStream(PATH)) {
            props.load(input);
            for (String key : props.stringPropertyNames()) {
                String[] xy = props.getProperty(key).split(",", 2);
                if (xy.length != 2) continue;
                double x = Double.parseDouble(xy[0]);
                double y = Double.parseDouble(xy[1]);
                if (Double.isFinite(x) && Double.isFinite(y)
                        && x >= 0 && x <= 1 && y >= 0 && y <= 1)
                    CUSTOM.put(key, new Normalized(x,y));
            }
        } catch (Exception e) {
            DashClient.LOGGER.warn("Could not read saved HUD layout", e);
        }
    }

    public static void save() {
        Properties props = new Properties();
        CUSTOM.forEach((key, p) -> props.setProperty(key, p.x + "," + p.y));
        Path temp = PATH.resolveSibling(PATH.getFileName() + ".tmp");
        try {
            Files.createDirectories(PATH.getParent());
            try (OutputStream output = Files.newOutputStream(temp)) {
                props.store(output, "Drag modules in Edit HUD to change their positions");
            }
            Files.move(temp, PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            DashClient.LOGGER.warn("Could not save HUD layout", e);
        }
    }
}
