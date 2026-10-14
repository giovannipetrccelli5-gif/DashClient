package com.dashclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/** Touch/mouse draggable module HUD editor. */
public final class HudEditorScreen extends Screen {
    private record Preview(Module module, String text, int x, int y, int width) {}
    private final List<Preview> previews = new ArrayList<>();
    private Module dragging;
    private Module selected;
    private boolean dragMoved;
    private int dragOffsetX, dragOffsetY;
    private int dragStartX, dragStartY;

    public HudEditorScreen() { super(Component.literal("DashClient HUD Editor")); }

    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Back to Modules"), b ->
            minecraft.gui.setScreen(new DashScreen())
        ).bounds(10, 8, Math.min(135,width / 3), 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset Positions"), b -> {
            HudLayout.reset();
            selected = null; dragging = null;
        }).bounds(width - Math.min(135,width / 3) - 10, 8,
            Math.min(135,width / 3), 20).build());
    }

    private void updatePreviews() {
        previews.clear();
        Minecraft c = Minecraft.getInstance();
        int ordinal = 0;
        int gap = DashClient.isEnabled("HUD Compact") ? 11 : 14;
        for (Module module : DashClient.MODULES) {
            if (!module.enabled()) continue;
            String text = module.hudText(c);
            // Show a placeholder even while outside a world, so touch users can
            // arrange HUD elements from the title screen.
            if (text == null || text.isBlank()) {
                try {
                    if (module.getClass().getMethod("hudText", Minecraft.class)
                            .getDeclaringClass() == Module.class) continue;
                } catch (NoSuchMethodException ex) { continue; }
                text = module.name() + " (preview)";
            }
            text = ModuleSymbols.symbol(module) + " " + text;
            int maxWidth = Math.min(190, Math.max(90, width - 12)) - 12;
            while (!text.isEmpty() && font.width(text) > maxWidth)
                text = text.substring(0, text.length() - 1);
            if (text.isEmpty()) continue;
            int tw = font.width(text);
            HudLayout.Point pos = HudLayout.position(module, ordinal++, width, height,
                tw, gap, DashClient.isEnabled("HUD Right Align"));
            if (pos != null) previews.add(new Preview(module,text,pos.x(),pos.y(),tw));
        }
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Give the top toolbar its clicks first.
        if (event.y() < 33 && super.mouseClicked(event,doubleClick)) return true;
        if (event.button() == 0) {
            updatePreviews();
            for (int i = previews.size() - 1; i >= 0; i--) {
                Preview p = previews.get(i);
                if (event.x() >= p.x - 5 && event.x() <= p.x + p.width + 5
                        && event.y() >= p.y - 4 && event.y() <= p.y + 14) {
                    selected = p.module;
                    dragging = p.module;
                    dragMoved = false;
                    dragStartX = (int) event.x();
                    dragStartY = (int) event.y();
                    dragOffsetX = (int)event.x() - p.x;
                    dragOffsetY = (int)event.y() - p.y;
                    return true;
                }
            }
        }
        // Touch fallback: select a HUD readout, then tap its new location.
        if (event.button() == 0 && selected != null && event.y() >= 34) {
            int tw = font.width(selected.name());
            for (Preview p : previews) if (p.module == selected) { tw = p.width; break; }
            HudLayout.move(selected, (int)event.x(), (int)event.y(), width, height, tw);
            HudLayout.save();
            selected = null;
            return true;
        }
        return super.mouseClicked(event,doubleClick);
    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging != null && event.button() == 0) {
            if (Math.abs(event.x() - dragStartX) + Math.abs(event.y() - dragStartY) <= 6
                    && !dragMoved) return true;
            dragMoved = true;
            int tw = font.width(dragging.name());
            for (Preview p : previews) if (p.module == dragging) { tw = p.width; break; }
            HudLayout.move(dragging,(int)event.x()-dragOffsetX,(int)event.y()-dragOffsetY,
                width,height,tw);
            return true;
        }
        return super.mouseDragged(event,dx,dy);
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            dragging = null;
            if (dragMoved) {
                selected = null;
                HudLayout.save();
            }
            dragMoved = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override public void onClose() {
        dragging = null;
        selected = null;
        HudLayout.save();
        minecraft.gui.setScreen(new DashScreen());
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g,
                                             int mx, int my, float partialTick) {
        // Draw translucent frames and texts as a live HUD preview.
        g.fill(0, 0, width, height, 0x77060A16);
        updatePreviews();
        for (Preview p : previews) {
            boolean highlighted = p.module == selected || p.module == dragging;
            int border = highlighted ? 0xFF66DFFF : 0xAA4978A0;
            g.fill(p.x - 6, p.y - 4, p.x + p.width + 6, p.y + 13, border);
            g.fill(p.x - 4, p.y - 2, p.x + p.width + 4, p.y + 11, 0xDD101D33);
            g.text(font, p.text, p.x, p.y, 0xFFFFFFFF, true);
        }
        super.extractRenderState(g,mx,my,partialTick);
        g.centeredText(font, "DRAG OR TAP MODULE, THEN TAP DESTINATION",
            width / 2, height - 15, 0xFF77E2FF);
        g.centeredText(font, "EDIT HUD", width / 2, 13, 0xFFFFFFFF);
    }
}
