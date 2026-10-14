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
import java.util.Properties;

public final class PerformanceModule extends Module {
    private static final Path BACKUP = FabricLoader.getInstance()
        .getConfigDir().resolve("dashclient-performance-backup.properties");
    private Properties before;

    public PerformanceModule() { super("Performance Boost"); }

    @Override public void onEnable() {
        Options o = Minecraft.getInstance().options;
        before = new Properties();
        if (Files.isRegularFile(BACKUP)) {
            try (InputStream in = Files.newInputStream(BACKUP)) {
                before.load(in);
            } catch (Exception e) {
                DashClient.LOGGER.warn("Cannot read performance backup", e);
            }
        }
        if (before.isEmpty()) {
            before.setProperty("render", "" + o.renderDistance().get());
            before.setProperty("simulation", "" + o.simulationDistance().get());
            before.setProperty("particles", o.particles().get().name());
            before.setProperty("clouds", o.cloudStatus().get().name());
            before.setProperty("shadows", "" + o.entityShadows().get());
            before.setProperty("biome", "" + o.biomeBlendRadius().get());
            try {
                Files.createDirectories(BACKUP.getParent());
                try (OutputStream out = Files.newOutputStream(BACKUP)) {
                    before.store(out, "Original settings before Performance Boost");
                }
            } catch (Exception e) {
                DashClient.LOGGER.warn("Cannot save performance backup", e);
            }
        }
        o.renderDistance().set(Math.min(o.renderDistance().get(), 6));
        o.simulationDistance().set(Math.min(o.simulationDistance().get(), 5));
        o.particles().set(ParticleStatus.MINIMAL);
        o.cloudStatus().set(CloudStatus.OFF);
        o.entityShadows().set(false);
        o.biomeBlendRadius().set(0);
    }

    @Override public void onDisable() {
        if (before == null || before.isEmpty()) return;
        Options o = Minecraft.getInstance().options;
        try {
            o.renderDistance().set(Integer.parseInt(before.getProperty("render")));
            o.simulationDistance().set(Integer.parseInt(before.getProperty("simulation")));
            o.particles().set(ParticleStatus.valueOf(before.getProperty("particles")));
            o.cloudStatus().set(CloudStatus.valueOf(before.getProperty("clouds")));
            o.entityShadows().set(Boolean.parseBoolean(before.getProperty("shadows")));
            o.biomeBlendRadius().set(Integer.parseInt(before.getProperty("biome")));
            Files.deleteIfExists(BACKUP);
            o.save();
        } catch (Exception e) {
            DashClient.LOGGER.warn("Cannot restore performance settings", e);
        }
        before = null;
    }
}
