package com.dashclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

/**
 * Phone-friendly import panel. Users copy files into the inbox using an
 * Android file manager, then review and explicitly install in-game.
 */
public final class ClientImportsScreen extends Screen {
    private List<ClientFileInbox.Entry> files = List.of();
    private String message = "Only compatible Fabric mods (.jar) and resource packs (.zip) are accepted.";
    private int page;
    private int panelX, panelY, panelWidth, panelHeight;
    private static final int PER_PAGE = 5;

    public ClientImportsScreen() { super(Component.literal("DashClient File Manager")); }

    @Override protected void init() {
        panelWidth = Math.min(width - 12, 560);
        panelHeight = Math.min(height - 12, 340);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        files = ClientFileInbox.scan();
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();
        int left = panelX + 12;
        int right = panelX + panelWidth - 12;
        int btnW = Math.max(44, Math.min(105, panelWidth / 5));
        int top = panelY + 43;
        page = Math.min(page, Math.max(0, (files.size() - 1) / PER_PAGE));

        addRenderableWidget(Button.builder(Component.literal("Back"), b ->
            minecraft.gui.setScreen(new DashScreen()))
            .bounds(left, panelY + 9, btnW, 21).build());
        addRenderableWidget(Button.builder(Component.literal("Refresh"), b -> {
            files = ClientFileInbox.scan();
            page = 0;
            rebuildButtons();
        }).bounds(right - btnW, panelY + 9, btnW, 21).build());

        int rowHeight = Math.max(25, Math.min(39, (panelHeight - 103) / PER_PAGE));
        for (int i = page * PER_PAGE;
                i < files.size() && i < (page + 1) * PER_PAGE; i++) {
            ClientFileInbox.Entry entry = files.get(i);
            int row = i - page * PER_PAGE;
            int y = top + row * rowHeight;
            String label = entry.kind() == ClientFileInbox.Kind.FABRIC_MOD
                ? "Install Mod" : entry.kind() == ClientFileInbox.Kind.RESOURCE_PACK
                ? "Install Pack" : "Invalid";
            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                ClientFileInbox.Result result = ClientFileInbox.install(entry.name());
                message = result.message();
                files = ClientFileInbox.scan();
                rebuildButtons();
            }).bounds(right - btnW, y, btnW, 21).build()).active =
                entry.kind() != ClientFileInbox.Kind.INVALID;
        }
        int footerY = panelY + panelHeight - 32;
        addRenderableWidget(Button.builder(Component.literal("< Page"), b -> {
            if (page > 0) { page--; rebuildButtons(); }
        }).bounds(left, footerY, btnW, 21).build()).active = page > 0;
        addRenderableWidget(Button.builder(Component.literal("Page >"), b -> {
            if ((page + 1) * PER_PAGE < files.size()) { page++; rebuildButtons(); }
        }).bounds(right - btnW, footerY, btnW, 21).build()).active =
            (page + 1) * PER_PAGE < files.size();
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g,
                                            int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xCC080E1D);
        g.fill(panelX - 1, panelY - 1, panelX + panelWidth + 1,
            panelY + panelHeight + 1, 0xFF3383B1);
        g.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xF7132032);
        g.fill(panelX + 3, panelY + 3, panelX + panelWidth - 3,
            panelY + 32, 0xFF183C53);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.centeredText(font, "DASHCLIENT / FILE IMPORTS", panelX + panelWidth / 2,
            panelY + 14, 0xFF8AE8FF);
        int left = panelX + 12;
        int top = panelY + 43;
        int rowHeight = Math.max(25, Math.min(39, (panelHeight - 103) / PER_PAGE));
        int labelWidth = Math.max(40, panelWidth - Math.max(44, Math.min(105, panelWidth / 5)) - 40);
        for (int i = page * PER_PAGE;
                i < files.size() && i < (page + 1) * PER_PAGE; i++) {
            var entry = files.get(i);
            int y = top + (i - page * PER_PAGE) * rowHeight;
            g.fill(left - 3, y - 3, panelX + panelWidth - 9, y + 22, 0xCC1F324A);
            String name = entry.name();
            while (!name.isEmpty() && font.width(name) > labelWidth)
                name = name.substring(0, name.length() - 1);
            g.text(font, name, left, y + 4,
                entry.kind() == ClientFileInbox.Kind.INVALID ? 0xFFFF9A9A : 0xFFF3FBFF, true);
        }
        if (files.isEmpty())
            g.centeredText(font, "Inbox is empty. Copy files there with a file manager.",
                panelX + panelWidth / 2, panelY + 60, 0xFF9FD3E6);
        String tip = "Inbox: .minecraft/dashclient-imports";
        g.centeredText(font, tip, panelX + panelWidth / 2,
            panelY + panelHeight - 68, 0xFF9FD3E6);
        g.centeredText(font, "Mods require a restart | Resource packs must be enabled",
            panelX + panelWidth / 2, panelY + panelHeight - 55, 0xFF9FD3E6);
        String status = message;
        while (!status.isEmpty() && font.width(status) > panelWidth - 24)
            status = status.substring(0, status.length() - 1);
        g.centeredText(font, status, panelX + panelWidth / 2,
            panelY + panelHeight - 43, 0xFFECD7AC);
    }
}
