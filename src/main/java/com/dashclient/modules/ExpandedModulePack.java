package com.dashclient.modules;

import com.dashclient.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.function.Function;

/**
 * Optional HUD/diagnostics pack designed for low-cost mobile sampling.
 * Item counters share one cached inventory scan, never packet or world hacks.
 */
public final class ExpandedModulePack {
    private ExpandedModulePack() {}
    private static final Map<Item,Integer> COUNTS = new HashMap<>();
    private static Object lastPlayer;
    private static int inventoryAge;

    public static void tick(Minecraft c) {
        if (c.player == null) {
            COUNTS.clear();
            lastPlayer = null;
            inventoryAge = 0;
            return;
        }
        if (lastPlayer == c.player && ++inventoryAge < 10) return;
        lastPlayer = c.player;
        inventoryAge = 0;
        COUNTS.clear();
        var inv = c.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) COUNTS.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
    }
    private static void add(List<Module> list, String name, Function<Minecraft,String> readout) {
        list.add(new Module(name) {
            @Override public String hudText(Minecraft c) { return readout.apply(c); }
        });
    }
    private static void item(List<Module> list, String name, Item item) {
        add(list, "Count: " + name, c -> c.player == null ? null :
            name + ": " + COUNTS.getOrDefault(item, 0));
    }
    private static String fmt(double d) {
        return String.format(Locale.ROOT, "%.1f", d);
    }
    public static void register(List<Module> list) {
        // Opt-in HUD workload controls. Eco refresh saves CPU; minimal renders
        // only the first 8 enabled readouts; fast refresh increases freshness.
        list.add(new Module("HUD Eco Refresh") {});
        list.add(new Module("HUD Fast Refresh") {});
        list.add(new Module("HUD Minimal Mode") {});

        item(list, "Bow", Items.BOW);
        item(list, "Crossbow", Items.CROSSBOW);
        item(list, "Shield", Items.SHIELD);
        item(list, "Elytra", Items.ELYTRA);
        item(list, "Trident", Items.TRIDENT);
        item(list, "Mace", Items.MACE);
        item(list, "Netherite Swords", Items.NETHERITE_SWORD);
        item(list, "Diamond Swords", Items.DIAMOND_SWORD);
        item(list, "Netherite Pickaxes", Items.NETHERITE_PICKAXE);
        item(list, "Diamond Pickaxes", Items.DIAMOND_PICKAXE);
        item(list, "Netherite Axes", Items.NETHERITE_AXE);
        item(list, "Diamond Axes", Items.DIAMOND_AXE);
        item(list, "Netherite Shovels", Items.NETHERITE_SHOVEL);
        item(list, "Diamond Shovels", Items.DIAMOND_SHOVEL);
        item(list, "Netherite Helmets", Items.NETHERITE_HELMET);
        item(list, "Netherite Chestplates", Items.NETHERITE_CHESTPLATE);
        item(list, "Netherite Leggings", Items.NETHERITE_LEGGINGS);
        item(list, "Netherite Boots", Items.NETHERITE_BOOTS);
        item(list, "Diamond Helmets", Items.DIAMOND_HELMET);
        item(list, "Diamond Chestplates", Items.DIAMOND_CHESTPLATE);
        item(list, "Diamond Leggings", Items.DIAMOND_LEGGINGS);
        item(list, "Diamond Boots", Items.DIAMOND_BOOTS);
        item(list, "Ender Chests", Items.ENDER_CHEST);
        item(list, "Shulker Boxes", Items.SHULKER_BOX);
        item(list, "Hoppers", Items.HOPPER);
        item(list, "Furnaces", Items.FURNACE);
        item(list, "Blast Furnaces", Items.BLAST_FURNACE);
        item(list, "Smokers", Items.SMOKER);
        item(list, "Iron Blocks", Items.IRON_BLOCK);
        item(list, "Gold Blocks", Items.GOLD_BLOCK);
        item(list, "Diamond Blocks", Items.DIAMOND_BLOCK);
        item(list, "Netherite Blocks", Items.NETHERITE_BLOCK);
        item(list, "Emerald Blocks", Items.EMERALD_BLOCK);
        item(list, "Redstone Blocks", Items.REDSTONE_BLOCK);
        item(list, "Books", Items.BOOK);
        item(list, "Enchanted Books", Items.ENCHANTED_BOOK);
        item(list, "Paper", Items.PAPER);
        item(list, "Leather", Items.LEATHER);
        item(list, "Torches", Items.TORCH);
        item(list, "Soul Torches", Items.SOUL_TORCH);
        item(list, "Lanterns", Items.LANTERN);
        item(list, "Scaffolding", Items.SCAFFOLDING);
        item(list, "Crafting Tables", Items.CRAFTING_TABLE);
        item(list, "Anvils", Items.ANVIL);
        item(list, "Grindstones", Items.GRINDSTONE);
        item(list, "Fishing Rods", Items.FISHING_ROD);
        item(list, "Brushes", Items.BRUSH);
        item(list, "Spyglasses", Items.SPYGLASS);
        item(list, "Fire Charges", Items.FIRE_CHARGE);
        item(list, "Flint And Steel", Items.FLINT_AND_STEEL);

        // JVM heap ≠ total device RAM; these are accurate Java counters.
        add(list, "Diagnostic: Heap Used", c -> {
            Runtime r = Runtime.getRuntime();
            return "Heap used: " + (r.totalMemory() - r.freeMemory()) / 1048576L + " MiB";
        });
        add(list, "Diagnostic: Heap Allocated", c ->
            "Heap allocated: " + Runtime.getRuntime().totalMemory() / 1048576L + " MiB");
        add(list, "Diagnostic: Heap Maximum", c ->
            "Heap maximum: " + Runtime.getRuntime().maxMemory() / 1048576L + " MiB");
        add(list, "Diagnostic: Heap Free", c ->
            "Heap free: " + Runtime.getRuntime().freeMemory() / 1048576L + " MiB");
        add(list, "Diagnostic: CPU Cores", c ->
            "CPU logical cores: " + Runtime.getRuntime().availableProcessors());
        add(list, "Diagnostic: FPS Cap", c -> c.options == null ? null :
            "FPS cap: " + c.options.framerateLimit().get());
        add(list, "Diagnostic: Entity Render Scale", c -> c.options == null ? null :
            "Entity render scale: " + fmt(c.options.entityDistanceScaling().get()));
        add(list, "Diagnostic: Graphics View", c -> c.options == null ? null :
            "Visible chunks: " + c.options.renderDistance().get());
        add(list, "Diagnostic: Simulation View", c -> c.options == null ? null :
            "Simulation chunks: " + c.options.simulationDistance().get());
        add(list, "Diagnostic: Approx Frame Time", c -> c.getFps() == 0 ?
            "Approx frame: --" : "Approx frame: " + fmt(1000.0/c.getFps()) + "ms");
        add(list, "Diagnostic: 60 FPS Budget", c -> c.getFps() <= 0 ? null :
            "60 FPS budget left: " + fmt(16.67 - 1000.0/c.getFps()) + "ms");
        add(list, "Diagnostic: 30 FPS Budget", c -> c.getFps() <= 0 ? null :
            "30 FPS budget left: " + fmt(33.33 - 1000.0/c.getFps()) + "ms");

        add(list, "Player: X Position", c -> c.player == null ? null :
            "X: " + fmt(c.player.getX()));
        add(list, "Player: Y Position", c -> c.player == null ? null :
            "Y: " + fmt(c.player.getY()));
        add(list, "Player: Z Position", c -> c.player == null ? null :
            "Z: " + fmt(c.player.getZ()));
        add(list, "Player: Eye Height", c -> c.player == null ? null :
            "Eye Y: " + fmt(c.player.getEyeY()));
        add(list, "Player: Facing Angle", c -> c.player == null ? null :
            "Yaw: " + fmt(c.player.getYRot()));
        add(list, "Player: Look Angle", c -> c.player == null ? null :
            "Pitch: " + fmt(c.player.getXRot()));
        add(list, "Player: Current Health", c -> c.player == null ? null :
            "Health: " + fmt(c.player.getHealth()));
        add(list, "Player: Current Hunger", c -> c.player == null ? null :
            "Hunger: " + c.player.getFoodData().getFoodLevel());
        add(list, "Player: Current Saturation", c -> c.player == null ? null :
            "Saturation: " + fmt(c.player.getFoodData().getSaturationLevel()));
        add(list, "Player: Potion Count", c -> c.player == null ? null :
            "Active effects: " + c.player.getActiveEffects().size());
        add(list, "Player: Invisibility", c -> c.player == null ? null :
            "Invisible: " + (c.player.isInvisible() ? "Yes" : "No"));
        add(list, "Player: Burning", c -> c.player == null ? null :
            "Burning: " + (c.player.isOnFire() ? "Yes" : "No"));
        add(list, "Player: Wet", c -> c.player == null ? null :
            "In water: " + (c.player.isInWater() ? "Yes" : "No"));
        add(list, "Player: Air Supply", c -> c.player == null ? null :
            "Breath: " + c.player.getAirSupply());
        add(list, "Player: Absorption", c -> c.player == null ? null :
            "Absorption: " + fmt(c.player.getAbsorptionAmount()));
        add(list, "Movement: Speed X", c -> c.player == null ? null :
            "Motion X: " + fmt(c.player.getDeltaMovement().x * 20) + " b/s");
        add(list, "Movement: Speed Y", c -> c.player == null ? null :
            "Motion Y: " + fmt(c.player.getDeltaMovement().y * 20) + " b/s");
        add(list, "Movement: Speed Z", c -> c.player == null ? null :
            "Motion Z: " + fmt(c.player.getDeltaMovement().z * 20) + " b/s");
        add(list, "Movement: Ground State", c -> c.player == null ? null :
            "Grounded: " + (c.player.onGround() ? "Yes" : "No"));
        add(list, "Movement: Swim State", c -> c.player == null ? null :
            "Swimming: " + (c.player.isSwimming() ? "Yes" : "No"));
        add(list, "Movement: Glide State", c -> c.player == null ? null :
            "Gliding: " + (c.player.isFallFlying() ? "Yes" : "No"));
        add(list, "Movement: Sneak State", c -> c.player == null ? null :
            "Sneaking: " + (c.player.isShiftKeyDown() ? "Yes" : "No"));
        add(list, "Movement: Sprint State", c -> c.player == null ? null :
            "Sprinting: " + (c.player.isSprinting() ? "Yes" : "No"));
        add(list, "World: Chunk X", c -> c.player == null ? null :
            "Chunk X: " + (c.player.blockPosition().getX() >> 4));
        add(list, "World: Chunk Z", c -> c.player == null ? null :
            "Chunk Z: " + (c.player.blockPosition().getZ() >> 4));
        add(list, "World: Block X In Chunk", c -> c.player == null ? null :
            "Chunk local X: " + (c.player.blockPosition().getX() & 15));
        add(list, "World: Block Z In Chunk", c -> c.player == null ? null :
            "Chunk local Z: " + (c.player.blockPosition().getZ() & 15));
        add(list, "World: Current Day", c -> c.level == null ? null :
            "Minecraft day: " + (c.level.getDayTime() / 24000));
        add(list, "World: Sun Time", c -> c.level == null ? null :
            "Day tick: " + (c.level.getDayTime() % 24000));
        add(list, "World: Thunder", c -> c.level == null ? null :
            "Thunder: " + (c.level.isThundering() ? "Yes" : "No"));
        add(list, "World: Rain", c -> c.level == null ? null :
            "Rain: " + (c.level.isRaining() ? "Yes" : "No"));
    }
}
