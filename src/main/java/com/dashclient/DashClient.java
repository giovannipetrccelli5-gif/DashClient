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

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("dashclient", "controls"));
        MENU_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.dashclient.menu", InputConstants.Type.KEYSYM,
            InputConstants.KEY_RIGHT_SHIFT, category));
        ZOOM_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.dashclient.zoom", InputConstants.Type.KEYSYM,
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
                if (client.player == null || client.options.hideGui) return;
                int y = 6;
                for (Module module : MODULES) {
                    if (!module.enabled()) continue;
                    String value = module.hudText(client);
                    if (value == null || value.isBlank()) continue;
                    graphics.fill(4, y - 2, 12 + client.font.width(value), y + 11, 0x880D1421);
                    graphics.text(client.font, value, 8, y, 0xFFFFFFFF, true);
                    y += 14;
                }
            }
        );
        LOGGER.info("DashClient initialized");
    }
}
