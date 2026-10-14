package com.dashclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Touch-first HUD position editor. Supports mouse dragging, select-then-tap,
 * large selection/nudge controls and up-only touch gestures from Android launchers.
 */
public final class HudEditorScreen extends Screen {
    private record Preview(Module module, String text, int x, int y, int width) {}
    private final List<Preview> previews = new ArrayList<>();
    private Module dragging;
    private Module selected;
    private boolean dragMoved;
    private int dragOffsetX, dragOffsetY;
    private int dragStartX, dragStartY;

    public HudEditorScreen() {
        super(Component.literal("DashClient HUD Editor"));
    }

    @Override protected void init() {
        int pad = 7;
        int gap = 4;
        int btnW = Math.max(44, (width - pad * 2 - gap * 3) / 4);
        int buttonY = height - 69;
        int nudgeY = height - 43;

        addRenderableWidget(Button.builder(Component.literal("< Prev HUD"), b -> {
            selectRelative(-1);
        }).bounds(pad, buttonY, btnW, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Next HUD >"), b -> {
            selectRelative(1);
        }).bounds(pad + btnW + gap, buttonY, btnW, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> {
            HudLayout.reset();
            selected = null;
            dragging = null;
        }).bounds(pad + (btnW + gap) * 2, buttonY, btnW, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> {
            HudLayout.save();
            minecraft.gui.setScreen(new DashScreen());
        }).bounds(pad + (btnW + gap) * 3, buttonY, btnW, 22).build());

        addRenderableWidget(Button.builder(Component.literal("< Left"), b -> {
            nudge(-12, 0);
        }).bounds(pad, nudgeY, btnW, 22).build());
        addRenderableWidget(Button.builder(Component.literal("^ Up"), b -> {
            nudge(0, -12);
        }).bounds(pad + btnW + gap, nudgeY, btnW, 22).build());
        addRenderableWidget(Button.builder(Component.literal("v Down"), b -> {
            nudge(0, 12);
        }).bounds(pad + (btnW + gap) * 2, nudgeY, btnW, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Right >"), b -> {
            nudge(12, 0);
        }).bounds(pad + (btnW + gap) * 3, nudgeY, btnW, 22).build());
    }

    private void updatePreviews() {
        previews.clear();
        Minecraft client = Minecraft.getInstance();
        int ordinal = 0;
        int lineHeight = DashClient.isEnabled("HUD Compact") ? 11 : 14;

        for (Module module : DashClient.MODULES) {
            if (!module.enabled()) continue;
            String value = module.hudText(client);
            if (value == null || value.isBlank()) {
                // Display real readout modules even when a world is not open.
                try {
                    if (module.getClass().getMethod("hudText", Minecraft.class)
                            .getDeclaringClass() == Module.class) continue;
                } catch (NoSuchMethodException e) { continue; }
                value = module.name() + " (preview)";
            }
            value = ModuleSymbols.symbol(module) + " " + value;
            int maxWidth = Math.min(190, Math.max(90, width - 12)) - 12;
            while (!value.isEmpty() && font.width(value) > maxWidth)
                value = value.substring(0, value.length() - 1);
            if (value.isEmpty()) continue;

            int textWidth = font.width(value);
            HudLayout.Point position = HudLayout.position(
                module, ordinal++, width, height, textWidth, lineHeight,
                DashClient.isEnabled("HUD Right Align"));
            if (position != null) {
                previews.add(new Preview(module, value,
                    position.x(), position.y(), textWidth));
            }
        }
    }

    private Preview selectedPreview() {
        for (Preview preview : previews)
            if (preview.module == selected) return preview;
        return null;
    }

    private void selectRelative(int offset) {
        updatePreviews();
        if (previews.isEmpty()) return;
        int current = -1;
        for (int i = 0; i < previews.size(); i++) {
            if (previews.get(i).module == selected) {
                current = i;
                break;
            }
        }
        int next = current == -1 ? (offset > 0 ? 0 : previews.size() - 1)
            : Math.floorMod(current + offset, previews.size());
        selected = previews.get(next).module;
        dragging = null;
    }

    private void placeSelected(int x, int y) {
        if (selected == null) return;
        updatePreviews();
        Preview preview = selectedPreview();
        if (preview == null) return;
        int safeY = Math.min(y, Math.max(30, height - 92));
        HudLayout.move(selected, x, safeY, width, height, preview.width);
        HudLayout.save();
        updatePreviews();
    }

    private void nudge(int dx, int dy) {
        if (selected == null) selectRelative(1);
        updatePreviews();
        Preview preview = selectedPreview();
        if (preview != null)
            placeSelected(preview.x + dx, preview.y + dy);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Always handle the bottom action buttons before trying to select HUD text.
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() != 0) return false;

        updatePreviews();
        for (int i = previews.size() - 1; i >= 0; i--) {
            Preview p = previews.get(i);
            // Include the big drag handle, not only the tiny font pixels.
            if (event.x() >= p.x - 10 && event.x() <= p.x + p.width + 36
                    && event.y() >= p.y - 6 && event.y() <= p.y + 15) {
                selected = p.module;
                dragging = p.module;
                dragMoved = false;
                dragStartX = (int) event.x();
                dragStartY = (int) event.y();
                dragOffsetX = (int) event.x() - p.x;
                dragOffsetY = (int) event.y() - p.y;
                return true;
            }
        }

        // Touch-friendly: tap a HUD element once, then tap where it should go.
        if (selected != null && event.y() < height - 76) {
            placeSelected((int) event.x(), (int) event.y());
            dragging = null;
            return true;
        }
        return false;
    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging != null && event.button() == 0) {
            int travel = Math.abs((int)event.x() - dragStartX)
                + Math.abs((int)event.y() - dragStartY);
            if (travel > 4 || dragMoved) {
                dragMoved = true;
                placeSelected((int)event.x() - dragOffsetX,
                    (int)event.y() - dragOffsetY);
            }
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            // PojavLauncher and some Android touch drivers deliver down/up but
            // no intermediate mouseDragged callbacks. Finish based on release.
            int travel = Math.abs((int)event.x() - dragStartX)
                + Math.abs((int)event.y() - dragStartY);
            if (travel > 4 || dragMoved)
                placeSelected((int)event.x() - dragOffsetX,
                    (int)event.y() - dragOffsetY);
            dragging = null;
            dragMoved = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override public void onClose() {
        dragging = null;
        HudLayout.save();
        minecraft.gui.setScreen(new DashScreen());
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g,
                                             int mx, int my, float partialTick) {
        g.fill(0, 0, width, height, 0x77060A16);
        updatePreviews();

        for (Preview p : previews) {
            boolean highlighted = p.module == selected;
            int border = highlighted ? 0xFF66DFFF : 0xAA4978A0;
            g.fill(p.x - 6, p.y - 4, p.x + p.width + 6, p.y + 13, border);
            g.fill(p.x - 4, p.y - 2, p.x + p.width + 4, p.y + 11, 0xDD101D33);
            g.text(font, p.text, p.x, p.y, 0xFFFFFFFF, true);

            // Enlarged handle lets players start dragging away from
            // Android's edge-navigation gesture region.
            int handleX = p.x + p.width + 9;
            g.fill(handleX, p.y - 3, handleX + 23, p.y + 12,
                highlighted ? 0xFF237EAE : 0xCC24435D);
            g.text(font, "MOVE", handleX + 1, p.y + 1, 0xFFFFFFFF, false);
        }
        super.extractRenderState(g, mx, my, partialTick);
        g.centeredText(font, "EDIT HUD  -  Select and tap a spot, drag MOVE, or use arrows",
            width / 2, 10, 0xFF77E2FF);
        String name = selected == null ? "Select a module with a tap or Next HUD" :
            "Selected: " + selected.name() + "  |  Arrows move 12 pixels";
        g.centeredText(font, name, width / 2, height - 87, 0xFFFFFFFF);
        g.centeredText(font, "Saved automatically", width / 2,
            height - 13, 0xFF77E2FF);
    }
}
