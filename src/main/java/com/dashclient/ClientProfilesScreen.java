package com.dashclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Original DashClient profile selector, deliberately usable on touch screens. */
public final class ClientProfilesScreen extends Screen {
    private int x, y, w, h;
    private String status = "Choose a module profile. Your current switches are saved.";
    public ClientProfilesScreen() { super(Component.literal("DashClient Profiles")); }

    @Override protected void init() {
        w = Math.min(width - 12, 480);
        h = Math.min(height - 12, 294);
        x = (width - w) / 2;
        y = (height - h) / 2;
        int pad = 12, gap = 6;
        int col = (w - pad * 2 - gap) / 2;
        int rowH = Math.max(20, Math.min(28, (h - 132) / 3));
        for (int i = 0; i < ClientProfiles.SLOTS.length; i++) {
            String slot = ClientProfiles.SLOTS[i];
            int bx = x + pad + (i % 2) * (col + gap);
            int by = y + 63 + (i / 2) * (rowH + gap);
            addRenderableWidget(Button.builder(Component.literal(
                (slot.equals(ClientProfiles.active()) ? "[ACTIVE] " : "") + slot
            ), b -> {
                ClientProfiles.switchTo(slot);
                status = "Now using " + slot + ". Module switches saved.";
                minecraft.gui.setScreen(new ClientProfilesScreen());
            }).bounds(bx, by, col, rowH).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Save current profile"), b -> {
            ClientProfiles.saveActive();
            status = "Saved " + ClientProfiles.active() + " module switches.";
        }).bounds(x + pad, y + h - 63, w - pad * 2, 24).build());
        addRenderableWidget(Button.builder(Component.literal("Back to modules"), b ->
            minecraft.gui.setScreen(new DashScreen())
        ).bounds(x + pad, y + h - 34, w - pad * 2, 24).build());
    }

    @Override public void onClose() {
        minecraft.gui.setScreen(new DashScreen());
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float tick) {
        g.fill(0, 0, width, height, 0xBE050509);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF4567A0);
        g.fill(x, y, x + w, y + h, 0xF012141B);
        g.fill(x + 2, y + 2, x + w - 2, y + 34, 0xFF1A2031);
        g.fill(x + 2, y + 34, x + w - 2, y + 36, 0xFF6B8AF2);
        super.extractRenderState(g, mx, my, tick);
        g.centeredText(font, "DASHCLIENT / PROFILES", x + w / 2, y + 15, 0xFFA7BFFF);
        g.centeredText(font, "Separate module setups for each way you play", 
            x + w / 2, y + 47, 0xFFD1D7E6);
        if (h > 245) {
            String desc = "Change a module anytime; switch back to save that profile.";
            while (!desc.isEmpty() && font.width(desc) > w - 20) 
                desc = desc.substring(0, desc.length() - 1);
            g.centeredText(font, desc, x + w / 2, y + h - 86, 0xFF9BAAC5);
        }
        String line = status;
        while (!line.isEmpty() && font.width(line) > w - 20)
            line = line.substring(0, line.length() - 1);
        if (h > 245) g.centeredText(font, line, x + w / 2, y + h - 101, 0xFF90DCC2);
    }
}
