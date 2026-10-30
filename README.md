# DashClient 2.0 — Modern GUI Edition
An original client-side **Fabric Minecraft mod** with a complete DashClient v3 user-interface overhaul, custom-drawn cards, toggles, and configurable HUD widgets.

## New custom GUI
- **Right Shift** opens the redesigned client settings.
- All visible module cards are drawn as original Minecraft-native panels instead of vanilla menu buttons.
- Left sidebar categories: All, HUD, Combat, Survival, World, Movement, Performance, Visual.
- Live module search, responsive page navigation, module selection and live ON/OFF toggles.
- On wider screens the details pane displays module information and a second working toggle.
- Shortcuts: HUD opens the draggable editor, Waypoints opens the local location manager, Profiles switches independent configurations, and Video changes vanilla settings.
- Styled profiles, waypoint manager, performance presets, and an alignment-grid HUD editor. GUI adapts to smaller Minecraft GUI scales.
- Accessible editable search remains a native Minecraft text input.


## Original modules removed
This release intentionally replaces the legacy DashClient code and old 143+ module collection rather than migrating them. No old automation, x-ray, or experimental world modification code is bundled. The previous project is recoverable on the `legacy-dashclient-v1` branch.

## In-game features
Press **Right Shift** for the new DashClient mod menu. Search modules, toggle live readouts and interface settings, choose module profiles (Default / PvP / Survival / Low FPS), adjust vanilla graphics presets, and drag readouts in the HUD editor (**J**).
Hold **C** to use the optional zoom. Supported readouts: FPS, coordinates, ping, direction, health, speed, time, memory, clock, playtime, rotation and dimension. All settings and HUD positions persist.

## Important limitations
DashClient is **not Lunar Client** and is not affiliated with Lunar Client. No Lunar code, logo, textures, cosmetics, account service or launcher is included. The settings screen's Restore button restores the snapshot taken before the first preset used in the current game session. Graphics presets do not guarantee FPS gains. Existing Java launchers with Fabric support can run the built JAR. A standalone desktop launcher is not included.

## Builds
GitHub Actions targets: Minecraft Java 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2 and 26.3. Build artifacts are only ready when the workflow passes. A build verifies compilation; real-device playtesting is still necessary.

## License — All Rights Reserved
DashClient's original code, assets, and documentation are **proprietary**. You may download and run official unmodified compiled builds for personal, non-commercial gameplay, but you may **not** reuse, modify, redistribute, rebrand, or sell original DashClient material without written permission from the project owner, except as allowed by law or platform terms. Third-party libraries and assets retain their own licenses. See [LICENSE.md](LICENSE.md) for the full notice.
