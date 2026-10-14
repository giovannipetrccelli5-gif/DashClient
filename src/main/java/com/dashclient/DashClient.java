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
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

public final class DashClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("DashClient");
    public static final List<Module> MODULES = new ArrayList<>();
    public static KeyMapping MENU_KEY;
    public static KeyMapping ZOOM_KEY;
    public static KeyMapping AUTO_CLICKER_KEY;
    private boolean configLoaded;

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
        // Rebindable in Minecraft Settings > Controls > Key Binds > DashClient.
        AUTO_CLICKER_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.dashclient.auto_clicker", InputConstants.Type.KEYBOARD,
            InputConstants.KEY_K, category));

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
        MODULES.add(new AutoTotemModule());
        MODULES.add(new AutoElytraModule());
        MODULES.add(new ElytraFinderModule());
        WorldPack.register(MODULES);
        StatusPack.register(MODULES);
        MODULES.add(new PerformanceModule("Extreme FPS", true));
        MODULES.add(new AdaptiveFpsModule());
        AdvancedHudPack.register(MODULES);
        CombatVisualPack.register(MODULES);
        ClientAssistPack.register(MODULES);
        MODULES.add(new PerformanceModule("Battery Saver", true, true));
        ExpandedModulePack.register(MODULES);
        DeviceHealthPack.register(MODULES);
        UtilityModulePack.register(MODULES);
        ModuleOrder.load(MODULES);
        ModuleFavorites.load();
        HudLayout.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Minecraft.Options is null during ClientModInitializer startup.
            // Apply saved module states after the first completed client tick,
            // when graphics settings are initialized (Performance Boost and
            // Extended View both access those options in onEnable()).
            if (!configLoaded && client.options != null) {
                Config.load();
                configLoaded = true;
            }
            AdvancedHudPack.sample(client);
            ExpandedModulePack.tick(client);
            HudFrameCache.sample(client);
            while (MENU_KEY.consumeClick()) {
                if (client.gui.screen() == null) client.gui.setScreen(new DashScreen());
            }
            while (AUTO_CLICKER_KEY.consumeClick()) {
                // Never toggle accidentally while typing in search or chat.
                if (client.gui.screen() != null || client.player == null) continue;
                for (Module module : MODULES) {
                    if (!module.name().equals("Auto Clicker")) continue;
                    module.setEnabled(!module.enabled());
                    Config.save();
                    client.gui.hud.setOverlayMessage(Component.literal(
                        "DashClient Auto Clicker: " + (module.enabled() ? "ON" : "OFF")), true);
                    break;
                }
            }
            for (Module module : MODULES) {
                if (module.enabled()) module.onTick(client);
            }
        });

        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("dashclient", "overlay"), (graphics, delta) -> {
                Minecraft client = Minecraft.getInstance();
                if (client.player == null || client.gui.screen() instanceof HudEditorScreen)
                    return;
                if (isEnabled("HUD Gameplay Only") && client.gui.screen() != null) return;

                int sw = client.getWindow().getGuiScaledWidth();
                int sh = client.getWindow().getGuiScaledHeight();
                int gap = isEnabled("HUD Compact") ? 11 : 14;
                boolean right = isEnabled("HUD Right Align");
                boolean rainbow = isEnabled("HUD Rainbow");
                boolean transparent = isEnabled("HUD Transparent");
                int ordinal = 0;

                // Icon HUDs render from current equipment every frame, allowing
                // enchanted glint, durability bars and the totem count to update.
                for (Module module : MODULES) {
                    if (!module.enabled() || !module.customHud()) continue;
                    HudLayout.Point position = HudLayout.position(module, 0, sw, sh,
                        module.hudWidth(client), module.hudHeight(client), gap, right);
                    if (position != null)
                        module.renderHud(client, graphics, position.x(), position.y());
                }

                for (HudFrameCache.Line line : HudFrameCache.lines()) {
                    Module module = line.module();
                    int tw = line.width();
                    String value = line.text();
                    HudLayout.Point pos = HudLayout.position(module, ordinal++, sw, sh,
                        tw, gap, right);
                    if (pos == null) continue;
                    int x = pos.x(), y = pos.y();
                    if (!transparent)
                        graphics.fill(x - 4, y - 2, x + tw + 4, y + 11, 0x880D1421);
                    int color = rainbow ? (java.awt.Color.HSBtoRGB(
                        ((System.currentTimeMillis() / 28L + ordinal * 18L) % 360L) / 360f,
                        0.72f, 1f) | 0xFF000000) : 0xFFFFFFFF;
                    graphics.text(client.font, value, x, y, color, true);
                }
            }
        );
        LOGGER.info("DashClient initialized");
    }
}
