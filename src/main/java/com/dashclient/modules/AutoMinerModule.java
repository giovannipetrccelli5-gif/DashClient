package com.dashclient.modules;

import com.dashclient.AutoMinerScreen;
import com.dashclient.DashClient;
import com.dashclient.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Normal-reach, client-directed mining of a selected block.
 *
 * Uses ordinary MultiPlayerGameMode block-break actions and walks only along
 * safe, flat, loaded paths. Never teleports, mines through walls, or requests
 * hidden server chunks. Auto Miner intentionally does not tunnel through
 * intervening blocks, descend shafts, or cross lava/water.
 */
public final class AutoMinerModule extends Module {
    private static final int SEARCH_RADIUS = 16;
    private static final int HEIGHT_RADIUS = 5;
    private static final int SCAN_INTERVAL = 20;
    private static final int MAX_CANDIDATES = 64;
    private static final int MAX_VISITED = 1200;
    private static final int MAX_TOTAL_TICKS = 20 * 60 * 30; // 30 minutes per run
    private static final int TARGET_STALL_TICKS = 20 * 16;
    private static final int WALK_STALL_TICKS = 60;
    private static final double REACH_SQUARED = 15.0; // < 4 blocks
    private static final Direction[] HORIZONTAL = {
        Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    private Item selectedItem;
    private Block selectedBlock;
    private boolean choosing;
    private boolean running;
    private int ticks;
    private int lastScan;
    private int targetTicks;
    private int walkingStall;
    private int mined;
    private String status = "Select a block to mine";
    private ClientLevel world;
    private BlockPos anchor;
    private BlockPos target;
    private final ArrayDeque<BlockPos> route = new ArrayDeque<>();
    private Vec3 previousPosition;
    private boolean pressingForward;
    private boolean breaking;
    private boolean openPickerOnTick;

    public AutoMinerModule() { super("Auto Miner"); }

    public Item selectedItem() { return selectedItem; }
    public boolean running() { return running; }
    public String status() { return status; }
    public int mined() { return mined; }

    @Override public void onEnable() {
        // Config.load() may enable modules before player/world exist.
        Minecraft c = Minecraft.getInstance();
        stop(c, false);
        choosing = c.player != null && c.level != null;
        openPickerOnTick = choosing;
    }

    public void select(Item item) {
        if (!(item instanceof BlockItem blockItem)) {
            status = "Choose a placeable block item";
            return;
        }
        selectedItem = item;
        selectedBlock = blockItem.getBlock();
        status = "Ready: " + item.getName(new net.minecraft.world.item.ItemStack(item))
            .getString();
    }

    /** Called from the picker only after the user selects a block and taps Start. */
    public boolean start(Minecraft c) {
        if (selectedBlock == null || selectedItem == null) {
            status = "Select a block first";
            return false;
        }
        if (c.player == null || c.level == null || c.gameMode == null) {
            status = "Join a world to start";
            return false;
        }
        if (conflicting()) {
            status = "Disable Freecam, Auto Walk and Auto Clicker";
            return false;
        }
        if (!c.player.isAlive() || c.player.isInLava()) {
            status = "Not safe to begin mining";
            return false;
        }
        stop(c, false);
        world = c.level;
        anchor = c.player.blockPosition().immutable();
        running = true;
        choosing = false;
        openPickerOnTick = false;
        status = "Looking for " + selectedItem.getName(new net.minecraft.world.item.ItemStack(selectedItem)).getString();
        mined = 0;
        ticks = 0;
        lastScan = SCAN_INTERVAL;
        return true;
    }

    private static boolean conflicting() {
        return DashClient.isEnabled("Freecam")
            || DashClient.isEnabled("Auto Walk")
            || DashClient.isEnabled("Auto Clicker");
    }

    /** Stop key/button leaves the module enabled, so the picker may be reopened. */
    public void stop(Minecraft c, boolean userRequested) {
        releaseMovement(c);
        stopBreaking(c);
        running = false;
        route.clear();
        target = null;
        previousPosition = null;
        targetTicks = 0;
        walkingStall = 0;
        if (userRequested) status = "Stopped. Mined " + mined + " blocks";
    }

    @Override public void onDisable() {
        choosing = false;
        openPickerOnTick = false;
        stop(Minecraft.getInstance(), true);
    }

    public void openPicker() {
        Minecraft c = Minecraft.getInstance();
        if (c.player != null) {
            choosing = true;
            openPickerOnTick = true;
        }
    }

    @Override public void onTick(Minecraft c) {
        if (openPickerOnTick && c.player != null && c.level != null) {
            openPickerOnTick = false;
            choosing = false;
            c.gui.setScreen(new AutoMinerScreen(c.gui.screen(), this));
            releaseMovement(c);
            stopBreaking(c);
            return;
        }
        if (!running) return;
        if (c.level == null || c.player == null || c.gameMode == null
                || c.level != world || !c.player.isAlive()
                || c.player.isInLava() || c.player.getHealth() <= 8f
                || c.player.getFoodData().getFoodLevel() <= 5
                || conflicting() || ticks++ > MAX_TOTAL_TICKS) {
            status = "Paused for safety / world change";
            stop(c, false);
            return;
        }
        if (c.gui.screen() != null) {
            releaseMovement(c);
            stopBreaking(c);
            return;
        }
        if (!c.player.onGround()) {
            // No walking over gaps or cliffs, and no air mining while falling.
            releaseMovement(c);
            stopBreaking(c);
            return;
        }
        if (target != null && !matches(c, target)) {
            // Mining completion: the selected block became something else.
            mined++;
            target = null;
            targetTicks = 0;
            route.clear();
            stopBreaking(c);
        }

        if (target == null && ++lastScan >= SCAN_INTERVAL) {
            lastScan = 0;
            plan(c);
        }
        if (target == null) {
            releaseMovement(c);
            stopBreaking(c);
            return;
        }

        if (++targetTicks > TARGET_STALL_TICKS) {
            // Stuck mining or behind a protected block; try a new route.
            status = "Target stalled; searching";
            target = null;
            route.clear();
            targetTicks = 0;
            lastScan = SCAN_INTERVAL;
            releaseMovement(c);
            stopBreaking(c);
            return;
        }

        if (canReach(c, c.player.getEyePosition(), target)) {
            releaseMovement(c);
            mine(c);
            return;
        }

        stopBreaking(c);
        if (route.isEmpty()) {
            target = null;
            lastScan = SCAN_INTERVAL;
            status = "No safe route to target";
            return;
        }
        walk(c);
    }

    private boolean matches(Minecraft c, BlockPos pos) {
        return c.level.hasChunkAt(pos) && c.level.getBlockState(pos).is(selectedBlock);
    }

    private static double distanceSquared(Vec3 from, BlockPos pos) {
        return from.distanceToSqr(Vec3.atCenterOf(pos));
    }

    private boolean canReach(Minecraft c, Vec3 eye, BlockPos pos) {
        if (distanceSquared(eye, pos) > REACH_SQUARED) return false;
        BlockHitResult result = c.level.clip(new ClipContext(eye,
            Vec3.atCenterOf(pos), ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE, c.player));
        return result.getBlockPos().equals(pos);
    }

    private void mine(Minecraft c) {
        if (!matches(c, target)) {
            stopBreaking(c);
            return;
        }
        Vec3 eye = c.player.getEyePosition();
        Vec3 aim = Vec3.atCenterOf(target);
        pointAt(c, eye, aim);
        BlockHitResult result = c.level.clip(new ClipContext(eye, aim,
            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, c.player));
        if (!result.getBlockPos().equals(target)) return;
        Direction face = result.getDirection();
        if (!breaking) {
            breaking = c.gameMode.startDestroyBlock(target, face);
        } else {
            c.gameMode.continueDestroyBlock(target, face);
        }
        status = "Mining " + target.getX() + " " + target.getY() + " " + target.getZ();
    }

    private static void pointAt(Minecraft c, Vec3 eye, Vec3 point) {
        double dx = point.x - eye.x;
        double dy = point.y - eye.y;
        double dz = point.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        c.player.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
        c.player.setXRot((float) -Math.toDegrees(Math.atan2(dy, horizontal)));
    }

    private void walk(Minecraft c) {
        BlockPos next = route.peek();
        if (next == null) return;
        double x = next.getX() + 0.5;
        double z = next.getZ() + 0.5;
        Vec3 at = c.player.position();
        double dx = x - at.x;
        double dz = z - at.z;
        if (dx * dx + dz * dz < 0.12) {
            route.poll();
            walkingStall = 0;
            previousPosition = null;
            releaseMovement(c);
            return;
        }
        BlockPos feet = c.player.blockPosition();
        if (feet.getY() != anchor.getY() || !safeToStand(c, next)) {
            status = "Route blocked";
            target = null;
            route.clear();
            releaseMovement(c);
            return;
        }
        pointAt(c, c.player.getEyePosition(),
            new Vec3(x, c.player.getEyeY(), z));
        c.options.keyUp.setDown(true);
        pressingForward = true;
        if (previousPosition != null
                && previousPosition.distanceToSqr(at) < 0.0016) walkingStall++;
        else walkingStall = 0;
        previousPosition = at;
        if (walkingStall > WALK_STALL_TICKS) {
            status = "Movement blocked; stopping";
            stop(c, false);
        } else status = "Walking to " + target.getX() + " " + target.getY() + " " + target.getZ();
    }

    private void releaseMovement(Minecraft c) {
        if (pressingForward && c.options != null) c.options.keyUp.setDown(false);
        pressingForward = false;
    }

    private void stopBreaking(Minecraft c) {
        if (breaking && c.gameMode != null) c.gameMode.stopDestroyBlock();
        breaking = false;
    }

    /**
     * Same-level BFS. The bot only walks on loaded, solid, nonhazardous
     * flooring with two empty blocks of headroom. It will NOT bridge, swim,
     * jump across pits, or dig toward a hidden target.
     */
    private void plan(Minecraft c) {
        BlockPos start = c.player.blockPosition();
        if (start.getY() != anchor.getY()) {
            status = "Needs flat ground at starting height";
            return;
        }
        Map<BlockPos, BlockPos> parent = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        parent.put(start, start);

        while (!queue.isEmpty() && parent.size() <= MAX_VISITED) {
            BlockPos here = queue.poll();
            for (Direction dir : HORIZONTAL) {
                BlockPos next = here.relative(dir);
                if (parent.containsKey(next)
                        || Math.abs(next.getX() - anchor.getX()) > SEARCH_RADIUS
                        || Math.abs(next.getZ() - anchor.getZ()) > SEARCH_RADIUS
                        || !safeToStand(c, next)) continue;
                parent.put(next, here);
                queue.add(next);
            }
        }

        List<BlockPos> candidates = new ArrayList<>();
        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                BlockPos column = anchor.offset(x, 0, z);
                if (!c.level.hasChunkAt(column)) continue;
                for (int dy = -HEIGHT_RADIUS; dy <= HEIGHT_RADIUS; dy++) {
                    BlockPos pos = column.offset(0, dy, 0);
                    if (matches(c, pos)
                            && c.level.getBlockState(pos).getDestroySpeed(c.level, pos) >= 0)
                        candidates.add(pos);
                }
            }
        }
        candidates.sort(Comparator.comparingDouble(pos ->
            pos.distSqr(c.player.blockPosition())));
        if (candidates.size() > MAX_CANDIDATES)
            candidates = candidates.subList(0, MAX_CANDIDATES);

        double bestScore = Double.POSITIVE_INFINITY;
        BlockPos bestTarget = null, bestStance = null;
        for (BlockPos pos : candidates) {
            for (BlockPos stance : parent.keySet()) {
                if (stance.distSqr(pos) > 17) continue;
                Vec3 hypotheticalEye = new Vec3(
                    stance.getX() + 0.5, stance.getY() + 1.62, stance.getZ() + 0.5);
                if (!canReach(c, hypotheticalEye, pos)) continue;
                double score = stance.distSqr(start) + 0.04 * stance.distSqr(pos);
                if (score < bestScore) {
                    bestScore = score;
                    bestTarget = pos;
                    bestStance = stance;
                }
            }
        }
        if (bestTarget == null) {
            status = "No reachable " + selectedItem.getName(
                new net.minecraft.world.item.ItemStack(selectedItem)).getString()
                + " in " + SEARCH_RADIUS + " blocks";
            return;
        }
        target = bestTarget.immutable();
        targetTicks = 0;
        route.clear();
        BlockPos cursor = bestStance;
        while (cursor != null && !cursor.equals(start)) {
            route.addFirst(cursor);
            cursor = parent.get(cursor);
        }
        walkingStall = 0;
        previousPosition = null;
        status = "Target " + target.getX() + " " + target.getY() + " " + target.getZ();
    }

    private static boolean safeToStand(Minecraft c, BlockPos feet) {
        if (!c.level.hasChunkAt(feet)) return false;
        BlockState atFeet = c.level.getBlockState(feet);
        BlockState atHead = c.level.getBlockState(feet.above());
        BlockState floor = c.level.getBlockState(feet.below());
        return atFeet.isAir() && atHead.isAir()
            && floor.getFluidState().isEmpty()
            && !floor.is(Blocks.MAGMA_BLOCK)
            && !floor.is(Blocks.CACTUS)
            && !floor.getCollisionShape(c.level, feet.below()).isEmpty();
    }

    @Override public String hudText(Minecraft c) {
        if (c.player == null || c.level == null) return null;
        return "Auto Miner: " + (running ? "ON" : "OFF") + " | "
            + mined + " mined | " + status;
    }
}
