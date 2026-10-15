package com.dashclient.modules;

import com.dashclient.AutoMinerScreen;
import com.dashclient.DashClient;
import com.dashclient.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Client-side continuous strip miner with short, safe corridors to nearby
 * selected ores. Uses ordinary vanilla mining input at ordinary reach.
 *
 * No world scanning beyond loaded chunks, packet spoofing, teleports,
 * anti-cheat bypass, shaft digging, bridging, lava crossing or bedrock mining.
 * Stops on user request, disconnect, death, or a hazardous player state.
 */
public final class AutoMinerModule extends Module {
    private static final int ORE_SEARCH_RADIUS = 16;
    private static final int ORE_SEARCH_HEIGHT = 4;
    private static final int PATH_RADIUS = 16;
    private static final int SCAN_INTERVAL = 20;
    private static final int MAX_CANDIDATES = 80;
    private static final int MAX_STANCES = 1300;
    private static final int BREAK_STALL_TICKS = 20 * 20;
    private static final int WALK_STALL_TICKS = 70;
    private static final int BLOCKED_COOLDOWN = 20 * 90;
    private static final double REACH_SQUARED = 15.0;
    private static final Direction[] CARDINALS = {
        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    private record Node(BlockPos pos, int cost) {}
    private record Plan(Map<BlockPos, BlockPos> parent, Map<BlockPos, Integer> cost) {}

    private Item selectedItem;
    private Block selectedBlock;
    private boolean running;
    private boolean openPickerOnTick;
    private ClientLevel world;
    private BlockPos goalOre;
    private BlockPos miningBlock;
    private BlockPos activeRouteTarget;
    private BlockPos originalStart;
    private Direction tunnelDirection = Direction.NORTH;
    private final ArrayDeque<BlockPos> route = new ArrayDeque<>();
    private final Map<BlockPos, Integer> blockedUntil = new HashMap<>();
    private int tickCount;
    private int lastSearch;
    private int miningTicks;
    private int walkingStall;
    private int selectedMined;
    private int tunnelMined;
    private String status = "Select a block to mine";
    private Vec3 previousPosition;
    private boolean pressingForward;
    private boolean breaking;

    public AutoMinerModule() { super("Auto Miner"); }
    public Item selectedItem() { return selectedItem; }
    public boolean running() { return running; }
    public String status() { return status; }
    public int mined() { return selectedMined; }
    public int cleared() { return tunnelMined; }

    @Override public void onEnable() {
        Minecraft c = Minecraft.getInstance();
        stop(c, false);
        if (c.player != null && c.level != null) openPickerOnTick = true;
    }

    @Override public void onDisable() {
        openPickerOnTick = false;
        stop(Minecraft.getInstance(), true);
    }

    public void select(Item item) {
        if (!(item instanceof BlockItem block)) {
            status = "Choose a placeable block item";
            return;
        }
        selectedItem = item;
        selectedBlock = block.getBlock();
        status = "Ready: " + displayName();
    }

    private String displayName() {
        return selectedItem == null ? "blocks" : selectedItem.getName(new ItemStack(selectedItem)).getString();
    }

    public boolean start(Minecraft c) {
        if (selectedBlock == null) { status = "Select a block first"; return false; }
        if (c.player == null || c.level == null || c.gameMode == null) {
            status = "Join a world to start";
            return false;
        }
        if (conflicting()) { status = "Disable Freecam, Auto Walk and Auto Clicker"; return false; }
        if (!c.player.isAlive() || c.player.isInLava()) {
            status = "Not safe to begin mining";
            return false;
        }

        stop(c, false);
        world = c.level;
        originalStart = c.player.blockPosition().immutable();
        tunnelDirection = directionFromYaw(c.player.getYRot());
        running = true;
        openPickerOnTick = false;
        selectedMined = 0;
        tunnelMined = 0;
        tickCount = 0;
        lastSearch = SCAN_INTERVAL;
        status = "Exploring for " + displayName() + " (stop with F7)";
        return true;
    }

    private static Direction directionFromYaw(float yaw) {
        // Minecraft 0 = South, +90 = West, -90 = East.
        int turn = Math.floorMod(Math.round(yaw / 90f), 4);
        return switch (turn) {
            case 0 -> Direction.SOUTH;
            case 1 -> Direction.WEST;
            case 2 -> Direction.NORTH;
            default -> Direction.EAST;
        };
    }

    private static boolean conflicting() {
        return DashClient.isEnabled("Freecam") || DashClient.isEnabled("Auto Walk")
            || DashClient.isEnabled("Auto Clicker");
    }

    /** The user stop control releases both keys and pending break action. */
    public void stop(Minecraft c, boolean userRequested) {
        releaseMovement(c);
        stopBreaking(c);
        running = false;
        route.clear();
        blockedUntil.clear();
        goalOre = null;
        activeRouteTarget = null;
        miningBlock = null;
        previousPosition = null;
        walkingStall = 0;
        miningTicks = 0;
        if (userRequested) status = "Stopped: " + selectedMined
            + " selected blocks, " + tunnelMined + " other blocks";
    }

    public void openPicker() {
        Minecraft c = Minecraft.getInstance();
        if (c.player != null) openPickerOnTick = true;
    }

    @Override public void onTick(Minecraft c) {
        if (openPickerOnTick && c.player != null && c.level != null) {
            openPickerOnTick = false;
            c.gui.setScreen(new AutoMinerScreen(c.gui.screen(), this));
            releaseMovement(c);
            stopBreaking(c);
            return;
        }
        if (!running) return;
        if (c.player == null || c.level == null || c.gameMode == null
                || c.level != world || !c.player.isAlive()
                || c.player.isInLava() || c.player.getHealth() <= 8f
                || c.player.getFoodData().getFoodLevel() <= 5 || conflicting()) {
            stop(c, false);
            status = "Stopped: safety or world changed";
            return;
        }
        tickCount++;
        if (tickCount % 200 == 0) blockedUntil.entrySet().removeIf(e -> e.getValue() < tickCount);
        if (c.gui.screen() != null || !c.player.onGround()) {
            releaseMovement(c);
            stopBreaking(c);
            return;
        }

        // A break may finish in the same tick. Keep working rather than
        // halting after a few blocks or counting a whole tunnel as the goal.
        if (miningBlock != null) {
            if (c.level.getBlockState(miningBlock).isAir()) {
                if (matches(c, miningBlock)) selectedMined++;
                else if (miningSelectedBlock) selectedMined++;
                else tunnelMined++;
                miningBlock = null;
                miningSelectedBlock = false;
                miningTicks = 0;
                stopBreaking(c);
                releaseMovement(c);
                if (goalOre != null && !matches(c, goalOre)) finishOreGoal();
                return;
            }
            if (++miningTicks > BREAK_STALL_TICKS || !breakAt(c, miningBlock)) {
                markBlocked(miningBlock);
                abandonRoute(c);
                status = "Blocked block skipped; looking for another route";
            }
            return;
        }

        if (goalOre != null && !matches(c, goalOre)) finishOreGoal();

        // A scanned ore takes precedence over digging the next strip-mining
        // tunnel segment; scanning only already-received blocks.
        if (++lastSearch >= SCAN_INTERVAL && (goalOre == null || route.isEmpty())) {
            lastSearch = 0;
            if (planOre(c)) return;
        }

        if (goalOre != null && canReach(c, c.player.getEyePosition(), goalOre)) {
            startBreaking(c, goalOre);
            return;
        }

        if (route.isEmpty()) {
            if (goalOre != null) {
                // At the destination, but the ore remains behind stone.
                // Mine the first intervening block using a normal raycast.
                BlockPos obstacle = obstructionTo(c, goalOre);
                if (obstacle != null && !obstacle.equals(goalOre) && safeToBreak(c, obstacle)) {
                    startBreaking(c, obstacle);
                    return;
                }
                // If unobstructed, target can be reached next tick;
                // otherwise avoid inaccessible/unsafe targets for a while.
                if (obstacle == null || !canReach(c, c.player.getEyePosition(), goalOre)) {
                    markBlocked(goalOre);
                    finishOreGoal();
                }
            }
            planExplore(c);
        }

        BlockPos step = route.peek();
        if (step == null) {
            releaseMovement(c);
            return; // Wait for safe unloaded chunks, never force loading.
        }
        if (!canMakeCorridor(c, step)) {
            markBlocked(step);
            abandonRoute(c);
            status = "Unsafe path; finding another direction";
            return;
        }
        BlockPos obstacle = corridorObstacle(c, step);
        if (obstacle != null) {
            if (safeToBreak(c, obstacle)) startBreaking(c, obstacle);
            else {
                markBlocked(step);
                abandonRoute(c);
            }
            return;
        }
        walk(c, step);
    }

    private boolean miningSelectedBlock;

    private void startBreaking(Minecraft c, BlockPos pos) {
        if (!safeToBreak(c, pos) || !canReach(c, c.player.getEyePosition(), pos)) {
            markBlocked(pos);
            abandonRoute(c);
            return;
        }
        releaseMovement(c);
        miningBlock = pos.immutable();
        miningSelectedBlock = matches(c, pos);
        miningTicks = 0;
        breaking = false;
        breakAt(c, pos);
    }

    private boolean breakAt(Minecraft c, BlockPos pos) {
        if (c.level.getBlockState(pos).isAir()) return true;
        if (!safeToBreak(c, pos)) return false;
        Vec3 eye = c.player.getEyePosition();
        Vec3 aim = Vec3.atCenterOf(pos);
        if (eye.distanceToSqr(aim) > REACH_SQUARED) return false;
        BlockHitResult hit = c.level.clip(new ClipContext(eye, aim,
            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, c.player));
        if (!hit.getBlockPos().equals(pos)) return false;
        pointAt(c, eye, aim);
        if (!breaking) {
            // startDestroyBlock's return value is not "is breaking".
            // On hard blocks it may be false even though breaking started.
            c.gameMode.startDestroyBlock(pos, hit.getDirection());
            breaking = true;
        } else {
            c.gameMode.continueDestroyBlock(pos, hit.getDirection());
        }
        status = "Mining " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
        return true;
    }

    private void markBlocked(BlockPos pos) {
        if (pos != null) blockedUntil.put(pos.immutable(), tickCount + BLOCKED_COOLDOWN);
        if (blockedUntil.size() > 512) blockedUntil.clear();
    }

    private boolean blocked(BlockPos pos) {
        return blockedUntil.getOrDefault(pos, 0) > tickCount;
    }

    private void finishOreGoal() {
        goalOre = null;
        route.clear();
        activeRouteTarget = null;
        lastSearch = SCAN_INTERVAL;
    }

    private void abandonRoute(Minecraft c) {
        stopBreaking(c);
        releaseMovement(c);
        miningBlock = null;
        miningSelectedBlock = false;
        miningTicks = 0;
        route.clear();
        goalOre = null;
        activeRouteTarget = null;
        previousPosition = null;
        walkingStall = 0;
        lastSearch = SCAN_INTERVAL;
    }

    private boolean matches(Minecraft c, BlockPos pos) {
        if (c.level == null || !c.level.hasChunkAt(pos) || selectedBlock == null) return false;
        Block block = c.level.getBlockState(pos).getBlock();
        if (block == selectedBlock) return true;
        return (selectedBlock == Blocks.DIAMOND_ORE || selectedBlock == Blocks.DEEPSLATE_DIAMOND_ORE)
                && (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE)
            || (selectedBlock == Blocks.EMERALD_ORE || selectedBlock == Blocks.DEEPSLATE_EMERALD_ORE)
                && (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE)
            || (selectedBlock == Blocks.IRON_ORE || selectedBlock == Blocks.DEEPSLATE_IRON_ORE)
                && (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE)
            || (selectedBlock == Blocks.GOLD_ORE || selectedBlock == Blocks.DEEPSLATE_GOLD_ORE)
                && (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE)
            || (selectedBlock == Blocks.COPPER_ORE || selectedBlock == Blocks.DEEPSLATE_COPPER_ORE)
                && (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE)
            || (selectedBlock == Blocks.COAL_ORE || selectedBlock == Blocks.DEEPSLATE_COAL_ORE)
                && (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE)
            || (selectedBlock == Blocks.LAPIS_ORE || selectedBlock == Blocks.DEEPSLATE_LAPIS_ORE)
                && (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE)
            || (selectedBlock == Blocks.REDSTONE_ORE || selectedBlock == Blocks.DEEPSLATE_REDSTONE_ORE)
                && (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE);
    }

    private boolean canReach(Minecraft c, Vec3 eye, BlockPos pos) {
        if (eye.distanceToSqr(Vec3.atCenterOf(pos)) > REACH_SQUARED) return false;
        BlockHitResult hit = c.level.clip(new ClipContext(eye, Vec3.atCenterOf(pos),
            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, c.player));
        return hit.getBlockPos().equals(pos);
    }

    /** Find nearest opaque block obscuring an ore, never mining by position alone. */
    private BlockPos obstructionTo(Minecraft c, BlockPos pos) {
        Vec3 eye = c.player.getEyePosition();
        if (eye.distanceToSqr(Vec3.atCenterOf(pos)) > REACH_SQUARED) return null;
        return c.level.clip(new ClipContext(eye, Vec3.atCenterOf(pos),
            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, c.player)).getBlockPos();
    }

    private boolean planOre(Minecraft c) {
        BlockPos center = c.player.blockPosition();
        List<BlockPos> found = new ArrayList<>();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int dx = -ORE_SEARCH_RADIUS; dx <= ORE_SEARCH_RADIUS; dx++) {
            for (int dz = -ORE_SEARCH_RADIUS; dz <= ORE_SEARCH_RADIUS; dz++) {
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                mutable.set(x, center.getY(), z);
                if (!c.level.hasChunkAt(mutable)) continue;
                for (int dy = -ORE_SEARCH_HEIGHT; dy <= ORE_SEARCH_HEIGHT; dy++) {
                    mutable.set(x, center.getY() + dy, z);
                    if (matches(c, mutable) && !blocked(mutable))
                        found.add(mutable.immutable());
                }
            }
        }
        if (found.isEmpty()) return false;
        found.sort(Comparator.comparingDouble(p -> p.distSqr(center)));
        if (found.size() > MAX_CANDIDATES)
            found = found.subList(0, MAX_CANDIDATES);

        Plan paths = findPaths(c, center);
        double bestScore = Double.POSITIVE_INFINITY;
        BlockPos bestOre = null;
        BlockPos bestStance = null;
        Vec3 playerEye = c.player.getEyePosition();

        for (BlockPos ore : found) {
            if (canReach(c, playerEye, ore) && safeToBreak(c, ore)) {
                goalOre = ore;
                route.clear();
                status = "Found " + displayName();
                return true;
            }
            if (!safeToBreak(c, ore)) continue;
            for (var entry : paths.cost().entrySet()) {
                BlockPos stance = entry.getKey();
                double distance = new Vec3(stance.getX() + 0.5,
                    stance.getY() + 1.62, stance.getZ() + 0.5)
                    .distanceToSqr(Vec3.atCenterOf(ore));
                if (distance > REACH_SQUARED || distance < 0.1) continue;
                // Prefer fewer excavated blocks, then nearer ores. A stance
                // can be solid *now*, since the corridor will clear it.
                double score = entry.getValue() * 10.0 + distance
                    + 0.05 * ore.distSqr(center);
                if (score < bestScore) {
                    bestScore = score;
                    bestOre = ore;
                    bestStance = stance;
                }
            }
        }
        if (bestOre == null) return false;
        goalOre = bestOre;
        buildRoute(paths.parent(), center, bestStance);
        status = "Tunneling toward " + displayName();
        return true;
    }

    private Plan findPaths(Minecraft c, BlockPos origin) {
        Map<BlockPos, BlockPos> parent = new HashMap<>();
        Map<BlockPos, Integer> costs = new HashMap<>();
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(Node::cost));
        parent.put(origin, origin);
        costs.put(origin, 0);
        queue.add(new Node(origin, 0));
        int visited = 0;
        while (!queue.isEmpty() && visited++ < MAX_STANCES) {
            Node current = queue.poll();
            if (current.cost() != costs.getOrDefault(current.pos(), Integer.MAX_VALUE))
                continue;
            for (Direction direction : CARDINALS) {
                BlockPos next = current.pos().relative(direction);
                if (Math.abs(next.getX() - origin.getX()) > PATH_RADIUS
                        || Math.abs(next.getZ() - origin.getZ()) > PATH_RADIUS
                        || blocked(next) || !canMakeCorridor(c, next)) continue;
                int digCost = (c.level.getBlockState(next).isAir() ? 0 : 4)
                    + (c.level.getBlockState(next.above()).isAir() ? 0 : 4);
                int cost = current.cost() + 1 + digCost;
                if (cost >= costs.getOrDefault(next, Integer.MAX_VALUE)) continue;
                costs.put(next, cost);
                parent.put(next, current.pos());
                queue.add(new Node(next, cost));
            }
        }
        return new Plan(parent, costs);
    }

    private void buildRoute(Map<BlockPos, BlockPos> parent, BlockPos start, BlockPos destination) {
        route.clear();
        if (destination == null) return;
        BlockPos cursor = destination;
        int guard = 0;
        while (!cursor.equals(start) && guard++ < MAX_STANCES) {
            route.addFirst(cursor);
            cursor = parent.get(cursor);
            if (cursor == null) { route.clear(); break; }
        }
        activeRouteTarget = destination;
        previousPosition = null;
        walkingStall = 0;
    }

    /**
     * Continuous exploration when the selected ore isn't in range: advance
     * one corridor square, clearing foot and head blocks before walking.
     * As new loaded terrain enters range, the ore planner diverts to it.
     */
    private void planExplore(Minecraft c) {
        BlockPos feet = c.player.blockPosition();
        Direction[] options = {
            tunnelDirection,
            tunnelDirection.getClockWise(),
            tunnelDirection.getCounterClockWise(),
            tunnelDirection.getOpposite()
        };
        for (Direction direction : options) {
            BlockPos next = feet.relative(direction);
            if (blocked(next) || !canMakeCorridor(c, next)) continue;
            tunnelDirection = direction;
            route.add(next);
            activeRouteTarget = next;
            status = "Searching for " + displayName() + " - mining ahead";
            return;
        }
        status = "No safe direction; waiting for terrain or manual stop";
    }

    private boolean canMakeCorridor(Minecraft c, BlockPos feet) {
        if (!c.level.hasChunkAt(feet) || !c.level.hasChunkAt(feet.above())
                || !c.level.hasChunkAt(feet.below())) return false;
        BlockPos floorPos = feet.below();
        BlockState floor = c.level.getBlockState(floorPos);
        if (floor.getFluidState().isEmpty() == false || floor.isAir()
                || floor.is(Blocks.MAGMA_BLOCK) || floor.is(Blocks.CACTUS)
                || floor.is(Blocks.POWDER_SNOW)
                || floor.getCollisionShape(c.level, floorPos).isEmpty()) return false;
        return (c.level.getBlockState(feet).isAir() || safeToBreak(c, feet))
            && (c.level.getBlockState(feet.above()).isAir() || safeToBreak(c, feet.above()));
    }

    private BlockPos corridorObstacle(Minecraft c, BlockPos feet) {
        if (!c.level.getBlockState(feet.above()).isAir()) return feet.above();
        if (!c.level.getBlockState(feet).isAir()) return feet;
        return null;
    }

    private boolean safeToBreak(Minecraft c, BlockPos pos) {
        if (c.level == null || !c.level.hasChunkAt(pos) || blocked(pos)) return false;
        BlockState state = c.level.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty()
                || state.getDestroySpeed(c.level, pos) < 0
                || c.level.getBlockEntity(pos) != null
                || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.CACTUS)
                || state.is(Blocks.MAGMA_BLOCK)) return false;
        // Do not mine the floor under the player or open a drop directly below.
        BlockPos player = c.player.blockPosition();
        if (pos.getX() == player.getX() && pos.getZ() == player.getZ()
                && pos.getY() < player.getY()) return false;
        // Don't open a fluid pocket or trigger unsupported gravity blocks.
        for (Direction d : Direction.values()) {
            BlockPos neighbor = pos.relative(d);
            if (!c.level.hasChunkAt(neighbor)) return false;
            if (!c.level.getFluidState(neighbor).isEmpty()) return false;
        }
        BlockState above = c.level.getBlockState(pos.above());
        if (above.is(Blocks.GRAVEL) || above.is(Blocks.SAND)
                || above.is(Blocks.RED_SAND)) return false;
        return true;
    }

    private void walk(Minecraft c, BlockPos next) {
        Vec3 pos = c.player.position();
        double x = next.getX() + 0.5;
        double z = next.getZ() + 0.5;
        double dx = x - pos.x;
        double dz = z - pos.z;
        if (dx * dx + dz * dz < 0.12) {
            route.poll();
            previousPosition = null;
            walkingStall = 0;
            releaseMovement(c);
            return;
        }
        if (next.getY() != c.player.blockPosition().getY()) {
            markBlocked(next);
            abandonRoute(c);
            return;
        }
        pointAt(c, c.player.getEyePosition(), new Vec3(x, c.player.getEyeY(), z));
        c.options.keyUp.setDown(true);
        pressingForward = true;
        if (previousPosition != null && previousPosition.distanceToSqr(pos) < 0.0016)
            walkingStall++;
        else walkingStall = 0;
        previousPosition = pos;
        if (walkingStall > WALK_STALL_TICKS) {
            markBlocked(next);
            abandonRoute(c);
            status = "Could not walk forward; trying another path";
        }
    }

    private static void pointAt(Minecraft c, Vec3 eye, Vec3 point) {
        double dx = point.x - eye.x;
        double dy = point.y - eye.y;
        double dz = point.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        c.player.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
        c.player.setXRot((float) -Math.toDegrees(Math.atan2(dy, horizontal)));
    }

    private void releaseMovement(Minecraft c) {
        if (pressingForward && c.options != null) c.options.keyUp.setDown(false);
        pressingForward = false;
    }

    private void stopBreaking(Minecraft c) {
        if (breaking && c.gameMode != null) c.gameMode.stopDestroyBlock();
        breaking = false;
    }

    @Override public String hudText(Minecraft c) {
        if (c.player == null || c.level == null) return null;
        return "Auto Miner: " + (running ? "ON" : "OFF") + " | "
            + selectedMined + " targets / " + tunnelMined + " cleared | " + status;
    }
}
