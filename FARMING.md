# Farming Waypoints

Manage and automate movement with waypoint files for the Garden.

Interactive tool for generating farming waypoint: [Waypoint Generator](https://github.com/SMxNcn/WaypointGenerator)

---

## Commands (`/hwp`)

| Command            | Description                                                                       |
|--------------------|-----------------------------------------------------------------------------------|
| `/hwp list`        | List all waypoint files in `config/hkim/waypoints/`                               |
| `/hwp load <file>` | Load a waypoint file by name (no `.json` extension needed; creates it if missing) |
| `/hwp reload`      | Reload the active waypoint file                                                   |
| `/hwp unload`      | Clear all loaded waypoints                                                        |

---

## Editing Waypoints In-Game

1. Enable **Allow Edits** in the module settings.
2. Aim at a solid block and right-click:

| Action                  | Result                                                                                             |
|-------------------------|----------------------------------------------------------------------------------------------------|
| **Right-click**         | Add a waypoint with no actions, or remove an existing waypoint at that position                    |
| **Shift + Right-click** | Add a waypoint and immediately open the Action Editor, or edit the actions of an existing waypoint |

### Action Editor

- Press the **W / A / S / D** keys to toggle the corresponding movement actions.
- Click anywhere inside the box to toggle **left-click**.
- Press **Done** to save, **Cancel** or **Esc** to discard.

---

## Nuker Behavior

**Setup two loadout slots for a better experience.**

- Default toggle keybind: **X**
- Starts at the first waypoint and applies its action.
- When the player reaches the next waypoint, it advances automatically.
- Pauses on pest spawn, swaps to Mossy loadout, and waits for the player to kill the pest manually.
- After killing a pest, warps back to the Garden and resumes.
- Stops on: world change, teleport, rotation change, or held item swap.

---

## Pest Automation

| Event            | Action                                                         |
|------------------|----------------------------------------------------------------|
| **Pest Spawned** | Stop Nuker, set spawn, swap to Mossy loadout, teleport to plot |
| **Pest Ready**   | Swap to Mantid loadout                                         |
| **Pest Killed**  | Warp to Garden, resume Nuker                                   |

## Future Changes

- Hypixel will **remove** [Pest Disco](https://hypixelskyblock.minecraft.wiki/w/Buzzybee%27s_Fantabulous_Disco_Destination) soon, and the Nuker behavior may change in the future.