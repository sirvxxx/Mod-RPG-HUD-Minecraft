# Custom RPG HUD (Fabric, Client-Side)

A lightweight, high-performance client-side HUD mod for Minecraft that brings a sleek RPG/MMORPG status interface to your screen.

## Features
- **Dynamic Skin Face & Name:** Displays your active player skin face and username right on the HUD.
- **Dual Overlay Modes:** Switch between **Circle Mode** (curved segmented bars around your avatar) and **Bar Mode** (classic rectangular layout).
- **Status Bars:** Real-time segmented bars for Health (HP), Hunger, and Armor.
- **Extra Widgets:** Built-in live **Coordinates (XYZ)** and **FPS** tracker panels.
- **In-Game Drag & Drop:** Easily reposition your HUD layout anywhere on the screen using your mouse.
- **Theming & Localization:** Multiple color theme presets and auto-language support based on your game settings.
- **Performance Focused:** Highly optimized with zero-lag overhead to prevent any noticeable FPS drops.

---

## Quick Download (via GitHub Actions)
If you just want the `.jar` file without setting up a local build environment:
1. Go to the **Actions** tab in this repository.
2. Click on the latest workflow run and wait for it to finish.
3. Scroll down to the **Artifacts** section and download the build zip.
4. Drop the `.jar` file into your `.minecraft/mods/` folder alongside the Fabric API.

---

## Local Build
If you want to compile it yourself:
- Requires **JDK 25** and **Gradle 9.5.1+**.
- Run `./build.sh` (Linux/macOS) or `build.bat` (Windows).
- Find your compiled `.jar` inside `build/libs/`.

---

## In-Game Controls & Commands
- Press **`H`** or type `/rpghud gui` to open the settings menu.
- **Features available:** Drag-and-drop HUD positioning, overlay type switcher (Circle/Bar), theme presets, element toggles, and size scaling.
- Config file is saved locally at `.minecraft/config/custom_rpg_hud.json`.
