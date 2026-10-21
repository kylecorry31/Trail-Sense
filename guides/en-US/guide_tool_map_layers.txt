Layers display information on the map. You can configure layers in the Navigation, Offline Maps, and Map tools. See the guide for each tool to learn how to open its layer settings.

Some layers are time dependent, and the sections below mark them. In the Map tool, you can change the map time to see these layers at different times. In other tools, they always show the current time.

## Manage layers
### Reorder layers
In the layer settings panel, a layer higher in the list is drawn on top of the layers below it. To change the order, use the arrows on the left side of each layer.

### Change the settings of a layer
To expand the settings of a layer, tap its name.

Each layer has the following settings:

- **Visible**: Shows or hides the layer on the map. When a layer is hidden, its header is grayed out.
- **Opacity**: Sets how transparent the layer is. 0 is fully transparent, and 100 is fully opaque.
- **Copy settings to other maps**: Copies the layer settings to other maps. Tap it, and choose the maps to copy the settings to in the dialog.
- **Remove layer**: Removes the layer from the active layer list. To add it again, tap **Additional layers**.

### Add layers
To add a layer, tap **Additional layers** at the bottom of the layers list.

Connected plugins can also provide map layers. Plugins are experimental. To use them, do the following:

1. Enable **Settings > Experimental > Plugins**.
2. Connect the plugin from **Settings > Plugins**.

## Base map
Shows a map of the world with colors based on satellite imagery. It has very low resolution and isn't suitable for navigation.

## Trail maps
Shows the visible trail maps from the Offline Maps tool.

Settings:

- **Manage maps**: Opens the Offline Maps tool.

## Elevation
Shows the elevation from the digital elevation model (DEM) as color.

Settings:

- **Digital elevation model (DEM)**: Opens **Settings > Altimeter**, where you configure the DEM source.
- **Color**: The color scale of the pixels. The color changes based on elevation.
- **High resolution**: Increases the resolution when you are zoomed out. This might be slower.

## Hillshade
Draws shadows from the DEM to help you see the elevation of the terrain.

Settings:

- **Digital elevation model (DEM)**: Opens **Settings > Altimeter**, where you configure the DEM source.
- **Draw accurate shadows**: Draws shadows using the position of the sun and moon (time dependent). This might reduce the 3D appearance.
- **Multi-direction shading**: Calculates shadows from multiple light directions to enhance terrain details.
- **High resolution**: Increases the resolution when you are zoomed out. This might be slower.

## Slope
Colors the terrain by steepness using the DEM.

Settings:

- **Digital elevation model (DEM)**: Opens **Settings > Altimeter**, where you configure the DEM source.
- **Color**: The color scale of the pixels. The color changes based on the steepness of the slope.
  - **Green to red**: Green for flat areas, yellow for moderate slopes, and red for steep slopes.
  - **White to red**: White for flat areas, yellow for moderate slopes, and red for steep slopes.
  - **Grayscale**: Black for flat areas to white for steep slopes.
- **Smooth**: Smooths the slope colors so you can see gradual changes.
- **Hide flat ground**: Makes flat ground (a slope below 10 degrees) invisible.
- **High resolution**: Increases the resolution when you are zoomed out. This might be slower.

## Ruggedness
Colors the terrain by how much the elevation changes around it (the ruggedness index). Low ruggedness is green, moderate ruggedness is yellow or orange, and highly rugged terrain is red.

Settings:

- **Digital elevation model (DEM)**: Opens **Settings > Altimeter**, where you configure the DEM source.
- **High resolution**: Increases the resolution when you are zoomed out. This might be slower.

## Aspect
Colors the terrain by aspect (the direction the slope is facing) using the DEM. Green is north, blue is east, red is south, and orange is west.

Settings:

- **Digital elevation model (DEM)**: Opens **Settings > Altimeter**, where you configure the DEM source.
- **High resolution**: Increases the resolution when you are zoomed out. This might be slower.

## Photo maps
Shows the visible photo maps, with the most zoomed-in map on top. To add maps, use the Offline Maps tool.

Settings:

- **Load PDF tiles**: Loads PDF tiles for maps that have a PDF version. This is slower but provides higher resolution maps.

## Contours
Shows contour lines generated from the DEM. Use them to see the steepness and elevation of map features.

Settings:

- **Digital elevation model (DEM)**: Opens **Settings > Altimeter**, where you configure the DEM source.
- **Show labels**: Shows or hides the contour labels.
- **Color**: The color of the contour lines. Some options are color scales that change based on elevation.

## Solar eclipse
Shows the solar eclipse visibility (time dependent). Black is a total eclipse, and gray is a partial eclipse.

Settings:

- **Smooth**: Smooths the eclipse visibility so you can see gradual changes. This makes it harder to see where the total eclipse is.
- **Show path**: Shows the path of the eclipse instead of the current visibility.

## Lunar eclipse
Shows the lunar eclipse visibility (time dependent). Orange means the eclipse is visible.

Settings:

- **Show path**: Shows the path of the eclipse instead of the current visibility.

## Night
Shows where it is nighttime based on the sun's altitude (time dependent). Black means the sun is well below the horizon, and gray means the sun is just below the horizon.

Settings:

- **Smooth**: Smooths the darkness so you can see gradual changes between day and night.

## Cell towers
Shows nearby cell towers. A circle under each tower shows the accuracy of the tower's location. The tower locations are approximate and come from OpenCelliD, Mozilla Location Service, and FCC Antenna Registrations. To navigate to a cell tower, tap it.

## Paths
Shows the visible paths. To add paths, use the Paths tool.

Settings:

- **Background color**: The background color drawn behind paths to make them easier to see.

## Beacons
Shows the visible beacons. To add beacons, use the Beacons tool. To navigate to a beacon, tap it.

## Navigation
Draws a line between your location and the destination you are navigating to.

Settings:

- **Show start and end points**: Shows the start and end points of the navigation path on the map. The start is a solid circle, and the end is a flag in a circle.
- **Background color**: The background color drawn behind the navigation path to make it easier to see.

## Tides
Shows the visible tides (time dependent). To add tides, use the Tides tool.

Settings:

- **Show modeled tides on coastline**: Loads tides from the built-in model on the coastline.
- **Show tide phase**: Shows the current phase direction of the tide instead of the high or low tide icon.

## Sightings
Shows your field guide sightings that have location data. The icon represents the classification of the field guide entry, such as plant, bird, or mammal. To navigate to a sighting, tap it.

Settings:

- **Show images**: Shows the image of the field guide page instead of the category icon.
- **Color by classification**: Colors the marker based on the classification of the page, such as animal or plant.

## Location
Shows your location, the direction you are facing (if your device has a compass), and the accuracy of your GPS. The location marker turns gray if your location is stale.

Settings:

- **Show GPS accuracy**: Shows or hides the GPS accuracy circle.
- **Large location indicator**: Makes the location marker larger.

## Map scale
Shows a scale bar in the bottom-left corner of the map. Use it to estimate distances on the map.

This layer isn't currently configurable.

## My elevation
Shows your elevation in the bottom-right corner of the map.

This layer isn't currently configurable.

## Compass
Shows a compass in the top-right corner of the map. The compass shows where true north is on the map.

This layer isn't currently configurable.
