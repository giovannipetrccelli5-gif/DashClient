package com.dashclient;

import com.dashclient.modules.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

public final class DashClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("DashClient");
    public static final List<Module> MODULES = new ArrayList<>();
    public static KeyMapping MENU_KEY;
    public static KeyMapping ZOOM_KEY;

    public static boolean isEnabled(String name) {
        for (Module m : MODULES) {
            if (m.name().equals(name)) return m.enabled();
        }
        return false;
    }

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("dashclient", "controls"));
        MENU_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.dashclient.menu", InputConstants.Type.KEYBOARD,
            InputConstants.KEY_RSHIFT, category));
        ZOOM_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.dashclient.zoom", InputConstants.Type.KEYBOARD,
            InputConstants.KEY_C, category));

        MODULES.add(new HudModules.Fps());
        MODULES.add(new HudModules.Coordinates());
        MODULES.add(new HudModules.Direction());
        MODULES.add(new HudModules.Clock());
        MODULES.add(new FullbrightModule());
        MODULES.add(new SprintModule());
        MODULES.add(new HudModules.Cps());
        MODULES.add(new HudModules.ArmorItems());
        MODULES.add(new ZoomModule());
        MODULES.add(new PerformanceModule());
        // Optional HUD modules inspired by popular client information overlays.
        MODULES.add(new ExtraHudModules.Ping());
        MODULES.add(new ExtraHudModules.Memory());
        MODULES.add(new ExtraHudModules.Playtime());
        MODULES.add(new ExtraHudModules.Keystrokes());
        MODULES.add(new ExtraHudModules.Effects());
        MODULES.add(new ExtraHudModules.Biome());
        MODULES.add(new ExtraHudModules.Chunk());
        MODULES.add(new ExtraHudModules.Speed());
        MODULES.add(new ExtraHudModules.Health());
        MODULES.add(new ExtraHudModules.Hunger());
        MODULES.add(new ExtraHudModules.Totems());
        MODULES.add(new ExtraHudModules.ItemCount());
        MODULES.add(new ExtraHudModules.ItemDurability());
        MODULES.add(new ExtraHudModules.WorldTime());
        MODULES.add(new ExtraHudModules.ServerAddress());
        MODULES.add(new ExtraHudModules.BlockInfo());
        // HUD, PvP information, navigation and visual customization.
        InventoryPack.register(MODULES);
        WorldPack.register(MODULES);
        StatusPack.register(MODULES);
        Config.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (MENU_KEY.consumeClick()) {
                if (client.gui.screen() == null) client.gui.setScreen(new DashScreen());
            }
            for (Module module : MODULES) {
                if (module.enabled()) module.onTick(client);
            }
        });

        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("dashclient", "overlay"), (graphics, delta) -> {
                Minecraft client = Minecraft.getInstance();
                if (client.player == null) return;
                if (isEnabled("HUD Gameplay Only") && client.gui.screen() != null) return;
                int screenWidth = client.getWindow().getGuiScaledWidth();
                int screenHeight = client.getWindow().getGuiScaledHeight();
                int width = Math.min(190, Math.max(110, screenWidth - 12));
                int columns = Math.max(1, screenWidth / (width + 4));
                int gap = isEnabled("HUD Compact") ? 11 : 14;
                boolean right = isEnabled("HUD Right Align");
                boolean transparent = isEnabled("HUD Transparent");
                boolean rainbow = isEnabled("HUD Rainbow");
                int row = 0, column = 0;
                for (Module module : MODULES) {
                    if (column >= columns) break;
                    if (!module.enabled()) continue;
                    String value = module.hudText(client);
                    if (value == null || value.isBlank()) continue;
                    if (row * gap + 18 > screenHeight) {
                        row = 0;
                        column++;
                    }
                    if (column >= columns) break; // Never draw past the visible HUD area.
                    int available = width - 12;
                    while (!value.isEmpty() && client.font.width(value) > available)
                        value = value.substring(0, value.length() - 1);
                    if (value.isEmpty()) continue;
                    int x = right ? screenWidth - 8 - client.font.width(value) - column * (width + 4)
                        : 8 + column * (width + 4);
                    int y = 6 + row * gap;
                    if (!transparent)
                        graphics.fill(x - 4, y - 2, x + client.font.width(value) + 4,
                            y + 11, 0x880D1421);
                    int color = rainbow ? java.awt.Color.HSBtoRGB(
                        ((System.currentTimeMillis() / 28L + row * 18L) % 360L) / 360f,
                        0.72f, 1f) | 0xFF000000 : 0xFFFFFFFF;
                    graphics.text(client.font, value, x, y, color, true);
                    row++;
                }
            }
        );
        LOGGER.info("DashClient initialized");
    }
}
