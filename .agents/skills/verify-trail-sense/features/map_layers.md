# Map Layers

Layers display information on the map. You can configure layers in the Navigation, Offline Maps, and Map tools. See the guide for each tool to learn how to open its layer settings. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_map_layers.txt`](../../../../guides/en-US/guide_tool_map_layers.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Map Layers` → tap the tile.
- Layer settings inside Map, Navigation, and Offline Maps.
- Precondition shortcut only: `$T open map`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- Open a map-capable tool: Map (`$T open map`), Navigation, or Offline Maps.

- **Layers sheet.** Open the layers settings per Manage layers. Run `$T ui find Layers`. Layer list with toggles.
- **Toggle.** Turn a layer (e.g. Hillshade) on and reorder it. Run `$T ui find Hillshade`, then `$T ui tap id=<reported id>`. Map redraws.
- **Proof.** Snap the map before and after; `$T pref list layer` shows the stored layer config.

## Gotchas

- Some layers are time dependent; in Map they follow the map time, elsewhere the current time.
- Elevation-based layers need DEM data; a fresh install may show only the built-in low-res data.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
