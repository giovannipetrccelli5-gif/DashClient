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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Client-side ore X-Ray: projects boxes around ores already present in loaded chunks.
 * Uses HUD extraction, not world/chunk renderer mixins, to remain compatible with Sodium.
 * It cannot see chunks or block data the server has not sent.
 */
public final class OreXrayModule extends Module {
    private static final int HORIZONTAL_RADIUS = 20;
    private static final int VERTICAL_RADIUS = 16;
    private static final int COLUMNS_PER_TICK = 3;
    private static final int MAX_MARKERS = 72;

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
    private int nextX = -HORIZONTAL_RADIUS;
    private final List<Target> collecting = new ArrayList<>();
    private List<Target> nearby = List.of();

    public OreXrayModule() {
        super("X-Ray");
    }

    @Override
    public void onDisable() {
        clear();
    }

    private void clear() {
        scannedLevel = null;
        scanCenter = null;
        nextX = -HORIZONTAL_RADIUS;
        collecting.clear();
        nearby = List.of();
    }

    private void startScan(BlockPos center) {
        scanCenter = center.immutable();
        nextX = -HORIZONTAL_RADIUS;
        collecting.clear();
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            clear();
            return;
        }
        if (scannedLevel != client.level) {
            clear();
            scannedLevel = client.level;
        }
        BlockPos playerPos = client.player.blockPosition();
        if (scanCenter == null
                || Math.abs(playerPos.getX() - scanCenter.getX()) > 8
                || Math.abs(playerPos.getY() - scanCenter.getY()) > 8
                || Math.abs(playerPos.getZ() - scanCenter.getZ()) > 8) {
            startScan(playerPos);
        }

        // Incremental scan limits the per-tick cost on Android.
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int columns = 0; columns < COLUMNS_PER_TICK
                && nextX <= HORIZONTAL_RADIUS; columns++, nextX++) {
            int x = scanCenter.getX() + nextX;
            for (int dz = -HORIZONTAL_RADIUS; dz <= HORIZONTAL_RADIUS; dz++) {
                int z = scanCenter.getZ() + dz;
                cursor.set(x, scanCenter.getY(), z);
                if (!client.level.hasChunkAt(cursor)) continue;
                for (int dy = -VERTICAL_RADIUS; dy <= VERTICAL_RADIUS; dy++) {
                    cursor.set(x, scanCenter.getY() + dy, z);
                    Ore ore = oreFor(client.level.getBlockState(cursor));
                    if (ore != null) collecting.add(new Target(cursor.immutable(), ore));
                }
            }
        }
        if (nextX > HORIZONTAL_RADIUS) {
            BlockPos center = scanCenter;
            collecting.sort(Comparator.comparingDouble(t -> distanceSquared(t.position(), center)));
            nearby = List.copyOf(collecting.subList(0, Math.min(MAX_MARKERS, collecting.size())));
            // Keep the last complete scan on screen until the next one completes.
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
