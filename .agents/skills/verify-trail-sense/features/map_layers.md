# Map Layers

Layers display information on the map. You can configure layers in the Navigation, Offline Maps, and Map tools. See the guide for each tool to learn how to open its layer settings. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_map_layers.txt`](../../../../guides/en-US/guide_tool_map_layers.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- There is no Map Layers tool or tile; it is a guide (Tools → User Guide → Location) and a shared sheet.
- Map: `id=menu_btn` → `Layers` (proven). Also Settings → Map / Navigation → `Layers`, and Offline Maps' photo map viewer.
- Precondition shortcut only: `$T open map`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- Open a map-capable tool: Map (`$T open map`), Navigation, or Offline Maps.

- **Layers sheet.** Open the layers settings per Manage layers. Dismiss the Map disclaimer first, then `$T ui tap id=menu_btn` and `$T ui tap 'text=/^Layers$/'`. The `Layers` sheet lists Location, Beacons, Navigation, Paths, Tides… each with up/down reorder arrows and an expand arrow (driven on 2026-11-21).
- **Toggle.** Turn a layer (e.g. Hillshade) on and reorder it. Layer rows have no ids: tap the layer title (`text=/^Hillshade$/`) to expand it, then toggle `Visible`. Scroll the sheet to reach layers further down. Toggling was not driven.
- **Proof.** Snap the map before and after; `$T pref list layer` shows the stored layer config.

## Gotchas

- Some layers are time dependent; in Map they follow the map time, elsewhere the current time.
- Elevation-based layers need DEM data; a fresh install may show only the built-in low-res data.
