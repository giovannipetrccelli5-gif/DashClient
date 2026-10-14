package com.dashclient.modules;

import com.dashclient.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Infers possible village locations from bells, beds and villagers that the
 * server has actually sent to the client. This is not /locate and cannot
 * reveal structures in chunks that are not loaded.
 */
public final class VillageFinderModule extends Module {
    private static final int RADIUS = 48;
    private static final int VERTICAL_RANGE = 20;
    private static final int COLUMNS_PER_TICK = 2;
    private static final int RESCAN_DELAY_TICKS = 160;
    private static final int MAX_BELLS = 24;
    private static final int MAX_BEDS = 96;

    private enum Quality {
        VILLAGER("Villager", false),
        VILLAGERS("Villagers", false),
        POSSIBLE("Possible village", true),
        LIKELY("Likely village", true);

        final String label;
        final boolean village;
        Quality(String label, boolean village) {
            this.label = label;
            this.village = village;
        }
    }

    private record Finding(BlockPos pos, Quality quality) {}

    private ClientLevel scannedLevel;
    private BlockPos center;
    private BlockPos lastScanLocation;
    private int scanX;
    private int rescanTicks;
    private boolean scanning;
    private final List<BlockPos> bells = new ArrayList<>();
    private final List<BlockPos> beds = new ArrayList<>();
    private Finding current;
    private Finding lastVillage;
    private BlockPos lastAnnounced;

    public VillageFinderModule() {
        super("Village Finder");
    }

    @Override
    public void onDisable() {
        clear();
    }

    private void clear() {
        scannedLevel = null;
        center = null;
        lastScanLocation = null;
        current = null;
        lastVillage = null;
        lastAnnounced = null;
        scanning = false;
        rescanTicks = 0;
        bells.clear();
        beds.clear();
    }

    private void begin(BlockPos player) {
        center = player.immutable();
        scanX = -RADIUS;
        bells.clear();
        beds.clear();
        scanning = true;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.level == null || client.player == null) {
            if (scannedLevel != null) clear();
            return;
        }

        if (scannedLevel != client.level) {
            clear(); // Never show coordinates from a previous world or dimension.
            scannedLevel = client.level;
        }

        BlockPos player = client.player.blockPosition();
        if (scanning && squaredDistance(player, center) > 48 * 48)
            begin(player); // Restart if the player travelled beyond the search area.

        if (!scanning) {
            if (rescanTicks > 0) rescanTicks--;
            if (lastScanLocation == null || rescanTicks == 0
                    || squaredDistance(player, lastScanLocation) > 20 * 20)
                begin(player);
            else return;
        }

        // Incremental search: at most two X columns per game tick on mobile.
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int col = 0; col < COLUMNS_PER_TICK && scanX <= RADIUS; col++, scanX++) {
            int x = center.getX() + scanX;
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                int z = center.getZ() + dz;
                cursor.set(x, center.getY(), z);
                if (!client.level.hasChunkAt(cursor)) continue;
                for (int dy = -VERTICAL_RANGE; dy <= VERTICAL_RANGE; dy++) {
                    cursor.set(x, center.getY() + dy, z);
                    BlockState state = client.level.getBlockState(cursor);
                    if (state.is(Blocks.BELL) && bells.size() < MAX_BELLS)
                        bells.add(cursor.immutable());
                    if (state.is(BlockTags.BEDS) && beds.size() < MAX_BEDS)
                        beds.add(cursor.immutable());
                }
            }
        }

        if (scanX <= RADIUS) return;

        scanning = false;
        lastScanLocation = center;
        rescanTicks = RESCAN_DELAY_TICKS;
        current = detect(client);
        if (current != null && current.quality().village) {
            lastVillage = current;
            if (!current.pos().equals(lastAnnounced)) {
                lastAnnounced = current.pos();
                BlockPos p = current.pos();
                client.player.displayClientMessage(Component.literal(
                    "[DashClient] " + current.quality().label + " around XYZ "
                        + p.getX() + " " + p.getY() + " " + p.getZ()
                        + " (loaded chunks only)"), false);
            }
        }
    }

    @Override
    public String hudText(Minecraft client) {
        if (client.level == null || client.player == null) return null;
        if (current != null) return format(current.quality().label, current.pos());
        if (lastVillage != null) return format("Last village", lastVillage.pos());
        return scanning ? "Village Finder: Scanning..." :
            "Village Finder: No evidence nearby";
    }

    private static String format(String label, BlockPos pos) {
        return label + ": " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private Finding detect(Minecraft client) {
        List<Villager> villagers = client.level.getEntitiesOfClass(
            Villager.class, client.player.getBoundingBox().inflate(64.0), v -> true);
        BlockPos playerPos = client.player.blockPosition();
        Finding best = null;
        double bestDistance = Double.POSITIVE_INFINITY;

        // A bell is a useful approximate centre, especially with multiple
        // villagers or nearby beds. A standalone bell is only a possible match.
        for (BlockPos bell : bells) {
            int villagersNear = countVillagers(villagers, bell, 40);
            int bedsNear = countBlocks(beds, bell, 32);
            Quality quality = villagersNear >= 2 || (villagersNear >= 1 && bedsNear >= 2)
                ? Quality.LIKELY : Quality.POSSIBLE;
            Finding candidate = new Finding(bell, quality);
            double distance = squaredDistance(playerPos, bell);
            if (best == null || quality.ordinal() > best.quality().ordinal()
                    || (quality == best.quality() && distance < bestDistance)) {
                best = candidate;
                bestDistance = distance;
            }
        }

        // Villages do not always have bells. Multiple villagers with at least
        // one two-block bed can suggest a village but cannot prove generation.
        for (Villager villager : villagers) {
            BlockPos pos = villager.blockPosition();
            int villagersNear = countVillagers(villagers, pos, 32);
            int bedsNear = countBlocks(beds, pos, 32);
            Quality quality = villagersNear >= 2 && bedsNear >= 2
                ? Quality.POSSIBLE : villagersNear >= 2 ? Quality.VILLAGERS : Quality.VILLAGER;
            double distance = squaredDistance(playerPos, pos);
            if (best == null || quality.ordinal() > best.quality().ordinal()
                    || (quality == best.quality() && distance < bestDistance)) {
                best = new Finding(pos.immutable(), quality);
                bestDistance = distance;
            }
        }
        return best;
    }

    private static int countVillagers(List<Villager> villagers, BlockPos origin, int range) {
        int count = 0;
        for (Villager villager : villagers)
            if (squaredDistance(villager.blockPosition(), origin) <= range * range) count++;
        return count;
    }

    private static int countBlocks(List<BlockPos> blocks, BlockPos origin, int range) {
        int count = 0;
        for (BlockPos pos : blocks)
            if (squaredDistance(pos, origin) <= range * range) count++;
        return count;
    }

    private static long squaredDistance(BlockPos a, BlockPos b) {
        long dx = (long) a.getX() - b.getX();
        long dy = (long) a.getY() - b.getY();
        long dz = (long) a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
