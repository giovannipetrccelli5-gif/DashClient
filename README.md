# DashClient 2.0
An original client-side **Fabric Minecraft mod** with an all-new dark client menu and optional HUD widgets.

## Original modules removed
This release intentionally replaces the legacy DashClient code and old 143+ module collection rather than migrating them. No old automation, x-ray, or experimental world modification code is bundled. The previous project is recoverable on the `legacy-dashclient-v1` branch.

## In-game features
Press **Right Shift** for the new DashClient mod menu. Search modules, toggle live readouts and interface settings, choose module profiles (Default / PvP / Survival / Low FPS), adjust vanilla graphics presets, and drag readouts in the HUD editor (**J**).
Hold **C** to use the optional zoom. Supported readouts: FPS, coordinates, ping, direction, health, speed, time, memory, clock, playtime, rotation and dimension. All settings and HUD positions persist.

## Important limitations
DashClient is **not Lunar Client** and is not affiliated with Lunar Client. No Lunar code, logo, textures, cosmetics, account service or launcher is included. The settings screen's Restore button restores the snapshot taken before the first preset used in the current game session. Graphics presets do not guarantee FPS gains. Existing Java launchers with Fabric support can run the built JAR. A standalone desktop launcher is not included.

## Builds
GitHub Actions targets: Minecraft Java 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2 and 26.3. Build artifacts are only ready when the workflow passes. A build verifies compilation; real-device playtesting is still necessary.
