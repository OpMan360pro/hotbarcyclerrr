# Hotbar Cyclerrr

A lightweight Fabric mod for Minecraft 1.21.11 that lets you cycle through all rows of your player inventory as active hotbars using keybindings.

Based on [Hotbar Cycler](https://github.com/ceaweed/hotbarcycler) by [ceaweed](https://github.com/ceaweed).

---

## Features

- **Inventory Row Cycling**: Quick-switch between the hotbar and any of the 3 main inventory rows.
- **HUD Row Indicator**: Visual indicator next to the hotbar showing which row is currently active (customizable position and size).
- **Inventory Screen Highlight**: Clear visual frame around the currently active row when viewing player inventory or containers.
- **In-Menu Hotkeys**: Cycle rows even while inside inventory and container screens.
- **Seamless Hotbar (Optional)**: Smooth visual sync keeping the hotbar appearance consistent.
- **Configuration Screen**: Accessible via ModMenu or dedicated keybinding.

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
