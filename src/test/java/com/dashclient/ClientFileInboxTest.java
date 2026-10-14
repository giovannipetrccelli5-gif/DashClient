package com.dashclient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.junit.jupiter.api.Assertions.*;

final class ClientFileInboxTest {
    @TempDir Path directory;

    private Path archive(String fileName, String... entries) throws Exception {
        Path file = directory.resolve(fileName);
        try (ZipOutputStream z = new ZipOutputStream(Files.newOutputStream(file))) {
            for (String entry : entries) {
                z.putNextEntry(new ZipEntry(entry));
                z.write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                z.closeEntry();
            }
        }
        return file;
    }

    @Test void onlyAcceptsFabricJars() throws Exception {
        assertEquals(ClientFileInbox.Kind.FABRIC_MOD,
            ClientFileInbox.validate(archive("example.jar", "fabric.mod.json")));
        assertEquals(ClientFileInbox.Kind.INVALID,
            ClientFileInbox.validate(archive("forge.jar", "META-INF/mods.toml")));
        assertEquals(ClientFileInbox.Kind.INVALID,
            ClientFileInbox.validate(archive("wrong.zip", "fabric.mod.json")));
    }

    @Test void onlyAcceptsResourcePackZip() throws Exception {
        assertEquals(ClientFileInbox.Kind.RESOURCE_PACK,
            ClientFileInbox.validate(archive("resource.zip", "pack.mcmeta")));
        assertEquals(ClientFileInbox.Kind.INVALID,
            ClientFileInbox.validate(archive("wrong.jar", "pack.mcmeta")));
    }

    @Test void rejectsPlainFilesAndFolders() throws Exception {
        Path fake = directory.resolve("fake.jar");
        Files.writeString(fake, "not a jar");
        assertEquals(ClientFileInbox.Kind.INVALID, ClientFileInbox.validate(fake));
        assertEquals(ClientFileInbox.Kind.INVALID, ClientFileInbox.validate(directory));
    }

    @Test void rejectsPathTraversalBeforeFilesystemAccess() {
        assertFalse(ClientFileInbox.install("../mods/evil.jar").success());
        assertFalse(ClientFileInbox.install("..").success());
        assertFalse(ClientFileInbox.install("/absolute.jar").success());
    }
}
