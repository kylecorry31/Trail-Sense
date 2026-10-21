Use the Augmented Reality tool to see beacons, paths, and the sun, moon, stars, and planets overlaid onto the real world through your camera.

**Note:** Before you use this tool, check its accuracy against the sun, moon, or a known landmark. If the overlay is not aligned with the real world, calibrate it. See the Calibrate the tool section.

## Use the camera
If you granted the Camera permission, a live camera feed is shown in the background. Otherwise, the background is black. You can control the camera as follows:

- To turn the camera on or off, tap the camera icon in the top-right corner of the screen.
- To zoom while the camera is on, pinch with two fingers or use the slider at the bottom of the screen.

By default, the camera's exposure compensation increases at night. To turn this off, disable **Settings > Augmented Reality > Increase exposure at night**.

## Read the screen
The screen shows the following:

- **Reticle**: A circular reticle is displayed in the center of the screen. Center a marker in the reticle to see more details about it.
- **Grid**: A grid helps you orient yourself. It is aligned with the cardinal directions and spaced at 30 degrees. The grid line for north is orange, and the cardinal directions are displayed over the grid.
- **Orientation**: Your compass bearing and tilt angle are shown at the top of the screen. The tool follows the **Settings > Sensors > Compass > True north** setting.
- **Accuracy**: The compass and GPS accuracy are shown in the bottom-left corner of the screen. Tap a status icon to see more details and instructions for calibrating your compass.

## Choose which layers are visible
You can show or hide the beacons, paths, astronomy, and grid layers. To do this, do one of the following:

- Tap the layer icon in the top-left corner of the screen.
- Change **Settings > Augmented Reality > Visible** for the desired layers.

## Locate a target
If a visible layer supports targeting, a search icon is shown in the top-right corner of the screen. To locate a target, do the following:

1. Tap the search icon to open the **Locate** picker.
2. Select a layer, such as **Beacons** or **Astronomy**.
3. Select a target from the layer.

A panel appears at the bottom of the screen, and the reticle becomes a guidance arrow that points toward the target. To align with the target, move your phone toward the arrow. When the target is in the reticle, the arrow disappears.

The target stays active until you tap the **X** button or select a different target.

## View beacons
Nearby beacons are displayed as markers on the screen. To change the distance at which beacons are shown, set **Settings > Augmented Reality > Beacons > View distance**.

To navigate to a beacon, do one of the following:

- Tap the search icon, select **Beacons**, and select a beacon from the list.
- Center the beacon in the reticle, and tap **Navigate**.
- Tap the beacon.

## View paths
Nearby paths are displayed below your feet. Because of GPS inaccuracy, paths might not line up perfectly with the real world.

Path settings:

- **Settings > Augmented Reality > Paths > View distance**: Changes the distance at which paths are shown.
- **Settings > Augmented Reality > Use path elevation**: Adjusts paths using their recorded elevation, which can make them line up better with hills and valleys. The result depends on the accuracy of the elevation data.
- **Settings > Augmented Reality > Paths > Appearance**: Switches between semi-transparent ribbons on the ground (the default) and lines with markers. You can also change this from the layers panel.

When you are navigating a path, the Paths layer shows the remaining route, even if the saved path is hidden. The route updates as you move. With the ribbon appearance, white arrows on the ribbon point in the direction of travel.

## View the sun and moon
The paths the sun and moon take today are displayed as markers. The sun's path is yellow, and the moon's path is white. A larger marker shows the current position (and the moon phase), and times in the past are lighter. Center the sun or moon in the reticle to see more details, such as the time.

To show the sun and moon when they are below the horizon, enable **Settings > Augmented Reality > Show below horizon**.

To locate an astronomy object, tap the search icon, and select **Astronomy**. You can guide to the sun, moon, meteor showers, planets, and stars. The picker follows your settings for which astronomy objects are visible and shows only some targets at night.

If you opened this tool with the **View in 3D** button in the Astronomy tool, you are prompted to select an astronomy target.

## View stars, planets, and meteor showers
Bright stars, planets, constellations, and meteor showers are shown at night. Center a star in the reticle to see its name and the constellations it belongs to. Star names are IAU proper names or Bayer designations, and constellations are from the IAU.

Settings for stars and planets:

- **Settings > Augmented Reality > Show stars and planets**: Disable to hide stars and planets.
- **Settings > Augmented Reality > Show low brightness stars/planets**: Enable to also show dimmer stars and planets. By default, only the brightest stars and the most visible planets (Venus, Mars, Jupiter, and Saturn) are shown.
- **Settings > Augmented Reality > Show constellations**: Disable to hide constellations.
- **Settings > Augmented Reality > Show below horizon**: Enable to show stars, planets, constellations, and meteor showers below the horizon.

## Preview a different time
To preview a different time of day on a given date, do the following:

1. Tap the clock icon on the right side of the screen to open the date and time slider.
2. Use the slider to choose a time.
3. Tap the clock icon again to return to the live time.

## Navigate with guidance
When you navigate, an arrow around the reticle points toward your target. If beacon, path, or bearing navigation is already active when you open the tool, guidance starts automatically. The arrow behaves as follows:

- **Beacon**: The arrow points toward the beacon.
- **Path**: The arrow points toward the next point along your route and updates as you move.
- **Bearing**: The arrow points along your selected bearing. If **Settings > Navigation > Lock bearing to location** is enabled and the bearing has a starting location, the arrow points toward the fixed destination instead.

Selecting a different beacon for guidance changes your navigation destination to that beacon. Switching to a target that is not a beacon, or closing guidance with the **X** button, leaves your navigation active.

## Calibrate the tool
If the tool is not aligned with the real world, calibrate it by doing the following:

1. Tap the compass icon in the top-left corner of the screen.
2. Center the sun or moon in the reticle.
3. Tap **Calibrate**.

Depending on the quality of your phone's sensors, you might need to repeat the calibration frequently. You can't calibrate on a moonless night.

If your device has a gyroscope, calibrating switches the tool to use the gyroscope for orientation. To turn this off, disable **Settings > Augmented Reality > Use gyroscope after calibration**.

To reset the calibration, long-press the compass icon, or close and reopen the tool.

## Choose a compass source
The tool follows the compass source in **Settings > Sensors > Compass > Source**. If your device does not have a gyroscope, the tool still works, but it is less accurate and might jitter.
