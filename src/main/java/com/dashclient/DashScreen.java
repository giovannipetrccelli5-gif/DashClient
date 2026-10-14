package com.dashclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Original dark dashboard inspired by modern client module managers.
 * Responsive category navigation, searchable module cards and HUD editor.
 */
public final class DashScreen extends Screen {
    private static final String[] TABS = {
        "All", "FPS", "Combat", "Visuals", "HUD", "Survival", "World", "Movement", "Extras"
    };
    private int selectedTab = 0;
    private int page = 0;
    private int pages = 1;
    private int enabledCount;
    private String query = "";
    private int panelX, panelY, panelW, panelH;
    private int contentX, contentW, top;
    private int columns, perPage;
    private EditBox searchBox;
    private final List<Button> moduleButtons = new ArrayList<>();
    private Button previousPageButton, nextPageButton, arrangeButton;
    private boolean searchRefreshPending;
    private boolean arrangeMode;
    private boolean favoritesOnly;
    private boolean enabledOnly;
    private Button favoritesFilterButton, enabledFilterButton;
    private Module selectedModule;
    private int dragStartX, dragStartY;
    private boolean dragMoved;
    private record Tile(Module module, int x, int y, int width, int height) {}
    private final List<Tile> tiles = new ArrayList<>();

    public DashScreen() {
        super(Component.literal("DashClient Modules"));
    }

    private boolean isFps(Module m) {
        return switch (m.name()) {
            case "Performance Boost", "Extreme FPS", "Adaptive FPS",
                "FPS Display", "FPS Average", "FPS Minimum", "FPS Maximum",
                "Frame Interval", "Low FPS Alert", "Render Distance",
                "Simulation Distance", "Particle Setting", "Cloud Setting",
                "Entity Shadows", "Biome Blend", "Field of View",
                "GUI Scale", "HUD Resolution", "Memory" -> true;
            default -> false;
        };
    }

    private String category(Module m, int index) {
        if (m.name().equals("Auto Clicker") || m.name().equals("Kill Aura")
            || m.name().equals("TriggerBot")) return "Combat";
        if (m.name().equals("Auto Sneak") || m.name().equals("Auto Use"))
            return "Movement";
        if (m.name().startsWith("JVM ") || m.name().startsWith("FPS Drop")
                || m.name().equals("Active Optimization Profile")
                || m.name().equals("HUD Update Period")
                || m.name().equals("Network Latency Warning")) return "FPS";
        if (m.name().contains("Durability Warning") || m.name().equals("Totem Backup Warning")
                || m.name().equals("Low Air Warning")) return "Survival";
        if (m.name().equals("Battery Saver") || m.name().startsWith("HUD Eco") ||
            m.name().startsWith("HUD Fast") || m.name().startsWith("HUD Minimal"))
            return "FPS";
        if (m.name().startsWith("Count:")) return "Survival";
        if (m.name().startsWith("Diagnostic:")) return "FPS";
        if (m.name().startsWith("Player:") || m.name().startsWith("Movement:"))
            return "HUD";
        if (m.name().startsWith("World:")) return "World";
        if (m.name().equals("Auto Totem")) return "Survival";
        if (m.name().equals("Auto Elytra")) return "Movement";
        if (m.name().equals("Elytra Finder")) return "World";
        if (isFps(m)) return "FPS";
        if (index >= 124) {
            String name = m.name();
            if (name.equals("Auto Jump") || name.equals("Auto Walk")) return "Movement";
            if (name.contains("ESP") || name.contains("Radar") ||
                name.contains("Compass") || name.startsWith("Nearest") ||
                name.contains("Proximity")) return "Visuals";
            return "Combat";
        }
        if (index < 26) return "HUD";
        if (index < 43) return "Survival";
        if (index < 61) return "World";
        if (index < 78) return "Movement";
        return "Extras";
    }

    private List<Module> filtered() {
        List<Module> all = new ArrayList<>();
        for (int index = 0; index < DashClient.MODULES.size(); index++) {
            Module m = DashClient.MODULES.get(index);
            if (selectedTab != 0 && !TABS[selectedTab].equals(category(m, index))) continue;
            if (favoritesOnly && !ModuleFavorites.contains(m)) continue;
            if (enabledOnly && !m.enabled()) continue;
            if (!m.name().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))
                continue;
            all.add(m);
        }
        all.sort(java.util.Comparator.comparingInt(ModuleOrder::indexOf));
        return all;
    }

    @Override protected void init() {
        panelW = Math.min(width - 12, 650);
        panelH = Math.min(height - 12, 390);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        int sideW = panelW < 400 ? 78 : 114;
        contentX = panelX + sideW + 9;
        contentW = panelW - sideW - 21;
        top = panelY + 67;

        int tabY = panelY + 44;
        // Fit all navigation tabs above the bottom toolbar even at Pojav GUI scales.
        int tabSpacing = Math.max(18, Math.min(25, (panelH - 90) / TABS.length));
        for (int i = 0; i < TABS.length; i++) {
            final int selected = i;
            String symbol = switch (TABS[i]) {
                case "FPS" -> "⚡ ";
                case "Combat" -> "⚔ ";
                case "Visuals" -> "✧ ";
                case "HUD" -> "◈ ";
                case "Survival" -> "♥ ";
                case "World" -> "⌖ ";
                case "Movement" -> "➤ ";
                default -> "";
            };
            String name = (i == selectedTab ? "› " : "  ")
                + (sideW > 85 ? symbol : "") + TABS[i];
            addRenderableWidget(Button.builder(Component.literal(name), b -> {
                selectedTab = selected;
                page = 0;
                rebuildWidgets();
            }).bounds(panelX + 6, tabY + i * tabSpacing,
                sideW - 7, Math.min(22, tabSpacing - 1)).build());
        }

        searchBox = new EditBox(font, contentX, panelY + 38,
            contentW, 20, Component.literal("Search modules"));
        searchBox.setValue(query);
        searchBox.setHint(Component.literal("Search modules..."));
        searchBox.setResponder(s -> {
            if (!s.equals(query)) {
                query = s;
                page = 0;
                // Rebuilding inside EditBox's character callback destroys the
                // focused widget, especially on Android IME keyboards.
                searchRefreshPending = true;
            }
        });
        addRenderableWidget(searchBox);
        int headerRight = panelX + panelW - 7;
        addRenderableWidget(Button.builder(Component.literal("FILES"), b ->
            minecraft.gui.setScreen(new ClientImportsScreen()))
            .bounds(headerRight - 54, panelY + 6, 54, 21).build());
        favoritesFilterButton = addRenderableWidget(Button.builder(
            Component.literal("★"), b -> {
            favoritesOnly = !favoritesOnly;
            page = 0;
            refreshCards();
            updateFilterLabels();
        }).bounds(headerRight - 100, panelY + 6, 42, 21).build());
        enabledFilterButton = addRenderableWidget(Button.builder(
            Component.literal("ON"), b -> {
            enabledOnly = !enabledOnly;
            page = 0;
            refreshCards();
            updateFilterLabels();
        }).bounds(headerRight - 146, panelY + 6, 42, 21).build());
        updateFilterLabels();

        refreshCards();
        int footY = panelY + panelH - 30;
        int footerWidth = panelW - 12;
        int gap = 4;
        int small = Math.max(38,(footerWidth - 4 * gap) / 5);
        addRenderableWidget(Button.builder(Component.literal("Edit HUD"), b ->
            minecraft.gui.setScreen(new HudEditorScreen())
        ).bounds(panelX + 6, footY, small, 22).build());
        arrangeButton = addRenderableWidget(Button.builder(
            Component.literal(arrangeMode ? "Save Order" : "Arrange"), b -> {
            arrangeMode = !arrangeMode;
            selectedModule = null;
            ModuleOrder.save();
            arrangeButton.setMessage(Component.literal(arrangeMode ? "Save Order" : "Arrange"));
            refreshCards();
        }).bounds(panelX + 6 + (small + gap), footY, small, 22).build());
        previousPageButton = addRenderableWidget(Button.builder(Component.literal("< Prev"), b -> {
            if (page > 0) { page--; refreshCards(); }
        }).bounds(panelX + 6 + 2*(small + gap), footY, small, 22).build());
        nextPageButton = addRenderableWidget(Button.builder(Component.literal("Next >"), b -> {
            if (page < pages - 1) { page++; refreshCards(); }
        }).bounds(panelX + 6 + 3*(small + gap), footY, small, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b ->
            minecraft.gui.setScreen(null)
        ).bounds(panelX + 6 + 4*(small + gap), footY, small, 22).build());
        refreshPageButtons();
    }

    private void updateFilterLabels() {
        if (favoritesFilterButton != null)
            favoritesFilterButton.setMessage(Component.literal(favoritesOnly ? "★ ALL" : "☆"));
        if (enabledFilterButton != null)
            enabledFilterButton.setMessage(Component.literal(enabledOnly ? "ON ✓" : "ON"));
    }

    private void refreshPageButtons() {
        if (previousPageButton != null) previousPageButton.active = page > 0;
        if (nextPageButton != null) nextPageButton.active = page < pages - 1;
    }

    /** Only refresh module cards: never destroy or replace the focused EditBox. */
    private void refreshCards() {
        for (Button button : moduleButtons) removeWidget(button);
        moduleButtons.clear();
        List<Module> matches = filtered();
        tiles.clear();
        enabledCount = 0;
        for (Module m : DashClient.MODULES) if (m.enabled()) enabledCount++;
        columns = contentW >= 280 ? 2 : 1;
        int rowCount = Math.max(1, (panelY + panelH - 49 - top) / 27);
        perPage = Math.max(1, columns * rowCount);
        pages = Math.max(1, (matches.size() + perPage - 1) / perPage);
        page = Math.min(page, pages - 1);
        int buttonW = (contentW - 5 * (columns - 1)) / columns;

        for (int i = page * perPage; i < Math.min(matches.size(), (page + 1) * perPage); i++) {
            Module module = matches.get(i);
            int localIndex = i - page * perPage;
            int x = contentX + (localIndex % columns) * (buttonW + 5);
            int y = top + (localIndex / columns) * 27;
            tiles.add(new Tile(module, x, y, buttonW, 23));
            String state = module.enabled() ? " +" : " -";
            String icon = ModuleSymbols.symbol(module) + " ";
            String name = module.name();
            while (!name.isEmpty() && font.width(icon + name + state) > buttonW - 30)
                name = name.substring(0, name.length() - 1);
            Button tileButton = Button.builder(Component.literal(icon + name + state), b -> {
                if (arrangeMode) {
                    if (selectedModule == null) selectedModule = module;
                    else if (selectedModule != module) {
                        ModuleOrder.move(selectedModule, module);
                        selectedModule = null;
                        refreshCards();
                    } else selectedModule = null;
                    return;
                }
                module.setEnabled(!module.enabled());
                Config.save();
                refreshCards();
            }).bounds(x, y, buttonW - 21, 23).build();
            moduleButtons.add(addRenderableWidget(tileButton));
            Button favorite = Button.builder(Component.literal(
                ModuleFavorites.contains(module) ? "★" : "☆"), b -> {
                ModuleFavorites.toggle(module);
                refreshCards();
            }).bounds(x + buttonW - 20, y, 20, 23).build();
            moduleButtons.add(addRenderableWidget(favorite));
        }
        refreshPageButtons();
    }

    @Override public void tick() {
        super.tick();
        if (!searchRefreshPending) return;
        searchRefreshPending = false;
        // Rebuild after the input event completes and restore typing focus.
        // Characters continue to flow without tapping search again.
        refreshCards();
    }

    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean dbl) {
        if (arrangeMode && event.button() == 0) {
            dragStartX = (int) event.x();
            dragStartY = (int) event.y();
            dragMoved = false;
        }
        return super.mouseClicked(event, dbl);
    }

    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        if (arrangeMode && selectedModule != null && event.button() == 0) {
            if (Math.abs(event.x() - dragStartX) + Math.abs(event.y() - dragStartY) > 8)
                dragMoved = true;
            if (dragMoved) return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (arrangeMode && selectedModule != null && dragMoved) {
            for (Tile tile : tiles) {
                if (event.x() >= tile.x && event.x() <= tile.x + tile.width
                    && event.y() >= tile.y && event.y() <= tile.y + tile.height) {
                    if (tile.module != selectedModule) ModuleOrder.move(selectedModule, tile.module);
                    break;
                }
            }
            selectedModule = null;
            dragMoved = false;
            refreshCards();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g,
                                            int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0x99030712);
        g.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, 0xFF32729F);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF00D1426);
        g.fill(panelX + 3, panelY + 3, panelX + panelW - 3, panelY + 31, 0xFF142D44);
        g.fill(panelX + 3, panelY + 30, panelX + panelW - 3, panelY + 32, 0xFF30749E);
        g.fill(contentX - 5, panelY + 34, contentX - 3, panelY + panelH - 35, 0xFF32729F);
        super.extractRenderState(g, mouseX, mouseY, delta);
        // Clear enabled/disabled accent rails make scanning a large module list easy.
        for (Tile tile : tiles) {
            int accent = tile.module.enabled() ? 0xFF5DD9AB : 0xFF47556B;
            g.fill(tile.x + 1, tile.y + 3, tile.x + 3,
                tile.y + tile.height - 3, accent);
        }
        int navSpacing = Math.max(18, Math.min(25, (panelH - 90) / TABS.length));
        g.fill(panelX + 5, panelY + 44 + selectedTab * navSpacing,
            panelX + 8, panelY + 44 + selectedTab * navSpacing +
            Math.min(22, navSpacing - 1), 0xFF77E2FF);
        g.text(font, "DASHCLIENT", panelX + 12, panelY + 12, 0xFF79DFFF, true);
        if (panelW > 540)
            g.text(font, DashClient.MODULES.size() + " Modules | " + enabledCount + " On",
                panelX + 117, panelY + 12, 0xFFEAF6FF, true);
        if (panelW > 340)
            g.text(font, "v2.2", panelX + panelW - 175, panelY + 12,
                0xFFA4BCD3, true);
        if (arrangeMode) {
            for (Tile tile : tiles) if (tile.module == selectedModule) {
                g.fill(tile.x - 2, tile.y - 2, tile.x + tile.width + 2, tile.y, 0xFF77E2FF);
                g.fill(tile.x - 2, tile.y, tile.x, tile.y + tile.height, 0xFF77E2FF);
                g.fill(tile.x + tile.width, tile.y, tile.x + tile.width + 2,
                    tile.y + tile.height, 0xFF77E2FF);
                g.fill(tile.x - 2, tile.y + tile.height,
                    tile.x + tile.width + 2, tile.y + tile.height + 2, 0xFF77E2FF);
            }
        }
        g.centeredText(font, (arrangeMode ? "Tap a module, then its new spot | "
            : favoritesOnly ? "Favorites | " : enabledOnly ? "Enabled | " : TABS[selectedTab] + " | ") +
            "Page " + (page + 1) + "/" + pages,
            contentX + contentW / 2, panelY + panelH - 43, 0xFFA9CDEA);
    }
}
