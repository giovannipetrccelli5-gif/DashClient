package com.dashclient;

import com.dashclient.modules.AutoMinerModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Searchable selector for every registered vanilla/modded item.
 * Non-block items are visible but cannot be chosen as mining targets.
 * Refreshes results after the keystroke so Android typing stays focused.
 */
public final class AutoMinerScreen extends Screen {
    private final Screen parent;
    private final AutoMinerModule miner;
    private final List<Item> allItems = new ArrayList<>();
    private final List<Button> itemButtons = new ArrayList<>();
    private final List<Cell> cells = new ArrayList<>();
    private record Cell(Item item, int x, int y, int w, int h) {}

    private EditBox search;
    private Button nextButton, previousButton, startButton;
    private String query = "";
    private String message = "Search all items; choose a BLOCK to mine";
    private boolean filterDirty;
    private int page;
    private int pages;
    private int resultCount;
    private int panelX, panelY, panelW, panelH, contentX, contentY, columns, rows;

    public AutoMinerScreen(Screen parent, AutoMinerModule miner) {
        super(Component.literal("DashClient - Auto Miner"));
        this.parent = parent;
        this.miner = miner;
        for (Item item : BuiltInRegistries.ITEM) allItems.add(item);
        allItems.sort(Comparator.comparing(item -> label(item).toLowerCase(Locale.ROOT)));
    }

    private static String label(Item item) {
        return new ItemStack(item).getHoverName().getString();
    }

    @Override protected void init() {
        panelW = Math.min(width - 12, 660);
        panelH = Math.min(height - 12, 400);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        contentX = panelX + 9;
        contentY = panelY + 83;
        int cellWidth = panelW - 18;
        columns = cellWidth >= 465 ? 3 : cellWidth >= 285 ? 2 : 1;
        rows = Math.max(1, Math.min(10, (panelH - 157) / 26));

        search = new EditBox(font, contentX, panelY + 48,
            panelW - 18, 22, Component.literal("Search Minecraft items"));
        search.setValue(query);
        search.setHint(Component.literal("Search by item name, e.g. diamond ore..."));
        search.setResponder(s -> {
            if (!s.equals(query)) {
                query = s;
                page = 0;
                filterDirty = true; // preserve EditBox focus for phone keyboards
            }
        });
        addRenderableWidget(search);

        int footerY = panelY + panelH - 34;
        int w = Math.max(40, (panelW - 38) / 4);
        previousButton = addRenderableWidget(Button.builder(Component.literal("Prev"), b -> {
            if (page > 0) { page--; rebuildItems(); }
        }).bounds(contentX, footerY, w, 22).build());
        nextButton = addRenderableWidget(Button.builder(Component.literal("Next"), b -> {
            if (page + 1 < pages) { page++; rebuildItems(); }
        }).bounds(contentX + w + 4, footerY, w, 22).build());
        startButton = addRenderableWidget(Button.builder(Component.literal("Start Mining"), b -> {
            if (miner.start(minecraft)) {
                minecraft.gui.setScreen(null); // close before gameplay begins
            } else {
                message = miner.status();
            }
        }).bounds(contentX + 2 * (w + 4), footerY, w, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Stop / Back"), b -> {
            miner.stop(minecraft, true);
            minecraft.gui.setScreen(parent);
        }).bounds(contentX + 3 * (w + 4), footerY, w, 22).build());

        rebuildItems();
    }

    private List<Item> filtered() {
        String text = query.strip().toLowerCase(Locale.ROOT).replace('_', ' ');
        if (text.isEmpty()) return allItems;
        List<Item> results = new ArrayList<>();
        for (Item item : allItems) {
            String name = label(item).toLowerCase(Locale.ROOT);
            String id = BuiltInRegistries.ITEM.getKey(item).toString().replace('_', ' ');
            if (name.contains(text) || id.contains(text)) results.add(item);
        }
        return results;
    }

    private void rebuildItems() {
        for (Button button : itemButtons) removeWidget(button);
        itemButtons.clear();
        cells.clear();
        List<Item> matches = filtered();
        resultCount = matches.size();
        int perPage = Math.max(1, columns * rows);
        pages = Math.max(1, (matches.size() + perPage - 1) / perPage);
        page = Math.min(page, pages - 1);
        int gap = 4;
        int cellW = (panelW - 18 - (columns - 1) * gap) / columns;

        for (int i = page * perPage; i < Math.min(matches.size(), (page + 1) * perPage); i++) {
            Item item = matches.get(i);
            int index = i - page * perPage;
            int x = contentX + (index % columns) * (cellW + gap);
            int y = contentY + (index / columns) * 26;
            cells.add(new Cell(item, x, y, cellW, 23));
            String name = label(item);
            int maxWidth = cellW - 30;
            while (!name.isEmpty() && font.width(name) > maxWidth)
                name = name.substring(0, name.length() - 1);
            final String selectedLabel = label(item);
            Button button = addRenderableWidget(Button.builder(
                Component.literal("    " + name), b -> {
                    if (item instanceof BlockItem) {
                        miner.select(item);
                        message = "Selected: " + selectedLabel;
                    } else {
                        message = selectedLabel + " is an item, not a block";
                    }
                    updateStart();
                }).bounds(x, y, cellW, 23).build());
            itemButtons.add(button);
        }

        previousButton.active = page > 0;
        nextButton.active = page + 1 < pages;
        updateStart();
    }

    private void updateStart() {
        startButton.active = miner.selectedItem() instanceof BlockItem;
    }

    @Override public void tick() {
        super.tick();
        if (filterDirty) {
            filterDirty = false;
            rebuildItems(); // do not replace the focused search field
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g,
                                            int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xAA050B15);
        g.fill(panelX - 1, panelY - 1, panelX + panelW + 1,
            panelY + panelH + 1, 0xFF24769B);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF00D1728);
        g.fill(panelX + 2, panelY + 2, panelX + panelW - 2, panelY + 37, 0xFF123048);

        super.extractRenderState(g, mouseX, mouseY, delta);
        g.centeredText(font, "DASHCLIENT / AUTO MINER",
            panelX + panelW / 2, panelY + 11, 0xFF86DDFF);
        g.centeredText(font, "All items shown • only blocks can be mined",
            panelX + panelW / 2, panelY + 28, 0xFFAECFE5);
        for (Cell cell : cells) {
            int border = cell.item == miner.selectedItem() ? 0xFF6EF3C3
                : cell.item instanceof BlockItem ? 0xFF265C76 : 0xFF39465A;
            g.fill(cell.x, cell.y, cell.x + cell.w, cell.y + 1, border);
            g.item(new ItemStack(cell.item), cell.x + 4, cell.y + 3);
        }
        String status = message;
        while (!status.isEmpty() && font.width(status) > panelW - 20)
            status = status.substring(0, status.length() - 1);
        g.centeredText(font, status,
            panelX + panelW / 2, panelY + panelH - 65, 0xFFBFE6FF);
        g.centeredText(font, "Page " + (page + 1) + " / " + pages
            + " • " + resultCount + " results",
            panelX + panelW / 2, panelY + panelH - 48, 0xFF86B4D2);
    }
}
