package com.dashclient.modules;

import com.dashclient.DashClient;
import com.dashclient.Module;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.server.level.ParticleStatus;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Graphics presets using vanilla settings; never hooks other mod renderers. */
public final class PerformanceModule extends Module {
    private static final Path BACKUP = FabricLoader.getInstance().getConfigDir()
        .resolve("dashclient-performance-backup.properties");
    private static Properties original;
    private static PerformanceModule active;
    private static int savedView;
    private final boolean extreme;
    private int ticks, low, high;

    public PerformanceModule() { this("Performance Boost", false); }
    public PerformanceModule(String name, boolean extreme) {
        super(name);
        this.extreme = extreme;
    }

    @Override public void onEnable() {
        // Presets cannot conflict; restore first before applying another preset.
        if (active != null && active != this && active.enabled()) active.setEnabled(false);
        Options o = Minecraft.getInstance().options;
        Properties p = readBackup();
        if (p == null) {
            p = snapshot(o);
            if (!writeBackup(p)) {
                DashClient.LOGGER.warn("Cannot activate FPS mode: backup not saved");
                setEnabled(false);
                return;
            }
        }
        original = p;
        active = this;
        savedView = number(p, "render", o.renderDistance().get());
        ticks = low = high = 0;
        o.renderDistance().set(Math.min(o.renderDistance().get(), extreme ? 4 : 6));
        o.simulationDistance().set(Math.min(o.simulationDistance().get(), extreme ? 4 : 5));
        o.particles().set(ParticleStatus.MINIMAL);
        o.cloudStatus().set(CloudStatus.OFF);
        o.entityShadows().set(false);
        o.biomeBlendRadius().set(0);
        o.save();
    }

    @Override public void onTick(Minecraft c) {
        if (active != this || c.player == null || c.level == null
                || !DashClient.isEnabled("Adaptive FPS")) return;
        if (++ticks < 100) return; // Sample every ~5 seconds instead of every tick.
        ticks = 0;
        int fps = c.getFps();
        if (fps > 0 && fps < 45) { low++; high = 0; }
        else if (fps > 85) { high++; low = 0; }
        else { high = low = 0; }
        int current = c.options.renderDistance().get();
        int max = Math.max(4, Math.min(savedView, extreme ? 8 : 12));
        int next = AdaptiveDistancePolicy.next(current, 4, max, low, high);
        if (next != current) {
            c.options.renderDistance().set(next);
            low = high = 0;
        }
    }

    public static void resetAdaptiveDistance() {
        if (active == null) return;
        Options o = Minecraft.getInstance().options;
        o.renderDistance().set(Math.min(savedView, active.extreme ? 4 : 6));
        active.ticks = active.low = active.high = 0;
    }

    @Override public void onDisable() {
        if (active != this) return;
        active = null;
        Properties p = original;
        original = null;
        if (p == null) return;
        Options o = Minecraft.getInstance().options;
        try {
            o.renderDistance().set(number(p, "render", o.renderDistance().get()));
            o.simulationDistance().set(number(p, "simulation", o.simulationDistance().get()));
            if (p.getProperty("particles") != null)
                o.particles().set(ParticleStatus.valueOf(p.getProperty("particles")));
            if (p.getProperty("clouds") != null)
                o.cloudStatus().set(CloudStatus.valueOf(p.getProperty("clouds")));
            if (p.getProperty("shadows") != null)
                o.entityShadows().set(Boolean.parseBoolean(p.getProperty("shadows")));
            o.biomeBlendRadius().set(number(p, "biome", o.biomeBlendRadius().get()));
            o.save();
            Files.deleteIfExists(BACKUP);
        } catch (Exception e) {
            DashClient.LOGGER.warn("Cannot restore original graphics settings; kept {}", BACKUP, e);
        }
    }

    private static int number(Properties p, String key, int fallback) {
        try { return Integer.parseInt(p.getProperty(key)); }
        catch (Exception e) { return fallback; }
    }

    private static Properties snapshot(Options o) {
        Properties p = new Properties();
        p.setProperty("render", "" + o.renderDistance().get());
        p.setProperty("simulation", "" + o.simulationDistance().get());
        p.setProperty("particles", o.particles().get().name());
        p.setProperty("clouds", o.cloudStatus().get().name());
        p.setProperty("shadows", "" + o.entityShadows().get());
        p.setProperty("biome", "" + o.biomeBlendRadius().get());
        return p;
    }

    private static Properties readBackup() {
        if (!Files.isRegularFile(BACKUP)) return null;
        try (InputStream in = Files.newInputStream(BACKUP)) {
            Properties p = new Properties();
            p.load(in);
            if (p.getProperty("render") == null || p.getProperty("simulation") == null)
                throw new IllegalStateException("Incomplete backup");
            return p;
        } catch (Exception e) {
            DashClient.LOGGER.warn("Unable to read FPS backup", e);
            return null;
        }
    }

    private static boolean writeBackup(Properties p) {
        Path temp = BACKUP.resolveSibling(BACKUP.getFileName() + ".tmp");
        try {
            Files.createDirectories(BACKUP.getParent());
            try (OutputStream out = Files.newOutputStream(temp))
                { p.store(out, "DashClient original graphics options"); }
            Files.move(temp, BACKUP, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            DashClient.LOGGER.warn("Unable to save FPS backup", e);
            return false;
        }
    }
}
