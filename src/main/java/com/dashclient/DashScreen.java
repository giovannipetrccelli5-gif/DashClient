package com.dashclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DashScreen extends Screen {
    public DashScreen() { super(Component.literal("DashClient")); }

    @Override
    protected void init() {
        int colWidth = Math.min(150, (width - 28) / 2);
        int left = width / 2 - colWidth - 5;
        int right = width / 2 + 5;
        for (int i = 0; i < DashClient.MODULES.size(); i++) {
            Module module = DashClient.MODULES.get(i);
            int x = i % 2 == 0 ? left : right;
            int y = 55 + (i / 2) * 28;
            String text = module.name() + ": " + (module.enabled() ? "ON" : "OFF");
            addRenderableWidget(Button.builder(Component.literal(text), button -> {
                module.setEnabled(!module.enabled());
                Config.save();
                rebuildWidgets();
            }).bounds(x, y, colWidth, 22).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Done"), button ->
            minecraft.gui.setScreen(null)
        ).bounds(width / 2 - 60, 55 + ((DashClient.MODULES.size() + 1) / 2) * 28,
            120, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, "DashClient Modules", width / 2, 24, 0xFFFFFFFF);
        graphics.centeredText(font, "Right Shift to open  |  C to zoom (rebindable)",
            width / 2, 39, 0xFFB6C7E9);
    }
}
