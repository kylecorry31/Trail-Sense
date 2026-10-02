# Proof of concept: 3D map (issue #3950)

## Goal

Show the map as 3D terrain. The user can pan, orbit, and zoom. The terrain shows what is on the 2D map. The sun and moon are shown in the sky.

## Result

It works as a view mode of the Map tool. Menu (⋮) > "3D" opens it. "2D" goes back.

Not tested on a device or emulator (none was connected). It builds.

## How it works

1. The user picks "3D".
2. `MapTerrainLoader.load` takes a snapshot of the `MapView` (`drawToBitmap`). Overlays are turned off for the snapshot (`MapView.drawOverlays`).
3. It reads the DEM for `mapView.mapBounds` with `DEM.getElevationGrid`. The grid is at most about 128 cells on the long side. The resolution is never finer than 1/960 degree (about 115 m).
4. `TerrainMesh.from` builds a triangle mesh. Each vertex has a position, a normal, and a texture coordinate.
   - Scene space is x = east, y = up, z = south. 1 unit = 1 km. Height is multiplied by 2.
   - The texture coordinate comes from `mapView.mapProjection.toPixels(lat, lon)`. This is why the snapshot lines up with the terrain. If the map is rotated, the pixel is rotated by the map azimuth.
5. `MapTerrainLoader` gets the sun and moon position (`AstronomyService`) for the center of the bounds. It uses the map time if one is set.
6. `Terrain3DView` (OpenGL ES 2.0) draws the mesh with the snapshot as texture, then the sun and moon as point sprites.
   - Lighting: ambient + sun + moon (diffuse). The result is scaled to `0.5 + 0.7 * light` so the map stays readable at night.
   - Sky color: blends from night to day using the sun altitude.
   - Camera: orbit camera (yaw, pitch, distance) around a target. One finger orbits, two fingers pan the target on the ground, pinch zooms.

## Changes

| File | Change |
| --- | --- |
| `shared/dem/DEM.kt` | New `ElevationGrid` and public `getElevationGrid(bounds, resolution)`. It wraps the existing private `getElevations`. |
| `tools/map/ui/terrain3d/TerrainMesh.kt` (new) | Builds vertices (with normals from central differences) and indices. |
| `tools/map/ui/terrain3d/MapTerrainLoader.kt` (new) | Snapshot, DEM grid, texture coordinates, sun and moon. Returns `MapTerrain`. |
| `tools/map/ui/terrain3d/Terrain3DView.kt` (new) | `GLSurfaceView`, renderer, shaders, touch handling. |
| `tools/map/ui/MapView.kt` | New `drawOverlays` flag. It skips `layerManager.drawOverlay`. |
| `tools/map/ui/MapAction.kt` | New `Toggle3D` action. |
| `tools/map/ui/MapFragment.kt` | `is3D` state, menu entry, and an effect that loads the terrain. |
| `res/layout/fragment_tool_map.xml` | `Terrain3DView` placed over the map, hidden by default. |

## Limits of the proof of concept

- The texture is a one-time snapshot at screen resolution. It does not update when the map moves. The user must go back to 2D, move the map, and open 3D again.
- The terrain is one fixed patch (the visible bounds). There are no tiles and no level of detail. The texture is blurry when zoomed in.
- When the map is rotated, the corners of the terrain are stretched. The bounds are enlarged to cover the rotation, and the snapshot does not cover them.
- The 2D map is hidden and does not get touches while in 3D. The map buttons (zoom, lock) still show but do nothing.
- Ocean and inland water are not handled. They use the elevation value.
- The mesh uses `Short` indices. The grid must stay under 65,536 vertices.
- No terrain shadows. Lighting is per vertex normal only.
- Vertical exaggeration (2x), grid size, and the menu label ("3D" / "2D") are hardcoded.
- No error handling (for example no DEM data, or GL shader errors).
- The map time button only shows when a time-dependent layer is on, so the user cannot always change the time for 3D.

## Ideas for a production version

- Render the texture from map tiles. This gives sharp detail and lets the texture update while moving. It could reuse the tile layer pipeline.
- Stream DEM tiles with level of detail. Use a larger world extent.
- Hide or replace the 2D map controls in 3D mode. Make the 3D mode its own state in the map (for example, keep the center and zoom in sync with the 2D map).
- Draw paths, beacons, and the user location as 3D objects on the surface, so they stay sharp and can be clamped to the terrain.
- Move the shader and mesh code to a reusable renderer. Consider a library if the scope grows.
- Add an exaggeration setting, a time control, and a compass or north indicator.
