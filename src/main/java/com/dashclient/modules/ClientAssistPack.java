package com.dashclient.modules;

import com.dashclient.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.AABB;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Extra opt-in combat and movement utilities. All target checks are client-side
 * and limited to already loaded, visible entities and normal vanilla reach.
 * No anti-cheat bypass, spoofed packets or hidden chunk retrieval.
 */
public final class ClientAssistPack {
    private ClientAssistPack() {}

    private static void add(List<Module> list, String name, Function<Minecraft,String> fn) {
        list.add(new Module(name) {
            @Override public String hudText(Minecraft c) { return fn.apply(c); }
        });
    }
    private static List<LivingEntity> nearby(Minecraft c, double range) {
        if (c.player == null || c.level == null) return List.of();
        return c.level.getEntitiesOfClass(LivingEntity.class,
            c.player.getBoundingBox().inflate(range),
            e -> e != c.player && e.isAlive() && c.player.distanceTo(e) <= range);
    }
    private static LivingEntity aimed(Minecraft c) {
        if (!(c.hitResult instanceof EntityHitResult result)) return null;
        return result.getEntity() instanceof LivingEntity living && living.isAlive() ? living : null;
    }
    private static Player nearestPlayer(Minecraft c, double range) {
        Player best = null;
        double distance = Double.POSITIVE_INFINITY;
        for (LivingEntity living : nearby(c, range)) {
            if (living instanceof Player p && c.player.distanceTo(p) < distance) {
                best = p;
                distance = c.player.distanceTo(p);
            }
        }
        return best;
    }
    private static String number(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    public static void register(List<Module> list) {
        list.add(new TriggerBot());
        list.add(new KillAura());
        list.add(new AutoJump());
        list.add(new AutoWalk());

        add(list, "Combat Reach Meter", c -> {
            LivingEntity e = aimed(c);
            return c.player == null || e == null ? null :
                "Reach: " + number(c.player.distanceTo(e)) + " / 3.0 blocks";
        });
        add(list, "Target Armor Value", c -> {
            LivingEntity e = aimed(c);
            return e == null ? null : "Target armor: " + e.getArmorValue();
        });
        add(list, "Target Absorption", c -> {
            LivingEntity e = aimed(c);
            return e == null ? null : "Target absorption: " + number(e.getAbsorptionAmount());
        });
        add(list, "Target On Ground", c -> {
            LivingEntity e = aimed(c);
            return e == null ? null : "Target grounded: " + (e.onGround() ? "Yes" : "No");
        });
        add(list, "Target Sprinting", c -> {
            LivingEntity e = aimed(c);
            return e == null ? null : "Target sprinting: " + (e.isSprinting() ? "Yes" : "No");
        });
        add(list, "Target Crouching", c -> {
            LivingEntity e = aimed(c);
            return e == null ? null : "Target crouching: " + (e.isShiftKeyDown() ? "Yes" : "No");
        });
        add(list, "Target Burning", c -> {
            LivingEntity e = aimed(c);
            return e == null ? null : "Target burning: " + (e.isOnFire() ? "Yes" : "No");
        });
        add(list, "Target Glowing", c -> {
            LivingEntity e = aimed(c);
            return e == null ? null : "Target glowing: " + (e.isCurrentlyGlowing() ? "Yes" : "No");
        });
        add(list, "Target Effects Count", c -> {
            LivingEntity e = aimed(c);
            return e == null ? null : "Target effects: " + e.getActiveEffects().size();
        });
        add(list, "Nearest PvP Health", c -> {
            Player p = nearestPlayer(c, 32);
            return p == null ? null : "Nearby player HP: " + p.getName().getString()
                + " " + number(p.getHealth());
        });
        add(list, "Nearest PvP Armor", c -> {
            Player p = nearestPlayer(c, 32);
            return p == null ? null : "Nearby player armor: " + p.getArmorValue();
        });
        add(list, "Player Compass", c -> {
            Player p = nearestPlayer(c, 48);
            if (p == null) return "Player compass: None loaded";
            double dx = p.getX() - c.player.getX();
            double dz = p.getZ() - c.player.getZ();
            double bearing = (Math.toDegrees(Math.atan2(-dx, dz)) + 360.0) % 360.0;
            return "Player compass: " + p.getName().getString() + " " + number(bearing) + "°";
        });
        add(list, "Nearest PvP Proximity", c -> {
            Player p = nearestPlayer(c, 32);
            return p == null ? "PvP proximity: Clear" :
                "PvP proximity: " + number(c.player.distanceTo(p)) + " blocks";
        });
        add(list, "Players Within 8", c -> "Players 8m: " +
            nearby(c, 8).stream().filter(e -> e instanceof Player).count());
        add(list, "Players Within 16", c -> "Players 16m: " +
            nearby(c, 16).stream().filter(e -> e instanceof Player).count());
        add(list, "Players Within 32", c -> "Players 32m: " +
            nearby(c, 32).stream().filter(e -> e instanceof Player).count());
        add(list, "Mobs Within 8", c -> "Mobs 8m: " +
            nearby(c, 8).stream().filter(e -> !(e instanceof Player)).count());
        add(list, "Mobs Within 16", c -> "Mobs 16m: " +
            nearby(c, 16).stream().filter(e -> !(e instanceof Player)).count());
        add(list, "Mobs Within 32", c -> "Mobs 32m: " +
            nearby(c, 32).stream().filter(e -> !(e instanceof Player)).count());
        add(list, "Vertical Motion", c -> c.player == null ? null :
            "Vertical motion: " + number(c.player.getDeltaMovement().y * 20) + " b/s");
        add(list, "Total Motion", c -> c.player == null ? null :
            "Total motion: " + number(c.player.getDeltaMovement().length() * 20) + " b/s");
        add(list, "Sprint State", c -> c.player == null ? null :
            "Sprinting: " + (c.player.isSprinting() ? "Yes" : "No"));
        add(list, "Crouch State", c -> c.player == null ? null :
            "Crouching: " + (c.player.isShiftKeyDown() ? "Yes" : "No"));
        add(list, "Ground Contact", c -> c.player == null ? null :
            "Ground contact: " + (c.player.onGround() ? "Yes" : "No"));
    }

    private static boolean canAttack(Minecraft c) {
        return c.player != null && c.player.isAlive() && c.level != null
            && c.gameMode != null && c.gui.screen() == null
            && c.player.getAttackStrengthScale(0f) >= 0.95f;
    }
    private static boolean inVanillaRange(Minecraft c, LivingEntity e) {
        return c.player.distanceTo(e) <= 3.0 && c.player.hasLineOfSight(e);
    }
    private static void attack(Minecraft c, LivingEntity e) {
        c.gameMode.attack(c.player, e);
    }

    /** Attacks the crosshair target when normal attack cooldown is ready. */
    public static final class TriggerBot extends Module {
        private int ticks;
        public TriggerBot() { super("TriggerBot"); }
        @Override public void onTick(Minecraft c) {
            if (!canAttack(c)) { ticks=0; return; }
            if (++ticks < 5) return;
            ticks=0;
            LivingEntity target = aimed(c);
            if (target != null && inVanillaRange(c, target)) attack(c, target);
        }
        @Override public void onDisable() { ticks=0; }
        @Override public String hudText(Minecraft c) { return "TriggerBot: normal melee range"; }
    }
    /** Attacks the closest visible entity at vanilla melee range. */
    public static final class KillAura extends Module {
        private int ticks;
        public KillAura() { super("Kill Aura"); }
        @Override public void onTick(Minecraft c) {
            if (!canAttack(c)) { ticks=0; return; }
            if (++ticks < 5) return;
            ticks=0;
            LivingEntity closest=null;
            double best=Double.POSITIVE_INFINITY;
            for (LivingEntity e : nearby(c, 3)) {
                if (!inVanillaRange(c, e)) continue;
                double dist = c.player.distanceTo(e);
                if (dist < best) { best = dist; closest = e; }
            }
            if (closest != null) attack(c, closest);
        }
        @Override public void onDisable() { ticks=0; }
        @Override public String hudText(Minecraft c) { return "Kill Aura: visible targets <= 3m"; }
    }
    public static final class AutoJump extends Module {
        public AutoJump() { super("Auto Jump"); }
        @Override public void onTick(Minecraft c) {
            if (c.player == null || c.gui.screen() != null) return;
            if (c.player.onGround() &&
                c.player.getDeltaMovement().horizontalDistanceSqr() > 0.02)
                c.player.jumpFromGround();
        }
        @Override public String hudText(Minecraft c) { return "Auto Jump: when moving"; }
    }
    public static final class AutoWalk extends Module {
        private boolean pressing;
        public AutoWalk() { super("Auto Walk"); }
        @Override public void onTick(Minecraft c) {
            if (c.options == null) return;
            boolean move = c.player != null && c.level != null && c.gui.screen() == null;
            if (move) { c.options.keyUp.setDown(true); pressing=true; }
            else if (pressing) { c.options.keyUp.setDown(false); pressing=false; }
        }
        @Override public void onDisable() {
            if (pressing) Minecraft.getInstance().options.keyUp.setDown(false);
            pressing = false;
        }
        @Override public String hudText(Minecraft c) { return "Auto Walk: forward"; }
    }
}
