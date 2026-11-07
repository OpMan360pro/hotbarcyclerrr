# Hotbar Cyclerrr

A lightweight Fabric mod for Minecraft 1.21.11 that lets you cycle through all rows of your player inventory as active hotbars using keybindings.

Based on [Hotbar Cycler](https://github.com/ceaweed/hotbarcycler) by [ceaweed](https://github.com/ceaweed).

---

## Features

- **Inventory Row Cycling**: Quick-switch between the hotbar and any of the 3 main inventory rows.
- **Direct Row Switching**: Bind direct keys to instantly switch to rows 1, 2, 3, or 4 (Row 1 bottom to Row 4 top).
- **HUD Row Indicator**: Visual indicator next to the hotbar showing which row is active. Supports styles (`Default`, `Minimal`, `Dots`, `Accent` with custom color), custom offset, and size.
- **Inventory Screen Highlight**: Highlight frame and tint over the active row in container and player screens. Fully customizable colors and opacity via RGB sliders with live preview.
- **Server Compatibility Settings**: Configurable swap delay, sprint pausing, input blocking, and movement freezing to prevent desyncs on strict or lagging servers.
- **Seamless Hotbar (Optional)**: Smooth visual client-side sync.
- **Configuration Screen**: In-game settings accessible via ModMenu or dedicated keybinding.

---

## How It Works

Minecraft player inventories consist of 36 main storage slots organized in 4 rows of 9:

```
[ Row 3 ]  slots 27–35   ← Virtual Hotbar 4
[ Row 2 ]  slots 18–26   ← Virtual Hotbar 3
[ Row 1 ]  slots  9–17   ← Virtual Hotbar 2
[Hotbar ]  slots  0–8    ← Active Hotbar 1 (default)
```

Pressing the **Next** or **Previous** keybind physically swaps the selected row into hotbar slots (0–8) and synchronizes with the server. When opening an inventory screen or disconnecting, the inventory safely reverts to the standard layout.

---

## Keybindings

Default category: **Hotbar Cyclerrr** (Options → Controls → Key Binds):

| Action | Default Key | Description |
|---|---|---|
| **Next Hotbar Row** | Unbound | Cycles to the next inventory row |
| **Previous Hotbar Row** | Unbound | Cycles to the previous inventory row |
| **Switch to Row 1 (Hotbar)** | Unbound | Directly switches to Row 1 (bottom / hotbar) |
| **Switch to Row 2** | Unbound | Directly switches to Row 2 |
| **Switch to Row 3** | Unbound | Directly switches to Row 3 |
| **Switch to Row 4 (Top)** | Unbound | Directly switches to Row 4 (top row) |
| **Return to Fixed Slots** | Unbound | Returns items back to their default fixed layout |
| **Open Settings** | Unbound | Opens mod configuration screen |
| **Toggle Row Indicator** | Unbound | Toggles HUD indicator visibility |

---

## Requirements

- **Minecraft**: 1.21.11 (supports >=1.21.10)
- **Fabric Loader**: >=0.14.0
- **Fabric API**: required
- **ModMenu** (optional): for in-game configuration access

Works in singleplayer and on multiplayer servers (mod must be installed on both client and server for full multiplayer synchronization).

---

## Credits & Acknowledgements

This mod is a fork/continuation based on [Hotbar Cycler](https://github.com/ceaweed/hotbarcycler) originally created by [ceaweed](https://github.com/ceaweed).

---

## License

This project is licensed under the [MIT License](LICENSE).
