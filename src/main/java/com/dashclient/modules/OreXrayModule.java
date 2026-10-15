package com.dashclient.modules;

import com.dashclient.Module;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.ArrayList;

/**
 * Client-side ore X-Ray: projects boxes around ores already present in loaded chunks.
 * Uses HUD extraction, not world/chunk renderer mixins, to remain compatible with Sodium.
 * It cannot see chunks or block data the server has not sent.
 */
public final class OreXrayModule extends Module {
    private static final int MAX_RADIUS = 32 * 16;
    private static final int CHUNKS_PER_TICK = 2;
    private static final int SNAPSHOT_INTERVAL_CHUNKS = 12;
    private static final int MAX_MARKERS = 512;


    private enum Ore {
        DIAMOND("Diamond", 0xFF5CF4F4),
        EMERALD("Emerald", 0xFF4EF18B),
        ANCIENT_DEBRIS("Debris", 0xFFFFB26C),
        GOLD("Gold", 0xFFFFD44D),
        IRON("Iron", 0xFFE3D3BE),
        COPPER("Copper", 0xFFFF9954),
        REDSTONE("Redstone", 0xFFFF6161),
        LAPIS("Lapis", 0xFF599BFF),
        COAL("Coal", 0xFFA8B2C0),
        QUARTZ("Quartz", 0xFFF9F4E5);

        final String label;
        final int color;

        Ore(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }

    private record Target(BlockPos position, Ore ore) {}
    private record Box(int left, int top, int right, int bottom) {}

    private ClientLevel scannedLevel;
    private BlockPos scanCenter;
    private int nextChunk = 0;
    private int radiusChunks = 0;
    // A bounded heap prevents a full render-distance search from allocating
    // an unbounded list for coal, copper, iron and other common ores.
    private final PriorityQueue<Target> collecting = new PriorityQueue<>(
        Comparator.<Target>comparingDouble(t -> distanceSquared(t.position(), scanCenter)).reversed());
    private List<BlockPos> chunkOrder = List.of();
    private List<Target> nearby = List.of();

    public OreXrayModule() { super("X-Ray"); }

    @Override public void onDisable() { clear(); }

    private void clear() {
        scannedLevel = null;
        scanCenter = null;
        nextChunk = 0;
        radiusChunks = 0;
        collecting.clear();
        chunkOrder = List.of();
        nearby = List.of();
    }

    private void startScan(BlockPos center, int chunks) {
        scanCenter = center.immutable();
        radiusChunks = chunks;
        nextChunk = 0;
        collecting.clear();
        // Near-first order makes useful results appear quickly even before a
        // very large search finishes. Beyond-server chunks are never loaded.
        List<BlockPos> ordered = new ArrayList<>();
        for (int dx = -chunks; dx <= chunks; dx++) {
            for (int dz = -chunks; dz <= chunks; dz++) {
                if (dx * dx + dz * dz > chunks * chunks) continue;
                ordered.add(new BlockPos((center.getX() >> 4) + dx, 0,
                    (center.getZ() >> 4) + dz));
            }
        }
        ordered.sort(Comparator.comparingDouble(pos -> {
            double dx = pos.getX() - (center.getX() >> 4);
            double dz = pos.getZ() - (center.getZ() >> 4);
            return dx * dx + dz * dz;
        }));
        chunkOrder = List.copyOf(ordered);
    }

    @Override public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) { clear(); return; }
        if (scannedLevel != client.level) { clear(); scannedLevel = client.level; }
        BlockPos playerPos = client.player.blockPosition();
        int chunks = Math.max(2, Math.min(MAX_RADIUS / 16, client.options.renderDistance().get()));
        if (scanCenter == null || chunks != radiusChunks
                || Math.abs(playerPos.getX() - scanCenter.getX()) > 16
                || Math.abs(playerPos.getZ() - scanCenter.getZ()) > 16
                || Math.abs(playerPos.getY() - scanCenter.getY()) > 16) {
            startScan(playerPos, chunks);
        }

        // Incremental near-first scan; never call getChunk for unloaded
        // coordinates. Show snapshots during the scan instead of waiting
        // for the outermost chunk before showing any markers.
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int batch = 0; batch < CHUNKS_PER_TICK && nextChunk < chunkOrder.size();
             batch++, nextChunk++) {
            BlockPos cp = chunkOrder.get(nextChunk);
            int cx = cp.getX();
            int cz = cp.getZ();
            cursor.set(cx << 4, scanCenter.getY(), cz << 4);
            if (!client.level.hasChunkAt(cursor)) continue;
            var chunk = client.level.getChunk(cx, cz);
            int minSection = client.level.getMinSectionY();
            int sections = chunk.getSectionsCount();
            for (int si = 0; si < sections; si++) {
                var section = chunk.getSection(si);
                if (section.hasOnlyAir() || !section.maybeHas(state -> oreFor(state) != null))
                    continue;
                int baseY = (minSection + si) << 4;
                for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++)
                    for (int x = 0; x < 16; x++) {
                        Ore ore = oreFor(section.getBlockState(x, y, z));
                        if (ore == null) continue;
                        cursor.set((cx << 4) + x, baseY + y, (cz << 4) + z);
                        Target candidate = new Target(cursor.immutable(), ore);
                        if (collecting.size() < MAX_MARKERS) {
                            collecting.add(candidate);
                        } else if (distanceSquared(candidate.position(), scanCenter)
                                < distanceSquared(collecting.peek().position(), scanCenter)) {
                            collecting.poll();
                            collecting.add(candidate);
                        }
                    }
            }
        }
        if (nextChunk % SNAPSHOT_INTERVAL_CHUNKS < CHUNKS_PER_TICK
                || nextChunk >= chunkOrder.size()) {
            List<Target> visible = new ArrayList<>(collecting);
            visible.sort(Comparator.comparingDouble(t ->
                distanceSquared(t.position(), client.player.blockPosition())));
            nearby = List.copyOf(visible);
        }
        if (nextChunk >= chunkOrder.size()) {
            // Preserve markers while waiting for the next refresh; a complete
            // round doesn't need to clear the overlay.
            scanCenter = null;
            collecting.clear();
        }
    }

    private static double distanceSquared(BlockPos a, BlockPos b) {
        double x = a.getX() - b.getX();
        double y = a.getY() - b.getY();
        double z = a.getZ() - b.getZ();
        return x * x + y * y + z * z;
    }

    private static Ore oreFor(BlockState state) {
        Block b = state.getBlock();
        if (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE) return Ore.DIAMOND;
        if (b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE) return Ore.EMERALD;
        if (b == Blocks.ANCIENT_DEBRIS) return Ore.ANCIENT_DEBRIS;
        if (b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE
                || b == Blocks.NETHER_GOLD_ORE) return Ore.GOLD;
        if (b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE) return Ore.IRON;
        if (b == Blocks.COPPER_ORE || b == Blocks.DEEPSLATE_COPPER_ORE) return Ore.COPPER;
        if (b == Blocks.REDSTONE_ORE || b == Blocks.DEEPSLATE_REDSTONE_ORE) return Ore.REDSTONE;
        if (b == Blocks.LAPIS_ORE || b == Blocks.DEEPSLATE_LAPIS_ORE) return Ore.LAPIS;
        if (b == Blocks.COAL_ORE || b == Blocks.DEEPSLATE_COAL_ORE) return Ore.COAL;
        if (b == Blocks.NETHER_QUARTZ_ORE) return Ore.QUARTZ;
        return null;
    }

    /**
     * Draw screen-space ore boxes after the world is rendered. No depth test is
     * applied to HUD graphics, so these remain visible through opaque stone.
     */
    public void renderOverlay(Minecraft client, GuiGraphicsExtractor graphics) {
        if (client.level == null || client.player == null
                || nearby.isEmpty() || client.gui.screen() != null) return;
        Camera camera = client.gameRenderer.mainCamera();
        if (!camera.isInitialized()) return;
        Vec3 origin = camera.position();
        Quaternionf inverse = new Quaternionf(camera.rotation()).conjugate();
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        if (screenWidth <= 0 || screenHeight <= 0) return;
        double fov = Math.max(30, Math.min(120, camera.getFov()));
        float focal = (float) ((screenHeight * 0.5) / Math.tan(Math.toRadians(fov * 0.5)));
        Vector3f temp = new Vector3f();
        int labels = 0;

        for (Target target : nearby) {
            Box box = project(target.position(), origin, inverse, focal,
                screenWidth, screenHeight, temp);
            if (box == null) continue;
            int left = Math.max(0, box.left());
            int top = Math.max(0, box.top());
            int right = Math.min(screenWidth, box.right());
            int bottom = Math.min(screenHeight, box.bottom());
            if (right - left < 3 || bottom - top < 3) continue;
            graphics.fill(left, top, right, bottom, (target.ore().color & 0xFFFFFF) | 0x14000000);
            graphics.outline(left, top, right - left, bottom - top, target.ore().color);
            if (labels < 8 && right - left >= 10 && top >= 12) {
                double dx = target.position().getX() + 0.5 - origin.x;
                double dy = target.position().getY() + 0.5 - origin.y;
                double dz = target.position().getZ() + 0.5 - origin.z;
                int meters = (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
                graphics.text(client.font, target.ore().label + " " + meters + "m",
                    left, top - 11, target.ore().color, true);
                labels++;
            }
        }
    }

    // Projects all eight cube corners so labels and borders follow perspective.
    private static Box project(BlockPos pos, Vec3 cameraPos, Quaternionf inverse,
                               float focal, int width, int height, Vector3f temp) {
        float left = Float.POSITIVE_INFINITY;
        float top = Float.POSITIVE_INFINITY;
        float right = Float.NEGATIVE_INFINITY;
        float bottom = Float.NEGATIVE_INFINITY;
        for (int ix = 0; ix <= 1; ix++) {
            for (int iy = 0; iy <= 1; iy++) {
                for (int iz = 0; iz <= 1; iz++) {
                    temp.set((float) (pos.getX() + ix - cameraPos.x),
                             (float) (pos.getY() + iy - cameraPos.y),
                             (float) (pos.getZ() + iz - cameraPos.z)).rotate(inverse);
                    if (temp.z >= -0.12f) return null;
                    float sx = width / 2f + temp.x * focal / -temp.z;
                    float sy = height / 2f - temp.y * focal / -temp.z;
                    left = Math.min(left, sx);
                    top = Math.min(top, sy);
                    right = Math.max(right, sx);
                    bottom = Math.max(bottom, sy);
                }
            }
        }
        if (left >= width || right <= 0 || top >= height || bottom <= 0
                || right - left > width * 0.8f || bottom - top > height * 0.8f) return null;
        return new Box((int) Math.floor(left), (int) Math.floor(top),
                       (int) Math.ceil(right), (int) Math.ceil(bottom));
    }
}
