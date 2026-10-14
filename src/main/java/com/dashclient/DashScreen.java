package com.dashclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Paginated menu with a single-column layout for small windows. */
public final class DashScreen extends Screen {
    private int page = 0;
    private int pageSize = 8;

    public DashScreen() { super(Component.literal("DashClient")); }

    @Override
    protected void init() {
        int columns = width < 340 ? 1 : 2;
        int top = height < 140 ? 36 : 51;
        int rows = Math.max(1, (height - top - 44) / 26);
        pageSize = rows * columns;
        int pages = Math.max(1, (DashClient.MODULES.size() + pageSize - 1) / pageSize);
        page = Math.max(0, Math.min(page, pages - 1));

        int colWidth = columns == 1 ? Math.max(60, width - 24)
            : Math.min(156, (width - 28) / 2);
        int left = columns == 1 ? (width - colWidth) / 2 : width / 2 - colWidth - 5;
        int right = width / 2 + 5;
        int start = page * pageSize;
        int end = Math.min(DashClient.MODULES.size(), start + pageSize);

        for (int i = start; i < end; i++) {
            Module module = DashClient.MODULES.get(i);
            int slot = i - start;
            int x = (slot % columns == 0) ? left : right;
            int y = top + (slot / columns) * 26;
            String suffix = module.enabled() ? "  ON" : "  OFF";
            String name = module.name();
            while (!name.isEmpty() && font.width(name + suffix) > colWidth - 10)
                name = name.substring(0, name.length() - 1);
            addRenderableWidget(Button.builder(Component.literal(name + suffix), button -> {
                module.setEnabled(!module.enabled());
                Config.save();
                rebuildWidgets();
            }).bounds(x, y, colWidth, 22).build());
        }

        int footer = height - 27;
        int nav = Math.min(65, Math.max(36, (width - 110) / 2 - 12));
        addRenderableWidget(Button.builder(Component.literal("< Prev"), b -> {
            if (page > 0) { page--; rebuildWidgets(); }
        }).bounds(8, footer, nav, 20).build()).active = page > 0;

        addRenderableWidget(Button.builder(Component.literal("Done"), b ->
            minecraft.gui.setScreen(null)
        ).bounds(width / 2 - 40, footer, 80, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Next >"), b -> {
            if (page < pages - 1) { page++; rebuildWidgets(); }
        }).bounds(width - nav - 8, footer, nav, 20).build()).active = page < pages - 1;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int pages = Math.max(1, (DashClient.MODULES.size() + pageSize - 1) / pageSize);
        g.centeredText(font, "DashClient - " + DashClient.MODULES.size() + " Modules",
            width / 2, 8, 0xFFFFFFFF);
        g.centeredText(font, "Page " + (page + 1) + "/" + pages,
            width / 2, 23, 0xFFB6C7E9);
    }
}
