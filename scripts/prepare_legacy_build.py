#!/usr/bin/env python3
"""Per-target compatibility bridge; runs only in the CI workspace."""
from pathlib import Path
import sys
mc = sys.argv[1]
root=Path("src/main/java")
if mc in ("1.21.11","26.1","26.1.1","26.1.2"):
    for path in root.rglob("*.java"):
        code=path.read_text(encoding="utf-8")
        code=code.replace(".gui.screen()", ".screen").replace(".gui.setScreen(", ".setScreen(")
        if mc=="1.21.11":
            code=code.replace("net.minecraft.client.gui.GuiGraphicsExtractor",
                "net.minecraft.client.gui.GuiGraphics")
            code=code.replace("GuiGraphicsExtractor", "GuiGraphics")
            code=code.replace("extractRenderState(", "render(")
            code=code.replace(".centeredText(", ".drawCenteredString(")
            code=code.replace(".text(", ".drawString(")
            code=code.replace("net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper",
                "net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper")
            code=code.replace("KeyMappingHelper.registerKeyMapping(",
                "KeyBindingHelper.registerKeyBinding(")
        path.write_text(code,encoding="utf-8")
if mc=="1.21.11":
    gradle=Path("build.gradle")
    source=gradle.read_text(encoding="utf-8")
    source=source.replace("id 'net.fabricmc.fabric-loom' version",
       "id 'net.fabricmc.fabric-loom-remap' version")
    source=source.replace(
       '    minecraft "com.mojang:minecraft:${minecraft_version}"',
       '    minecraft "com.mojang:minecraft:${minecraft_version}"\n    mappings loom.officialMojangMappings()')
    source=source.replace(
       '    implementation "net.fabricmc:fabric-loader:',
       '    modImplementation "net.fabricmc:fabric-loader:')
    source=source.replace(
       '    implementation "net.fabricmc.fabric-api:fabric-api:',
       '    modImplementation "net.fabricmc.fabric-api:fabric-api:')
    gradle.write_text(source,encoding="utf-8")
print("Prepared clean-room DashClient for",mc)
