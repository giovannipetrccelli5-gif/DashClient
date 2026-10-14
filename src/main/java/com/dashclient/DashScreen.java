package com.dashclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// Paginated menu: fits on phone/PojavLauncher screens without clipping buttons.
public final class DashScreen extends Screen {
    private int page = 0;
    private int pageSize = 8;

    public DashScreen() { super(Component.literal("DashClient")); }

    @Override
    protected void init() {
        int columns = 2;
        int rows = Math.max(2, (height - 112) / 26);
        pageSize = Math.max(4, rows * columns);
        int pages = Math.max(1, (DashClient.MODULES.size() + pageSize - 1) / pageSize);
        page = Math.max(0, Math.min(page, pages - 1));

        int colWidth = Math.max(62, Math.min(156, (width - 28) / 2));
        int left = width / 2 - colWidth - 5;
        int right = width / 2 + 5;
        int start = page * pageSize;
        int end = Math.min(DashClient.MODULES.size(), start + pageSize);

        for (int i = start; i < end; i++) {
            Module module = DashClient.MODULES.get(i);
            int slot = i - start;
            int x = slot % 2 == 0 ? left : right;
            int y = 51 + (slot / 2) * 26;
            String text = module.name() + (module.enabled() ? " : ON" : " : OFF");
            addRenderableWidget(Button.builder(Component.literal(text), button -> {
                module.setEnabled(!module.enabled());
                Config.save();
                rebuildWidgets();
            }).bounds(x, y, colWidth, 22).build());
        }

        int footer = height - 29;
        int nav = Math.min(75, Math.max(43, (width - 138) / 2));
        addRenderableWidget(Button.builder(Component.literal("< Prev"), b -> {
            if (page > 0) { page--; rebuildWidgets(); }
        }).bounds(left, footer, nav, 20).build()).active = page > 0;

        addRenderableWidget(Button.builder(Component.literal("Done"), b ->
            minecraft.gui.setScreen(null)
        ).bounds(width / 2 - 45, footer, 90, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Next >"), b -> {
            if (page < pages - 1) { page++; rebuildWidgets(); }
        }).bounds(right + colWidth - nav, footer, nav, 20).build()).active = page < pages - 1;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int pages = Math.max(1, (DashClient.MODULES.size() + pageSize - 1) / pageSize);
        g.centeredText(font, "DashClient - " + DashClient.MODULES.size() + " Modules", width / 2, 13, 0xFFFFFFFF);
        g.centeredText(font, "Page " + (page + 1) + "/" + pages + "  |  Right Shift: menu",
            width / 2, 30, 0xFFB6C7E9);
    }
}
