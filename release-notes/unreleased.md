This is currently just a running list of what is in the 8.3.0 release to make it easier to write these notes by hand later.

# 8.3.0 Release Notes

This contains a list of the changes in Trail Sense 8.3.0.

## Paths

### Path Navigation

- Added path navigation

### DEM Elevations

- Added the ability to replace a path's elevations with elevations from the digital elevation model
- Automatically populate the DEM elevations for paths created on the map

### Elevation Smoothing

- Added an elevation smoothing setting to Paths, with a preview of the smoothed elevations
- Improved the elevation smoothing algorithm

### Backtrack

- Added a minimum distance between recorded Backtrack points setting
- Backtrack no longer keeps the device awake by default
- Changed default backtrack frequency to 1 minute
- Backtrack paths are now kept when stopped, and the Backtrack history default is longer
- Tapping the Backtrack notification now opens the path being recorded, which can be stopped from the path details

### Draw on Map

- Added a shortcut to create a path by drawing it on the map
- Drawing a path on the map now supports moving points with a magnifier, selecting points, returning to an earlier point, and redo

### Simplification Preview

- Added a preview of the simplified path before applying the simplification

## Navigation

### Map Layer Improvements

- The navigation layer is now enabled by default on the radar compass and has a background color setting
- Added a setting to show the start and end points of the navigation path on the map

### Minor Changes

- Canceling navigation is now a single tap with an undo button instead of a confirmation
- The calibrate compass reminder when navigating to a beacon is now only shown when the compass accuracy is low
- Merged the navigation lock screen settings into a single 'Show on lock screen' setting (never, while navigating, or while open)

## Augmented Reality

### Path Ribbons

- Paths are now rendered in Augmented Reality as ribbons (the line style is still available in settings), with arrows showing the direction of travel while navigating a path

### Path and Bearing Navigation

- Augmented Reality guidance now supports path and bearing navigation and starts automatically when navigation is already active

## GPS

### Accuracy Filter

- Added GPS accuracy filter to GPS settings

### Smoothing

- Added GPS smoothing percentage to GPS settings

### Minor Changes

- GPS location source is now a selector between GPS and Manual
- Removed NMEA altitude correction setting in favor of built-in model
- Added GPS power usage hint preferences
- Improved GPS reading rejection
- Added timeout to location and elevation pickers
- Increased GPS timeout to 30 seconds
- Improved the stale location indication: the GPS status badge shows searching and the location marker turns gray on maps

## Miscellaneous

- Added a compass update frequency setting to the compass calibration settings, used by the Map, Navigation, Photo Maps, and Augmented Reality tools
- Added a confirmation before deleting a packing list item
- Improved map tile loading and caching, and failed tiles are now retried
- Improved the performance of the built-in elevation model
- Reduced update frequency on the Navigation, Flashlight, and White Noise tools to save battery
- Reordered and grouped some settings to make them easier to scan
- Fixed the radar compass sometimes showing a distance of 0
- Fixed the battery charging indicator not updating
- Added percent of time the device was in deep sleep on battery tool
- Fixed a bug where sensors were not paused when the screen was off while navigating with show on lock screen enabled
- Changed default weather monitor frequency to 30 minutes
- Dropped support for Android 6
- Fixed clock tool's GPS time on devices running Android 7, 8, and 9
- Fixed the Photo Maps compass declination waiting for the first GPS reading
