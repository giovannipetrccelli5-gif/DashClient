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
 * Touch-first full-screen editor. The two rows of buttons move to the top
 * when a HUD readout enters their bottom region. No editor toolbar exclusion
 * is applied to saved element coordinates.
 */
public final class HudEditorScreen extends Screen {
    private record Preview(Module module, String text, int x, int y,
                           int width, int height, boolean custom) {}
    private final List<Preview> previews = new ArrayList<>();
    private final List<Button> firstRow = new ArrayList<>();
    private final List<Button> secondRow = new ArrayList<>();
    private Module dragging;
    private Module selected;
    private boolean dragMoved;
    private boolean toolbarTop;
    private long nextPreviewRefreshNanos;
    private int dragOffsetX, dragOffsetY, dragStartX, dragStartY;

    public HudEditorScreen() { super(Component.literal("DashClient HUD Editor")); }

    private void firstButton(Button button) {
        firstRow.add(button);
        addRenderableWidget(button);
    }
    private void secondButton(Button button) {
        secondRow.add(button);
        addRenderableWidget(button);
    }

    @Override protected void init() {
        firstRow.clear();
        secondRow.clear();
        int pad = 7;
        int gap = 4;
        int btnW = Math.max(36, (width - pad * 2 - gap * 3) / 4);
        int row1 = height - 69;
        int row2 = height - 43;

        firstButton(Button.builder(Component.literal("< Prev HUD"), b ->
            selectRelative(-1)).bounds(pad, row1, btnW, 22).build());
        firstButton(Button.builder(Component.literal("Next HUD >"), b ->
            selectRelative(1)).bounds(pad + (btnW + gap), row1, btnW, 22).build());
        firstButton(Button.builder(Component.literal("Reset"), b -> {
            HudLayout.reset();
            dragging = selected = null;
            updatePreviews(true);
        }).bounds(pad + 2 * (btnW + gap), row1, btnW, 22).build());
        firstButton(Button.builder(Component.literal("Back"), b -> {
            HudLayout.save();
            minecraft.gui.setScreen(new DashScreen());
        }).bounds(pad + 3 * (btnW + gap), row1, btnW, 22).build());

        secondButton(Button.builder(Component.literal("< Left"), b ->
            nudge(-12, 0)).bounds(pad, row2, btnW, 22).build());
        secondButton(Button.builder(Component.literal("^ Up"), b ->
            nudge(0, -12)).bounds(pad + (btnW + gap), row2, btnW, 22).build());
        secondButton(Button.builder(Component.literal("v Down"), b ->
            nudge(0, 12)).bounds(pad + 2 * (btnW + gap), row2, btnW, 22).build());
        secondButton(Button.builder(Component.literal("Right >"), b ->
            nudge(12, 0)).bounds(pad + 3 * (btnW + gap), row2, btnW, 22).build());
        updatePreviews();
    }

    private void positionButtons(boolean top) {
        if (toolbarTop == top) return;
        toolbarTop = top;
        int y1 = top ? 6 : height - 69;
        int y2 = top ? 31 : height - 43;
        for (Button button : firstRow) button.setY(y1);
        for (Button button : secondRow) button.setY(y2);
    }

    private void updatePreviews() { updatePreviews(false); }

    private void updatePreviews(boolean immediate) {
        // Normal rendering may run 60+ times/sec on mobile. Most readouts
        // include string formatting and some query nearby entities. Only
        // resample previews 4x/sec or immediately after a layout change.
        long now = System.nanoTime();
        if (!immediate && now < nextPreviewRefreshNanos) return;
        nextPreviewRefreshNanos = now + 250_000_000L;
        previews.clear();
        Minecraft client = Minecraft.getInstance();
        int ordinal = 0;
        int lineHeight = DashClient.isEnabled("HUD Compact") ? 11 : 14;

        for (Module module : DashClient.MODULES) {
            if (!module.enabled()) continue;
            boolean custom = module.customHud();
            String text;
            int elementWidth;
            int elementHeight;
            if (custom) {
                text = "";
                elementWidth = module.hudWidth(client);
                elementHeight = module.hudHeight(client);
            } else {
                text = module.hudText(client);
                if (text == null || text.isBlank()) {
                    // Preview world-dependent readouts while on the title screen.
                    try {
                        if (module.getClass().getMethod("hudText", Minecraft.class)
                                .getDeclaringClass() == Module.class) continue;
                    } catch (NoSuchMethodException e) { continue; }
                    text = module.name() + " (preview)";
                }
                text = ModuleSymbols.symbol(module) + " " + text;
                int maxWidth = Math.min(190, Math.max(90, width - 12)) - 12;
                while (!text.isEmpty() && font.width(text) > maxWidth)
                    text = text.substring(0, text.length() - 1);
                if (text.isEmpty()) continue;
                elementWidth = font.width(text);
                elementHeight = 12;
            }
            if (elementWidth <= 0 || elementHeight <= 0) continue;
            HudLayout.Point position = HudLayout.position(
                module, ordinal++, width, height, elementWidth, elementHeight,
                lineHeight, DashClient.isEnabled("HUD Right Align"));
            if (position == null) continue;
            previews.add(new Preview(module, text, position.x(), position.y(),
                elementWidth, elementHeight, custom));
        }

        // If any HUD enters the lower controls, lift those controls to the top.
        // Dragging and tap-placement are NEVER restricted by the toolbar's area.
        boolean bottomOccupied = false;
        for (Preview preview : previews) {
            if (preview.y + preview.height >= height - 76) {
                bottomOccupied = true;
                break;
            }
        }
        positionButtons(bottomOccupied);
    }

    private Preview selectedPreview() {
        for (Preview preview : previews)
            if (preview.module == selected) return preview;
        return null;
    }

    private void selectRelative(int direction) {
        updatePreviews();
        if (previews.isEmpty()) return;
        int current = -1;
        for (int i = 0; i < previews.size(); i++) {
            if (previews.get(i).module == selected) { current = i; break; }
        }
        int next = current < 0 ? (direction > 0 ? 0 : previews.size() - 1)
            : Math.floorMod(current + direction, previews.size());
        selected = previews.get(next).module;
        dragging = null;
    }

    private void placeSelected(int x, int y, boolean save) {
        if (selected == null) return;
        updatePreviews();
        Preview preview = selectedPreview();
        if (preview == null) return;
        HudLayout.move(selected, x, y, width, height, preview.width, preview.height);
        if (save) HudLayout.save();
        updatePreviews(true);
    }

    private void nudge(int dx, int dy) {
        if (selected == null) selectRelative(1);
        updatePreviews();
        Preview p = selectedPreview();
        if (p != null) placeSelected(p.x + dx, p.y + dy, true);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Buttons are still accessible when the HUD is under their area:
        // Prev/Next can select any obscured HUD for repositioning.
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() != 0) return false;
        updatePreviews();
        for (int i = previews.size() - 1; i >= 0; i--) {
            Preview p = previews.get(i);
            if (event.x() >= p.x - 8 && event.x() <= p.x + p.width + 34
                    && event.y() >= p.y - 5 && event.y() <= p.y + p.height + 5) {
                selected = dragging = p.module;
                dragMoved = false;
                dragStartX = (int) event.x();
                dragStartY = (int) event.y();
                dragOffsetX = (int) event.x() - p.x;
                dragOffsetY = (int) event.y() - p.y;
                return true;
            }
        }
        // Pojav touch fallback: select a HUD, then tap ANY screen coordinate.
        if (selected != null) {
            placeSelected((int) event.x(), (int) event.y(), true);
            dragging = null;
            return true;
        }
        return false;
    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging != null && event.button() == 0) {
            int travel = Math.abs((int) event.x() - dragStartX)
                + Math.abs((int) event.y() - dragStartY);
            if (travel > 4 || dragMoved) {
                dragMoved = true;
                placeSelected((int) event.x() - dragOffsetX,
                    (int) event.y() - dragOffsetY, false);
            }
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            int travel = Math.abs((int) event.x() - dragStartX)
                + Math.abs((int) event.y() - dragStartY);
            if (travel > 4 || dragMoved)
                placeSelected((int) event.x() - dragOffsetX,
                    (int) event.y() - dragOffsetY, true);
            HudLayout.save();
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

    @Override public void extractRenderState(GuiGraphicsExtractor graphics,
                                             int mx, int my, float partialTick) {
        graphics.fill(0, 0, width, height, 0x77060A16);
        updatePreviews();
        Minecraft client = Minecraft.getInstance();

        for (Preview p : previews) {
            boolean highlighted = p.module == selected;
            int border = highlighted ? 0xFF66DFFF : 0xAA4978A0;
            graphics.fill(p.x - 6, p.y - 3, p.x + p.width + 6,
                p.y + p.height + 3, border);
            if (p.custom) {
                p.module.renderHud(client, graphics, p.x, p.y);
            } else {
                graphics.fill(p.x - 4, p.y - 2, p.x + p.width + 4,
                    p.y + p.height + 1, 0xDD101D33);
                graphics.text(font, p.text, p.x, p.y, 0xFFFFFFFF, true);
            }

            int handleX = Math.min(width - 27, p.x + p.width + 6);
            int handleY = Math.min(height - 14, p.y);
            graphics.fill(handleX, handleY, handleX + 25, handleY + 13,
                highlighted ? 0xFF237EAE : 0xCC24435D);
            graphics.text(font, "MOVE", handleX + 1, handleY + 2,
                0xFFFFFFFF, false);
        }

        super.extractRenderState(graphics, mx, my, partialTick);
        int helperY = toolbarTop ? height - 13 : 10;
        graphics.centeredText(font, "DRAG OR TAP MODULE, THEN TAP ANYWHERE",
            width / 2, helperY, 0xFF77E2FF);
        String current = selected == null ? "Tap a module or press Next HUD" :
            "Selected: " + selected.name() + " (arrow buttons move 12px)";
        int selectedY = toolbarTop ? height - 26 : Math.max(30, height - 88);
        graphics.centeredText(font, current, width / 2, selectedY, 0xFFFFFFFF);
    }
}
