# DashClient

A Fabric client-side utility mod for **Minecraft 26.3**, built using Java 25,
Fabric API and Loom 1.17.

**78 toggleable modules**: the original 26, plus 52 additional modules inspired by
the practical HUD/status features offered by popular Minecraft clients.

## Controls
- **Right Shift** — DashClient menu (paginated).
- **Hold C** — zoom (enable Zoom in the menu first).
- Customize both controls through Minecraft Controls.

## New modules (52)
**Inventory and survival (17):** Arrows, Spectral Arrows, Ender Pearls,
Firework Rockets, Golden Apples, Enchanted Apples, Diamonds, Emeralds,
Experience Level, Experience Progress, Armor Points, Absorption Hearts,
Air Supply, Offhand Item, Offhand Count, Inventory Free Slots, Inventory Fill.

**World and target (18):** Target Name, Target Health, Target Range,
Target Type, Target Block XYZ, Target Block Range, Target Block State,
Block Hardness, Block Light, Sky Light, Dimension, Weather, World Days,
Online Players, Yaw, Pitch, Nether Coordinates, Chunk Offset.

**Movement and HUD styling (17):** Sprint State, Sneak State, Swim State,
Elytra State, Creative Flight, Ground State, Fire Warning, Vertical Speed,
Session Distance, Peak Speed, Low Health Warning, Low Durability Warning,
HUD Rainbow, HUD Right Align, HUD Transparent, HUD Gameplay Only, HUD Compact.

The HUD wraps across columns and truncates long entries to avoid drawing
off-screen. Colors, alignment, spacing, and backgrounds are independently
toggleable. HUD switches do not themselves draw a status line.

Only FPS Display and Coordinates are on by default; the other modules
stay off until enabled. Modules are saved in Fabric's config directory.
The Performance Boost module only changes local settings (no claimed FPS multiple).

## Downloads
[Development JAR on GitHub Releases](https://github.com/giovannipetrccelli5-gif/DashClient/releases/tag/dev-build)

GitHub Actions builds on each push. Compilation is verified separately from
in-game functionality, which still requires hands-on testing.
