# DashClient

A client-side Fabric module collection for **Minecraft 26.3**, Java 25.
Built with GitHub Actions. Compatible with other optimization mods because
DashClient does not inject into chunk rendering, world renderers, lighting,
Sodium, Entity Culling or other mods.

## 143 modules

There are 143 toggleable modules: the previous 78 plus 44 new status and
inventory readouts, two FPS modes, and 19 combat, player radar and visuals modules.

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

### Auto Miner (nearby selected-block mining)
- Toggle **F7** or **Right Shift → World → Auto Miner** to open the new picker.
  It searches every item registered with Minecraft and shows its item icon;
  only actual placeable block items can be selected as mining targets.
- Choose a block (e.g. Stone or Diamond Ore), then press **Start Mining**.
  The miner looks for matching blocks in a **16-block radius** of where it
  started. It uses normal block-breaking actions, walks flat safe routes,
  and continues mining nearby targets without holding input.
- **F7 turns it off immediately.** It also stops mining on low health,
  low hunger, disconnect, death or after 30 minutes. No automatic startup.
- Does not dig tunnels through other blocks, cross lava/water, climb stairs,
  or explore unloaded areas. Unreachable blocks are skipped. No pathfinding
  beyond the bounded, flat area and no anti-cheat bypass.
- Equip an appropriate tool before starting. Run only where automation is
  permitted; some public servers disallow it.

### Freecam (client-only camera)
- **F6** toggles Freecam, or open **Right Shift → Visuals → Freecam**.
- Fly the camera with **W/A/S/D**, **Space** up and **Shift** down.
  Hold the sprint key for faster movement.
- Your real player stays at its original position. Freecam does **not**
  teleport you, spoof movement, or reveal chunks the server has not sent.
- When disabled, the view returns to your player. The module also resets
  when you change worlds or respawn.
- Left-click mining, attack, right-click item use and block picking are
  suppressed while Freecam is active, to reduce accidents.
- **Experimental:** automated builds verify compilation, not real gameplay.
  Servers may prohibit Freecam; respect their rules.

### Village Finder (loaded-chunk detection)
- Toggle with **V** or **Right Shift → World → Village Finder**.
- Displays **X Y Z coordinates** on the HUD and announces newly identified
  likely village locations on-screen.
- Scans for **bells, beds and villagers** within 48 blocks horizontally of
  the player, using a capped incremental search for Android performance.
- **Likely village** means village clues were found together; **Possible
  village** means the evidence is uncertain. If only villagers are present,
  their position is reported without claiming a village.
- **Limit:** Does not use the world seed or a server-side `/locate` command.
  It cannot detect villages in unloaded, unexplored or unsent chunks.

### Ore X-Ray overlay
- **X-Ray**: press **X** or toggle **Visuals → X-Ray** in the Right Shift menu.
  Coloured boxes mark diamond, emerald, debris, gold, iron, copper,
  redstone, lapis, coal and quartz ore, including their deepslate variants.
- The overlay stays visible through solid blocks but **only uses terrain
  already sent by the server**. It does not reveal unloaded chunks.
- Scanning is spread across ticks and capped at 72 markers to reduce FPS impact
  on Android. It uses no Sodium or terrain renderer mixins.
- Turn it off with X. Some servers prohibit X-Ray features; follow server rules.

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

### Experimental Combat & Visual modules (19)
- **Auto Clicker**: up to four attempts/second at a crosshair-selected
  entity when the vanilla attack cooldown is sufficiently recovered.
- **Player ESP** and **Entity ESP**: glowing outlines on nearby, already
  loaded entities. No hidden or unsent terrain is revealed.
- **Fly**: experimental local survival flight; the server remains authoritative.
  If flight is not permitted, expect rubber-banding, kick or moderation.
  No anti-cheat bypasses or packet spoofing are implemented.
- **Extended View**: tries to increase local render distance to 24 chunks.
  Cannot display world data beyond what the server sends.
- Nearby player/mob radar, nearest mob/player, entity velocity/coordinates,
  target health and attack cooldown, fall/water/lava state.
- **Combat and Visuals** categories in the Right Shift dashboard.


## DashClient File Manager (Fabric client / PojavLauncher)

Open **Right Shift → FILES** in DashClient.

1. In your Android file manager, find the Minecraft game directory used by PojavLauncher.
2. Copy downloaded **Fabric client mod .jar** files or **Minecraft resource pack .zip** files into its **dashclient-imports** subfolder. Open the FILES panel and press **Refresh** if necessary.
3. Check the file name and choose **Install Mod** or **Install Pack**. Fabric JARs copy to `mods/` and require a game restart. Resource pack ZIPs copy to `resourcepacks/` and must be enabled from Minecraft's Resource Packs settings.

Only JARs containing `fabric.mod.json` and ZIPs containing `pack.mcmeta` are accepted. Existing files are not overwritten. Mods execute code after restart; only install downloads you trust. The panel does not install server plugins: Paper/Purpur/Spigot plugin JARs must be installed on the server by its administrator. Mobile touch drag-and-drop depends on the launcher's OS integration; the file-manager inbox works without it.

**Performance:** Enabled modules are not all free. The HUD cache throttles informational readouts, and idle inventory scans are skipped. Real FPS and RAM improvements vary with renderer, device, world, and enabled mods.
