package com.dashclient;

import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipFile;

/**
 * Safe, review-before-install inbox for Fabric mods and resource packs.
 * This never executes imported jars and never writes to server plugin folders.
 */
public final class ClientFileInbox {
    public enum Kind { FABRIC_MOD, RESOURCE_PACK, INVALID }
    public record Entry(String name, Kind kind, String status) {}
    public record Result(boolean success, String message) {}
    private static final long MAX_BYTES = 150L * 1024 * 1024;
    private static final int MAX_FILES = 200;
    private static final String INBOX_NAME = "dashclient-imports";

    private ClientFileInbox() {}

    public static Path inbox() {
        return FabricLoader.getInstance().getGameDir().resolve(INBOX_NAME);
    }

    public static String instructions() {
        return "Copy Fabric .jar or resource pack .zip files into the dashclient-imports folder";
    }

    public static List<Entry> scan() {
        try {
            Files.createDirectories(inbox());
            List<Entry> found = new ArrayList<>();
            try (var paths = Files.list(inbox())) {
                for (Path path : paths.sorted(Comparator.comparing(p ->
                        p.getFileName().toString().toLowerCase(Locale.ROOT))).limit(MAX_FILES).toList()) {
                    String name = path.getFileName().toString();
                    Kind kind = validate(path);
                    String status = switch (kind) {
                        case FABRIC_MOD -> "Fabric mod - restart Minecraft after installation";
                        case RESOURCE_PACK -> "Resource pack - activate in Resource Packs";
                        case INVALID -> "Unsupported or invalid file";
                    };
                    found.add(new Entry(name, kind, status));
                }
            }
            return found;
        } catch (IOException e) {
            DashClient.LOGGER.warn("Cannot list DashClient file inbox", e);
            return List.of();
        }
    }

    public static Kind validate(Path file) {
        try {
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                    || Files.size(file) <= 0 || Files.size(file) > MAX_BYTES) return Kind.INVALID;
            String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
            if (!name.endsWith(".jar") && !name.endsWith(".zip")) return Kind.INVALID;
            try (ZipFile zip = new ZipFile(file.toFile())) {
                if (name.endsWith(".jar") && zip.getEntry("fabric.mod.json") != null)
                    return Kind.FABRIC_MOD;
                if (name.endsWith(".zip") && zip.getEntry("pack.mcmeta") != null)
                    return Kind.RESOURCE_PACK;
            }
        } catch (Exception ignored) {
            // Non-zip, oversized, unreadable and truncated files are rejected.
        }
        return Kind.INVALID;
    }

    /**
     * Stage a file dropped on the client window. The drop gesture is deliberate;
     * callers can then install the validated staged file immediately.
     */
    public static Result stageDropped(Path dropped) {
        if (dropped == null || validate(dropped) == Kind.INVALID)
            return new Result(false, "Unsupported drop: Fabric JARs or pack ZIPs only");
        String name = dropped.getFileName().toString();
        if (!validName(name)) return new Result(false, "Invalid filename");
        try {
            Files.createDirectories(inbox());
            Path target = inbox().resolve(name);
            if (dropped.toAbsolutePath().normalize().equals(target.toAbsolutePath().normalize()))
                return new Result(true, "Already in import inbox");
            Files.copy(dropped, target);
            return new Result(true, "Added to import inbox");
        } catch (java.nio.file.FileAlreadyExistsException e) {
            return new Result(false, "File already in inbox; did not overwrite");
        } catch (IOException e) {
            return new Result(false, "Cannot stage file; check storage permission");
        }
    }

    private static boolean validName(String name) {
        return name != null && name.matches("[A-Za-z0-9 _+.()-]{1,120}")
            && !name.equals(".") && !name.equals("..");
    }

    /** Only files currently in this inbox are eligible for installation. */
    public static Result install(String requestedName) {
        if (!validName(requestedName)) return new Result(false, "Invalid filename");
        Path source = inbox().resolve(requestedName).normalize();
        if (!source.getParent().equals(inbox()))
            return new Result(false, "File must be inside dashclient-imports");
        Kind kind = validate(source);
        if (kind == Kind.INVALID) return new Result(false, "Unsupported file; no changes made");

        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path destinationFolder = gameDir.resolve(
            kind == Kind.FABRIC_MOD ? "mods" : "resourcepacks");
        Path destination = destinationFolder.resolve(source.getFileName()).normalize();
        if (!destination.getParent().equals(destinationFolder))
            return new Result(false, "Invalid destination");
        try {
            Files.createDirectories(destinationFolder);
            // Never replace existing mods or packs; avoid accidental data loss.
            Files.copy(source, destination);
            return new Result(true, kind == Kind.FABRIC_MOD
                ? "Mod copied to mods folder. Restart Minecraft to load it."
                : "Pack copied to resourcepacks. Enable it in Resource Packs.");
        } catch (java.nio.file.FileAlreadyExistsException e) {
            return new Result(false, "That file already exists. Existing file was not changed.");
        } catch (IOException e) {
            DashClient.LOGGER.warn("Could not import {}", requestedName, e);
            return new Result(false, "Import failed: check storage permission and free space.");
        }
    }
}
