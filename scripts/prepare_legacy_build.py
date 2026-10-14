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
    print(f"Prepared {sys.argv[1]} legacy API variant: {count} source files updated.")


if __name__ == "__main__":
    main()
