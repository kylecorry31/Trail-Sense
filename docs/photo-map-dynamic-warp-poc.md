# Photo Map Dynamic Warp POC

## Goal

Try dynamic photo-map perspective warping without rewriting and saving the warped map image. The POC stores selected warp bounds on the map calibration and applies the warp while loading map tiles or SubsamplingScaleImageView regions.

## Behavior

- `WarpMapFragment` no longer rewrites `map.filename`.
- The selected perspective/crop bounds are stored as `MapCalibration.warpBounds`.
- `MapCalibration.warped` still indicates that the map should be treated as warped.
- `PhotoMap.unrotatedSize()` returns the virtual warped/cropped image size when `warped == true` and `warpBounds != null`.
- `PhotoMap.calibratedSize()`, `PhotoMapProjection`, `PhotoMapRotationService`, and `MapRotationCalculator` use that virtual size so calibration points are interpreted in the warped image coordinate space.
- Room migration `56 -> 57` adds eight nullable `maps` columns:
  - `warp_top_left_x`, `warp_top_left_y`
  - `warp_top_right_x`, `warp_top_right_y`
  - `warp_bottom_left_x`, `warp_bottom_left_y`
  - `warp_bottom_right_x`, `warp_bottom_right_y`
- Existing maps with no `warpBounds` continue using the old source image/PDF behavior.
- Existing physically warped maps can still load, but this POC does not include a migration to recover original pre-warp source images.

## Tile Loading

- `PhotoMapRegionLoader` treats tile requests as requests in the virtual warped image.
- For warped maps, it inverse-maps the tile corners into the original source image using `PhotoMapWarp.sourceTransform()`.
- It decodes the minimum original-source region that contains those transformed corners.
- It applies `CorrectPerspective` to produce the requested tile-sized bitmap.
- It applies a feathered alpha crop with `PhotoMapWarp.featherCrop()` so tile edges are smooth instead of jagged.
- The tile layer is the strongest proof point: dynamic warp works without saving a warped image.

## Subsample View

- `EnhancedImageView` now exposes `setImageSource()` so callers can provide custom SubsamplingScaleImageView decoders.
- `BasePhotoMapView` registers a custom warped source for warped image-backed maps.
- `WarpedPhotoMapImageSource` stores in-memory source registrations and exposes a custom URI scheme, `trail-sense-photo-map-warp://...`.
- `WarpedPhotoMapImageRegionDecoder` reports the virtual warped size to SubsamplingScaleImageView and decodes warped regions on demand from the original source image.
- `WarpedPhotoMapImageDecoder` supports full-image decode fallback through the same region decoder.
- When this custom source is active, `BasePhotoMapView` uses `PhotoMapProjection(map, usePdf = false)` so markers and calibration points line up with the image-backed warped source.

## Calibration View

- Calibration taps for warped maps are stored in unrotated warped-image percentage coordinates.
- Saved calibration points are drawn through the same source-to-map coordinate conversion used by the view.
- The custom subscale source avoids double-applying the warp in the calibration view.
- After changing warp behavior, recalibrate maps created under earlier POC versions because those saved points may be in the wrong coordinate space.

## Existing Code References

- `app/src/main/java/com/kylecorry/trail_sense/tools/offline_maps/ui/photo_maps/WarpMapFragment.kt`
- `app/src/main/java/com/kylecorry/trail_sense/tools/offline_maps/domain/photo_maps/MapCalibration.kt`
- `app/src/main/java/com/kylecorry/trail_sense/tools/offline_maps/domain/photo_maps/PhotoMapEntity.kt`
- `app/src/main/java/com/kylecorry/trail_sense/tools/offline_maps/domain/photo_maps/PhotoMap.kt`
- `app/src/main/java/com/kylecorry/trail_sense/tools/offline_maps/domain/photo_maps/PhotoMapRotationService.kt`
- `app/src/main/java/com/kylecorry/trail_sense/tools/offline_maps/domain/photo_maps/projections/PhotoMapProjection.kt`
- `app/src/main/java/com/kylecorry/trail_sense/tools/offline_maps/infrastructure/photo_maps/calibration/MapRotationCalculator.kt`
- `app/src/main/java/com/kylecorry/trail_sense/tools/offline_maps/infrastructure/photo_maps/PhotoMapWarp.kt`
- `app/src/main/java/com/kylecorry/trail_sense/tools/offline_maps/infrastructure/photo_maps/tiles/PhotoMapRegionLoader.kt`
- `app/src/main/java/com/kylecorry/trail_sense/shared/views/EnhancedImageView.kt`
- `app/src/main/java/com/kylecorry/trail_sense/tools/offline_maps/ui/photo_maps/BasePhotoMapView.kt`
- `app/src/main/java/com/kylecorry/trail_sense/tools/offline_maps/ui/photo_maps/MapCalibrationView.kt`
- `app/src/main/java/com/kylecorry/trail_sense/tools/offline_maps/ui/photo_maps/WarpedPhotoMapImageSource.kt`
- `app/src/test/java/com/kylecorry/trail_sense/tools/offline_maps/infrastructure/calibration/MapRotationCalculatorTest.kt`

## Notes

- This is still POC-grade. `WarpedPhotoMapImageSource` uses an in-memory registry and does not clean registrations.
- The custom subscale decoder currently supports image-backed maps. PDF-backed dynamic warp is not fully designed.
- Dynamic tile warp is more CPU-intensive than loading a pre-warped saved image. A production design should measure tile load time and memory under large maps.
- `PhotoMapWarp.featherCrop()` is pixel-loop based. It is acceptable for the POC, but a production implementation should consider using Canvas/Shader/Mask operations or caching crop masks.
- The POC adds persistence columns directly to `maps`; a final design should decide whether warp/crop metadata belongs in `MapCalibration`, a separate value object, or a separate table.
- Unresolved: how to migrate maps already physically warped by older app versions if original source images are unavailable.
- Unresolved: whether final behavior should keep PDF support, force image-backed rendering for warped maps, or generate a derived image/PDF preview cache.
- Unresolved: whether cache keys should include a stable warp metadata version for invalidating tile and subscale decoder state.
