# DashClient

A client-side Fabric module collection for **Minecraft 26.3**, Java 25.
Built with GitHub Actions. Compatible with other optimization mods because
DashClient does not inject into chunk rendering, world renderers, lighting,
Sodium, Entity Culling or other mods.

## 124 modules

There are 124 toggleable modules: the previous 78 plus 44 new status and
inventory readouts, and two real performance modules.

### FPS and performance
- **Performance Boost**: balanced low-load graphics preset, lower draw/simulation
  distances, minimal particles, clouds/shadows off, zero biome blend.
- **Extreme FPS**: more aggressive preset: as little as 4 chunks render distance
  and 4 simulation distance. Quality is significantly reduced for speed.
- **Adaptive FPS**: every roughly 5 seconds, checks measured FPS. When below
  45 FPS for two samples, lower view distance 2 chunks (minimum 4). Above
  85 FPS for three samples, raise it 1 chunk (up to your saved limit).
- Presets never run simultaneously. Both save original Minecraft graphics
  settings, and attempt to restore them when switched off.
- No claims of guaranteed FPS increases or compatibility with every device.
  Server/network TPS will not be improved by a client-side mod.

### New readouts
18 item counters (netherite, ancient debris, rockets, iron/gold,
obsidian, cobwebs, end crystals, TNT, water/lava buckets and more);
performance diagnostics (FPS average/min/max, approximate frame time,
FPS warnings, settings information) and survival/equipment
readouts (armor durability by slot, saturation, player information
and key states).

### Lunar-inspired DashClient module dashboard
**Right Shift** opens an original, dark, cyan-accented dashboard,
with category navigation, search, enabled/disabled module switches,
pagination and an **Edit HUD** button. Supports smaller screens.

### Drag HUD elements
Choose **Edit HUD** from the module GUI. Click/hold (or touch/hold)
a displayed HUD element and drag it to a new location. Positions save on
release and remain at approximately the same location when resizing the window.
**Reset Positions** restores the default layout.
Only enabled modules with visible text are draggable.

### Download
[Latest compiled JAR](https://github.com/giovannipetrccelli5-gif/DashClient/releases/download/dev-build/dashclient-1.0.0.jar)
or [Release page](https://github.com/giovannipetrccelli5-gif/DashClient/releases/tag/dev-build).
Install the JAR into your Minecraft Fabric mods folder. Compilation
and unit tests run in GitHub Actions; gameplay should be tested separately.
