package com.dashclient.modules;

import com.dashclient.DashClient;
import com.dashclient.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Local visuals and experimental gameplay utilities. No anti-cheat evasion,
 * chunk fabrication, reach extension or custom movement packets.
 */
public final class CombatVisualPack {
    private CombatVisualPack() {}
    private static void add(List<Module> list, String name, Function<Minecraft,String> text) {
        list.add(new Module(name) {
            @Override public String hudText(Minecraft c) { return text.apply(c); }
        });
    }

    private static List<LivingEntity> nearby(Minecraft c) {
        if (c.player == null || c.level == null) return List.of();
        AABB area = c.player.getBoundingBox().inflate(48.0);
        return c.level.getEntitiesOfClass(LivingEntity.class, area,
            e -> e.isAlive() && e != c.player);
    }

    public static void register(List<Module> list) {
        list.add(new Fly());
        list.add(new ExtendedView());
        list.add(new AutoClickerModule());
        list.add(new GlowEsp("Player ESP", true));
        list.add(new GlowEsp("Entity ESP", false));

        // Readouts based solely on entities and positions the server already sent.
        add(list, "Player Radar", c -> {
            int n=0;
            for (LivingEntity e : nearby(c)) if (e instanceof Player) n++;
            return "Nearby players: " + n;
        });
        add(list, "Mob Radar", c -> {
            int n=0;
            for (LivingEntity e : nearby(c)) if (!(e instanceof Player)) n++;
            return "Loaded mobs: " + n;
        });
        add(list, "Nearest Player", c -> {
            if (c.player == null) return null;
            Player closest = null;
            double best = Double.POSITIVE_INFINITY;
            for (LivingEntity e : nearby(c)) {
                if (!(e instanceof Player player)) continue;
                double d = c.player.distanceTo(player);
                if (d < best) { best=d; closest=player; }
            }
            return closest == null ? "Nearest player: None" :
                String.format(Locale.ROOT, "Nearest player: %s (%.0fm)",
                    closest.getName().getString(), best);
        });
        add(list, "Nearest Mob", c -> {
            if (c.player == null) return null;
            LivingEntity closest = null;
            double best = Double.POSITIVE_INFINITY;
            for (LivingEntity e : nearby(c)) {
                if (e instanceof Player) continue;
                double d = c.player.distanceTo(e);
                if (d < best) { best=d; closest=e; }
            }
            return closest == null ? "Nearest mob: None" :
                String.format(Locale.ROOT, "Nearest mob: %s (%.0fm)",
                    closest.getType().getDescription().getString(),best);
        });
        add(list, "Target Crosshair", c -> {
            if (!(c.hitResult instanceof EntityHitResult target)) return null;
            return "Looking at: " + target.getEntity().getName().getString();
        });
        add(list, "Target Coordinates", c -> {
            if (!(c.hitResult instanceof EntityHitResult hit)) return null;
            Entity target = hit.getEntity();
            return String.format(Locale.ROOT, "Entity XYZ: %.0f %.0f %.0f",
                target.getX(), target.getY(), target.getZ());
        });
        add(list, "Target Velocity", c -> {
            if (!(c.hitResult instanceof EntityHitResult hit)) return null;
            return String.format(Locale.ROOT, "Entity speed: %.1f b/s",
                hit.getEntity().getDeltaMovement().horizontalDistance()*20);
        });
        add(list, "Critical Ready", c -> c.player == null ? null :
            "Crit setup: " + (c.player.fallDistance > 0 && !c.player.onGround()
                && !c.player.isInWater() ? "Falling" : "Not falling"));
        add(list, "Attack Cooldown", c -> c.player == null ? null :
            String.format(Locale.ROOT, "Attack cooldown: %.0f%%",
                c.player.getAttackStrengthScale(0f)*100));
        add(list, "Player Flight State", c -> c.player == null ? null :
            "Flight: " + (c.player.getAbilities().flying ? "On" : "Off"));
        add(list, "Flight Permission", c -> c.player == null ? null :
            "May fly: " + (c.player.getAbilities().mayfly ? "Yes" : "No"));
        add(list, "Fall Distance", c -> c.player == null ? null :
            String.format(Locale.ROOT, "Fall: %.1fm", c.player.fallDistance));
        add(list, "Water State", c -> c.player == null ? null :
            "Water: " + (c.player.isInWater() ? "Yes" : "No"));
        add(list, "Lava State", c -> c.player == null ? null :
            "Lava: " + (c.player.isInLava() ? "Yes" : "No"));
        add(list, "View Limit Reminder", c -> c.getCurrentServer() == null ?
            "View: Local singleplayer" : "View: Server may cap sent chunks");
    }

    /** Creative/spectator or server-authorized flight only. */
    public static final class Fly extends Module {
        private LocalPlayer player;
        private boolean previousMayfly;
        private boolean previousFlying;

        public Fly() { super("Fly"); }

        // Experimental local survival flight. It cannot override server authority.
        @Override public void onTick(Minecraft c) {
            if (c.player == null || c.level == null) {
                restore();
                return;
            }
            if (player != c.player) {
                restore();
                player = c.player;
                previousMayfly = player.getAbilities().mayfly;
                previousFlying = player.getAbilities().flying;
            }
            if (c.gui.screen() != null) return;
            // Uses Minecraft's own movement handling; no evasion or packet tricks.
            c.player.getAbilities().mayfly = true;
            c.player.getAbilities().flying = true;
        }

        @Override public String hudText(Minecraft c) {
            return c.player == null ? null :
                "Fly (experimental): SPACE up / SHIFT down";
        }

        private void restore() {
            if (player != null) {
                player.getAbilities().mayfly = previousMayfly;
                player.getAbilities().flying = previousFlying;
            }
            player = null;
        }
        @Override public void onDisable() { restore(); }
    }

        /** Local chunk radius only; cannot create missing server chunk data. */
    public static final class ExtendedView extends Module {
        private Integer previous;
        public ExtendedView() { super("Extended View"); }
        @Override public void onEnable() {
            Minecraft c = Minecraft.getInstance();
            previous = c.options.renderDistance().get();
            // Respect the player's configured maximum. Extreme preset is turned
            // off so it won't immediately override the render distance.
            for (Module m : DashClient.MODULES) {
                if (m.enabled() && (m.name().equals("Performance Boost") ||
                    m.name().equals("Extreme FPS") || m.name().equals("Battery Saver"))) m.setEnabled(false);
            }
            // Raise local view settings; server view distance is NOT bypassed.
            c.options.renderDistance().set(Math.max(previous, Math.min(32, 24)));
        }
        @Override public String hudText(Minecraft c) {
            return "Local render: " + c.options.renderDistance().get() + " chunks (server capped)";
        }
        @Override public void onDisable() {
            if (previous != null) Minecraft.getInstance().options.renderDistance().set(previous);
            previous=null;
        }
    }

    /** Glow outlines for entities already loaded in the client; no x-ray chunk scan. */
    public static final class GlowEsp extends Module {
        private final boolean playersOnly;
        private final Map<Entity,Boolean> changed = new IdentityHashMap<>();
        private Object world;
        private int ticks;
        public GlowEsp(String name, boolean playersOnly) {
            super(name);
            this.playersOnly=playersOnly;
        }
        @Override public void onTick(Minecraft c) {
            if (world != c.level) {
                changed.clear(); world=c.level; ticks=0;
            }
            if (c.player == null || c.level == null || ++ticks < 20) return;
            ticks=0;
            for (LivingEntity e : nearby(c)) {
                if (playersOnly != (e instanceof Player)) continue;
                if (!changed.containsKey(e)) changed.put(e, e.isCurrentlyGlowing());
                if (!e.isCurrentlyGlowing()) e.setGlowingTag(true);
            }
            // Keep limited memory even when moving through long sessions.
            changed.entrySet().removeIf(entry -> !entry.getKey().isAlive());
        }
        @Override public void onDisable() {
            for (Map.Entry<Entity,Boolean> entry : changed.entrySet()) {
                Entity entity = entry.getKey();
                if (entity.isAlive()) entity.setGlowingTag(entry.getValue());
            }
            changed.clear();
            world=null;
            ticks=0;
        }
        @Override public String hudText(Minecraft c) {
            return name() + ": loaded " + (playersOnly ? "players" : "mobs") + " only";
        }
    }
}
