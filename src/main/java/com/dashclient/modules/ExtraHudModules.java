package com.dashclient.modules;

import com.dashclient.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import java.util.Locale;

// Lightweight informational modules: all local/client HUD, no packets or automation.
public final class ExtraHudModules {
    private ExtraHudModules() {}

    public static final class Ping extends Module {
        public Ping() { super("Ping"); }
        @Override public String hudText(Minecraft c) {
            if (c.player == null || c.getConnection() == null) return null;
            PlayerInfo p = c.getConnection().getPlayerInfo(c.player.getUUID());
            return p == null ? "Ping: --" : "Ping: " + p.getLatency() + " ms";
        }
    }

    public static final class Memory extends Module {
        public Memory() { super("Memory"); }
        @Override public String hudText(Minecraft c) {
            Runtime r = Runtime.getRuntime();
            long used = (r.totalMemory() - r.freeMemory()) / 1048576L;
            return "RAM: " + used + " / " + r.maxMemory() / 1048576L + " MB";
        }
    }

    public static final class Playtime extends Module {
        private final long started = System.nanoTime();
        public Playtime() { super("Client Uptime"); }
        @Override public String hudText(Minecraft c) {
            long sec = (System.nanoTime() - started) / 1000000000L;
            return String.format(Locale.ROOT, "Uptime: %02d:%02d:%02d",
                sec / 3600, (sec / 60) % 60, sec % 60);
        }
    }

    public static final class Keystrokes extends Module {
        public Keystrokes() { super("Keystrokes"); }
        @Override public String hudText(Minecraft c) {
            return "Keys: " + key(c.options.keyUp.isDown(), "W")
                + key(c.options.keyLeft.isDown(), "A")
                + key(c.options.keyDown.isDown(), "S")
                + key(c.options.keyRight.isDown(), "D")
                + " " + key(c.options.keyJump.isDown(), "SPACE")
                + " " + key(c.options.keyAttack.isDown(), "LMB")
                + key(c.options.keyUse.isDown(), "RMB");
        }
        private static String key(boolean down, String letter) {
            return down ? "[" + letter + "]" : " " + letter + " ";
        }
    }

    public static final class Effects extends Module {
        public Effects() { super("Potion Effects"); }
        @Override public String hudText(Minecraft c) {
            if (c.player == null) return null;
            StringBuilder text = new StringBuilder("Effects: ");
            int count = 0;
            for (MobEffectInstance effect : c.player.getActiveEffects()) {
                if (count > 0) text.append(", ");
                text.append(effect.getEffect().value().getDisplayName().getString());
                int seconds = effect.getDuration() / 20;
                if (seconds > 0) text.append(" ").append(seconds).append("s");
                if (++count == 3) break;
            }
            return count == 0 ? "Effects: None" : text.toString();
        }
    }

    public static final class Biome extends Module {
        public Biome() { super("Biome"); }
        @Override public String hudText(Minecraft c) {
            if (c.player == null || c.level == null) return null;
            return "Biome: " + c.level.getBiome(c.player.blockPosition())
                .unwrapKey().map(k -> k.toString()).orElse("Unknown");
        }
    }

    public static final class Chunk extends Module {
        public Chunk() { super("Chunk Position"); }
        @Override public String hudText(Minecraft c) {
            if (c.player == null) return null;
            return "Chunk: " + (c.player.blockPosition().getX() >> 4)
                + ", " + (c.player.blockPosition().getZ() >> 4);
        }
    }

    public static final class Speed extends Module {
        public Speed() { super("Speedometer"); }
        @Override public String hudText(Minecraft c) {
            if (c.player == null) return null;
            return String.format(Locale.ROOT, "Speed: %.2f blocks/s",
                c.player.getDeltaMovement().horizontalDistance() * 20.0);
        }
    }

    public static final class Health extends Module {
        public Health() { super("Health"); }
        @Override public String hudText(Minecraft c) {
            if (c.player == null) return null;
            return String.format(Locale.ROOT, "Health: %.1f / %.1f",
                c.player.getHealth(), c.player.getMaxHealth());
        }
    }

    public static final class Hunger extends Module {
        public Hunger() { super("Hunger"); }
        @Override public String hudText(Minecraft c) {
            return c.player == null ? null :
                "Food: " + c.player.getFoodData().getFoodLevel() + " / 20";
        }
    }

    public static final class Totems extends Module {
        public Totems() { super("Totem Counter"); }
        @Override public String hudText(Minecraft c) {
            if (c.player == null) return null;
            int count = c.player.getInventory().countItem(Items.TOTEM_OF_UNDYING);
            if (c.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) count++;
            return "Totems: " + count;
        }
    }

    public static final class ItemCount extends Module {
        public ItemCount() { super("Item Count"); }
        @Override public String hudText(Minecraft c) {
            if (c.player == null) return null;
            ItemStack held = c.player.getMainHandItem();
            return "Held stack: " + (held.isEmpty() ? 0 : held.getCount());
        }
    }

    public static final class ItemDurability extends Module {
        public ItemDurability() { super("Item Durability"); }
        @Override public String hudText(Minecraft c) {
            if (c.player == null) return null;
            ItemStack held = c.player.getMainHandItem();
            if (held.isEmpty() || !held.isDamageableItem()) return "Durability: N/A";
            return "Durability: " + Math.max(0, held.getMaxDamage() - held.getDamageValue())
                + " / " + held.getMaxDamage();
        }
    }

    public static final class WorldTime extends Module {
        public WorldTime() { super("World Age"); }
        @Override public String hudText(Minecraft c) {
            if (c.level == null) return null;
            long ticks = c.level.getLevelData().getGameTime();
            return "World age: " + ticks + " ticks";
        }
    }

    public static final class ServerAddress extends Module {
        public ServerAddress() { super("Server Address"); }
        @Override public String hudText(Minecraft c) {
            var server = c.getCurrentServer();
            return server == null ? "Server: Singleplayer" : "Server: " + server.ip;
        }
    }

    public static final class BlockInfo extends Module {
        public BlockInfo() { super("Block Info"); }
        @Override public String hudText(Minecraft c) {
            if (c.level == null || !(c.hitResult instanceof BlockHitResult hit))
                return null;
            return "Block: " + c.level.getBlockState(hit.getBlockPos())
                .getBlock().getName().getString();
        }
    }
}
