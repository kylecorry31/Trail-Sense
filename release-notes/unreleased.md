# 8.3.0 Release Notes

This contains a list of the changes in Trail Sense 8.3.0.

## Paths

### Path Navigation

You can now follow paths to their start, end, or a specific point along the path. Once you start following the path, the Navigation and Map tools will show the remaining distance, elevation change, direction to the next point, and estimated time to arrival. The Navigation map layer will show arrows along the path indicating the direction of travel.

TODO: Screenshot of new buttons on path overview screen

TODO: Screenshot of path navigation on the Navigation tool

### DEM Elevations

If you create a path from the map, the elevations will automatically be taken from the digital elevation model (DEM). To replace the elevations with DEM data for an existing path, use the new option on the path details screen.

### Elevation Smoothing

The smoothing of path elevations was improved to work better on paths with noisy elevation data. You can now also change how much smoothing is applied to paths via settings.

TODO: Screenshot of elevation smoothing setting and preview

### Backtrack Improvements

Backtrack should use less battery while recording paths at a frequency below 15 minutes, as it no longer keeps the device awake continuously. Because of this, the default backtrack frequency has been changed to 1 minute, which is more in line with how most users want it to record paths out of the box. A new minimum distance between recorded points setting has also been added so it will avoid recording points when you are taking a break.

The behavior of the notification has been improved to open the path when tapped rather than opening the Paths tool. The screen that opens (path details) will let you stop the recording directly from there. Finally, when you do stop recording, the path will be kept until you manually delete it instead of remaining a temporary path.

TODO: Screenshot of backtrack recording in progress banner

### Draw on Map

Creating a path on the map is now easier and more intuitive. You can drag placed points and insert new points along the path. It also labels the start and end to make it easier to distinguish the two.

A shortcut was added from the Path tool's add path menu to draw the path on the map. This opens the map tool with the "Create path" mode enabled.

TODO: Screenshot of drawing a path on the map

### Simplification Preview

When you attempt to simplify a path, you can now preview what the simplification will look like at different levels before saving it. You will see both your raw path and the simplified version in a dialog.

TODO: Screenshot of simplification dialog

## Navigation

### Navigation Sheet
The navigation sheet was redesigned to improve readability. A new arrow indicator shows the direction to follow, and it will change between green and red depending on whether you are facing the correct direction.

TODO: Screenshot of the redesigned navigation sheet

### Map Layer Improvements

The navigation layer has been enabled by default on the Navigation tool so you can see a path you are following on the compass. It also received a background color setting to make the paths stand out and it will now show the start and end points of the path to help you orient yourself.

TODO: Screenshot of navigation layer with start and end

### Minor Changes

- Canceling navigation is now a single tap with an undo button instead of a confirmation
- The calibrate compass reminder when navigating to a beacon is now only shown when the compass accuracy is low
- Merged the navigation lock screen settings into a single 'Show on lock screen' setting (never, while navigating, or while open)

## Augmented Reality

### Path Ribbons

Paths are now rendered in Augmented Reality as ribbons so you can more clearly tell where they are. You can revert to the old line style within settings. While navigating along a path, the direction of travel will show as arrows rendered onto the ribbon.

TODO: Screenshot of path navigation

### Path and Bearing Navigation

The guidance target now shows the active path or bearing navigation targets when you open the tool.

## GPS

### Accuracy Filter

You can now control what accuracy is acceptable for a GPS reading and how long Trail Sense should wait before accepting a less accurate reading.

TODO: Screenshot of settings screen

### Smoothing

A new GPS smoothing setting was added which will run new GPS readings through a smoothing filter to avoid jitter. This smoothing is done in realtime rather than on recorded readings, so be sure to test this out on a few paths before committing to a setting.

### Minor Changes

- GPS location source is now a selector between GPS and Manual
-- Removed the NMEA altitude correction setting in favor of the built-in model
- Added GPS power usage hint preferences
- Improved GPS reading rejection
- Added timeout to location and elevation pickers
- Increased GPS timeout to 30 seconds
- Improved the stale location indication: the GPS status badge shows searching and the location marker turns gray on maps

## Miscellaneous

- Added a compass update frequency setting to the compass calibration settings so you can lower it to reduce power use
- Added a confirmation before deleting a packing list item
- Improved map tile loading and caching, and failed tiles are now retried
- Reduced update frequency on the Navigation, Flashlight, and White Noise tools to save battery
- Reordered and grouped some settings to make them easier to scan
- Fixed the radar compass sometimes showing a distance of 0
- Fixed the battery charging indicator not updating
- Added the percent of time the device was in deep sleep to the Battery tool
- Fixed a bug where sensors were not paused when the screen was off while navigating with show on lock screen enabled
- Changed default weather monitor frequency to 30 minutes
- Dropped support for Android 6
- Fixed clock tool's GPS time on devices running Android 7, 8, and 9
- Fixed the Photo Maps compass declination waiting for the first GPS reading
- Improved built-in DEM, tide, cell tower, temperature, and precipitation models