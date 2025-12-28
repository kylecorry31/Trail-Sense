Layers are used to display information on the map, they are configurable on the Navigation, Photo Maps, and Map tool. See the respective tool guides for how to access the layer settings.

Click on the layer's name to expand its settings.

Each layer has the following settings:

- **Visible**: Determines if the layer is shown on the map. When it is not visible, the layer header will be grayed out.
- **Opacity**: Determines how transparent the layer is. 0 is fully transparent and 100 is fully opaque.
- **Copy settings to other maps**: Copy the layer settings to other maps. Clicking this will open a dialog where you can choose which maps to copy the settings to.

## Base map
This layer shows a map of the world with colors based on satellite imagery. It is very low resolution and not suitable for navigation.

## Elevation
This layer shows the elevation from the digital elevation model (DEM) as color. You can change the DEM in Settings > Altimeter.

Settings:

- **Color**: The color scale of the pixels. The color will change based on elevation.

## Hillshade
This layer draws shadows to help see elevation in the terrain from the digital elevation model (DEM). You can change the DEM in Settings > Altimeter.

Settings:

- **Draw accurate shadows**: If enabled, shadows will be drawn using the position of the sun and moon, but the 3D appearance may be reduced.

## Photo Maps
This layer shows visible Photo Maps, with the most zoomed-in map appearing on top. You can add new maps in the Photo Maps tool.

Settings:

- **Load PDF tiles**: If enabled, PDF tiles will be loaded for maps that have a PDF version available. This is slower but provides higher resolution maps.

## Contours
This layer shows contour lines generated from the digital elevation model (DEM) and can be used to see the steepness and elevation of map features. You can change the DEM in Settings > Altimeter.

Settings:

- **Show labels**: Determines if contour labels are shown on the map.
- **Color**: The color of the contour lines, some options are color scales which change based on elevation.

## Cell towers
This layer shows nearby cell towers with the accuracy of the tower's location shown as a circle under the tower. These are approximate tower locations from OpenCelliD, Mozilla Location Service, and FCC Antenna Registrations. You can click on a cell tower to navigate to it.

## Paths
This layer shows visible paths. You can add new paths in the Paths tool.

Settings:

- **Background color**: The background color to render behind paths for increased visibility.

## Beacons
This layer shows visible beacons. You can add new beacons in the Beacons tool. You can click on a beacon to navigate to it.

## Navigation
This layer draws a line between your location and the destination point you are navigating to.

## Tides
This layer shows visible tides. You can add new tides in the Tides tool.

Settings:

- **Show modeled tides on coastline**: If enabled, tides will be loaded from the built-in model on the coastline.

## My location
This layer shows your location, which direction you are facing (if you have a compass), and the accuracy of your GPS.

Settings:

- **Show GPS accuracy**: Determines if the GPS accuracy circle is visible.

## Scale
This layer shows a scale bar in the bottom-left corner of the map, which can be used to estimate distances on the map.

This layer is not currently configurable.

## Elevation
This layer shows your elevation in the bottom-right corner of the map.

This layer is not currently configurable.

## Compass
This layer shows a compass in the top-right corner of the map. It shows where true north is on the map.

This layer is not currently configurable.
