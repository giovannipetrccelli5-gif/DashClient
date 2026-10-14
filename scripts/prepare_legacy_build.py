#!/usr/bin/env python3
"""Adapt official Mojang API source names for builds before Minecraft 26.2.

Executed only in the ephemeral GitHub Actions checkout, before compilation.
The committed source remains the 26.3 variant; `git restore` resets it for
each build. This avoids runtime reflection or mixing incompatible APIs.
"""
from pathlib import Path
import sys

LEGACY = {"1.21.11", "26.1", "26.1.1", "26.1.2"}


def main():
    if len(sys.argv) != 2 or sys.argv[1] not in LEGACY:
        raise SystemExit(f"Expected one of {sorted(LEGACY)}")
    root = Path("src/main/java")
    count = 0
    replaced = set()
    for file in root.rglob("*.java"):
        original = file.read_text(encoding="utf-8")
        content = original

        # 26.2+ Minecraft.gui owns the screen; older builds keep it on Minecraft.
        for before, after in (
            (".gui.screen()", ".screen"),
            (".gui.setScreen(", ".setScreen("),
            (".gui.hud.setOverlayMessage(", ".gui.setOverlayMessage("),
            (".gameRenderer.mainCamera()", ".gameRenderer.getMainCamera()"),
            ("net.minecraft.world.entity.EntityTypes", "net.minecraft.world.entity.EntityType"),
            ("EntityTypes.VILLAGER", "EntityType.VILLAGER"),
        ):
            if before in content:
                content = content.replace(before, after)
                replaced.add(before)
        if content != original:
            file.write_text(content, encoding="utf-8")
            count += 1
    expected = {
        ".gui.screen()",
        ".gui.setScreen(",
        ".gui.hud.setOverlayMessage(",
        ".gameRenderer.mainCamera()",
        "net.minecraft.world.entity.EntityTypes",
        "EntityTypes.VILLAGER",
    }
    missing = expected - replaced
    if missing:
        raise SystemExit("Legacy adapter did not find required API references: " + str(missing))
    if sys.argv[1] == "1.21.11":
        # Minecraft 1.21.11 predates the GUI extraction API introduced in 26.x.
        # Its legacy GuiGraphics render methods have different names.
        replaces = (
            ("net.minecraft.client.gui.GuiGraphicsExtractor",
             "net.minecraft.client.gui.GuiGraphics"),
            ("GuiGraphicsExtractor", "GuiGraphics"),
            ("extractRenderState(", "render("),
            (".centeredText(", ".drawCenteredString("),
            (".itemDecorations(", ".renderItemDecorations("),
            (".item(", ".renderItem("),
            (".text(", ".drawString("),
            ("net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper",
             "net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper"),
            ("KeyMappingHelper.registerKeyMapping(", "KeyBindingHelper.registerKeyBinding("),
            ("net.minecraft.world.inventory.ContainerInput",
             "net.minecraft.world.inventory.ClickType"),
            ("ContainerInput.", "ClickType."),
            ("camera.getFov()", "client.options.fov().get()"),
            ("graphics.outline(", "graphics.renderOutline("),
            (".handleContainerInput(", ".handleInventoryMouseClick("),
        )
        adapted = 0
        for file in root.rglob("*.java"):
            source = file.read_text(encoding="utf-8")
            changed = source
            for before, after in replaces:
                changed = changed.replace(before, after)
            # Only GUI rendering's text() changed; HudFrameCache.Line.text()
            # remains a record accessor, not a graphics method.
            changed = changed.replace("line.drawString()", "line.text()")
            if changed != source:
                file.write_text(changed, encoding="utf-8")
                adapted += 1
        print(f"Mapped 1.21.11 GUI/inventory APIs in {adapted} files.")

    if sys.argv[1] == "1.21.11":
        # 1.21.11 was still obfuscated: Fabric API ships intermediary access
        # wideners and must be remapped with Loom's legacy plugin.
        gradle_file = Path("build.gradle")
        gradle = gradle_file.read_text(encoding="utf-8")
        gradle = gradle.replace(
            "id 'net.fabricmc.fabric-loom' version",
            "id 'net.fabricmc.fabric-loom-remap' version",
        )
        gradle = gradle.replace(
            '    minecraft "com.mojang:minecraft:${minecraft_version}"',
            '    minecraft "com.mojang:minecraft:${minecraft_version}"'
            + "\n    mappings loom.officialMojangMappings()",
        )
        gradle = gradle.replace(
            '    implementation "net.fabricmc:fabric-loader:',
            '    modImplementation "net.fabricmc:fabric-loader:',
        ).replace(
            '    implementation "net.fabricmc.fabric-api:fabric-api:',
            '    modImplementation "net.fabricmc.fabric-api:fabric-api:',
        )
        if "mappings loom.officialMojangMappings()" not in gradle:
            raise SystemExit("Missing 1.21.11 mappings dependency")
        gradle_file.write_text(gradle, encoding="utf-8")
        print("Switched Minecraft 1.21.11 to remapping Loom + Mojang mappings.")

    print(f"Prepared {sys.argv[1]} legacy API variant: {count} source files updated.")


if __name__ == "__main__":
    main()
