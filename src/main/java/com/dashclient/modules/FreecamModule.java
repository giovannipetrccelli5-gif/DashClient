package com.dashclient.modules;

import com.dashclient.Module;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import java.util.Locale;
import java.util.UUID;

/**
 * A client-only spectator-like camera. The real player is not moved, the
 * camera cannot load extra chunks, and no movement packets are spoofed.
 */
public final class FreecamModule extends Module {
    private static final int CAMERA_ENTITY_ID = -242003;
    private static final double NORMAL_SPEED = 0.55;
    private static final double SPRINT_SPEED = 1.15;

    private CameraAvatar camera;
    private ClientLevel cameraLevel;
    private LocalPlayer anchor;
    private ClientInput originalInput;
    private final ClientInput idleInput = new ClientInput();
    private CameraType oldPerspective;

    public FreecamModule() {
        super("Freecam");
    }

    /** Camera used by the small mouse-turn mixin. */
    public AbstractClientPlayer cameraEntity() {
        return camera;
    }

    @Override
    public void onEnable() {
        // May be restored from config while the main menu is showing.
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.player != null && client.options != null)
            attach(client);
    }

    private void attach(Minecraft client) {
        if (camera != null || client.player == null || client.level == null) return;
        anchor = client.player;
        cameraLevel = client.level;
        originalInput = anchor.input;
        oldPerspective = client.options.getCameraType();

        // The camera is not a server-controlled entity. Spawn it only locally.
        CameraAvatar next = new CameraAvatar(cameraLevel);
        next.snapTo(anchor.getX(), anchor.getY(), anchor.getZ(),
            anchor.getYRot(), anchor.getXRot());
        next.setNoGravity(true);
        next.noPhysics = true;
        next.getAbilities().flying = true;
        next.setInvisible(true);
        cameraLevel.addEntity(next);
        camera = next;
        client.options.setCameraType(CameraType.FIRST_PERSON);
        client.setCameraEntity(next);
        anchor.input = idleInput;
    }

    /** Called at the START of the client tick, before the player's movement. */
    public void beforePlayerTick(Minecraft client) {
        if (camera != null && client.player == anchor && client.level == cameraLevel
                && anchor.input != idleInput) {
            anchor.input = idleInput;
        }
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            if (camera != null) {
                setEnabled(false);
            }
            return;
        }

        if (camera != null && (client.player != anchor || client.level != cameraLevel
                || anchor.isRemoved())) {
            setEnabled(false); // Reset safely on respawn, dimension change or disconnect.
            return;
        }

        if (camera == null) {
            attach(client);
            return;
        }

        // Never interpret typing and menu navigation as camera motion.
        if (client.gui.screen() != null) {
            camera.setDeltaMovement(Vec3.ZERO);
            return;
        }

        int forward = bool(client.options.keyUp.isDown()) - bool(client.options.keyDown.isDown());
        int right = bool(client.options.keyRight.isDown()) - bool(client.options.keyLeft.isDown());
        int up = bool(client.options.keyJump.isDown()) - bool(client.options.keyShift.isDown());
        if (forward == 0 && right == 0 && up == 0) {
            camera.setDeltaMovement(Vec3.ZERO);
            return;
        }

        Vec3 look = Vec3.directionFromRotation(0f, camera.getYRot());
        Vec3 side = Vec3.directionFromRotation(0f, camera.getYRot() + 90f);
        Vec3 movement = look.scale(forward).add(side.scale(right)).add(0, up, 0);
        if (movement.lengthSqr() > 0) {
            double speed = client.options.keySprint.isDown() ? SPRINT_SPEED : NORMAL_SPEED;
            movement = movement.normalize().scale(speed);
            // Moving this fake entity locally does not send any movement to a server.
            camera.setDeltaMovement(Vec3.ZERO);
            camera.setPos(camera.getX() + movement.x,
                camera.getY() + movement.y, camera.getZ() + movement.z);
        }
    }

    private static int bool(boolean value) {
        return value ? 1 : 0;
    }

    @Override
    public String hudText(Minecraft client) {
        return camera == null ? "Freecam: Waiting for world" :
            String.format(Locale.ROOT, "Freecam XYZ %.0f %.0f %.0f",
                camera.getX(), camera.getY(), camera.getZ());
    }

    @Override
    public void onDisable() {
        Minecraft client = Minecraft.getInstance();
        try {
            if (camera != null && client.getCameraEntity() == camera && client.player != null)
                client.setCameraEntity(client.player);
            if (anchor != null && client.player == anchor && anchor.input == idleInput)
                anchor.input = originalInput;
            if (oldPerspective != null && client.options != null)
                client.options.setCameraType(oldPerspective);
            if (camera != null && cameraLevel != null)
                cameraLevel.removeEntity(camera.getId(), Entity.RemovalReason.DISCARDED);
        } finally {
            camera = null;
            cameraLevel = null;
            anchor = null;
            originalInput = null;
            oldPerspective = null;
        }
    }

    /**
     * Invisible local avatar solely used as the renderer's camera target.
     * Never instantiated on the server and never added to a multiplayer connection.
     */
    private static final class CameraAvatar extends AbstractClientPlayer {
        CameraAvatar(ClientLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "DashFreecam"));
            setId(CAMERA_ENTITY_ID);
            noPhysics = true;
            setNoGravity(true);
            getAbilities().flying = true;
        }

        @Override
        public float getViewXRot(float partialTick) {
            return getXRot();
        }

        @Override
        public float getViewYRot(float partialTick) {
            return getYRot();
        }

        @Override
        public void tick() {
            super.tick();
            noPhysics = true;
            setNoGravity(true);
            setDeltaMovement(Vec3.ZERO);
        }
    }
}
