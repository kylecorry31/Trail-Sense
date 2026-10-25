Use the Offline Maps tool to import and manage maps that you can use without an Internet connection. The tool supports images, PDFs, and Mapsforge (.map) files.

**Warning:** Maps are only as accurate and current as the map file you imported or calibrated. Always carry a physical map and compass as a backup, and verify important navigation decisions against your surroundings.

## Find maps
Map files aren't included with Trail Sense. You must create or import them. Common sources of trail maps are:

- A list of Mapsforge and geospatial PDF download servers: https://kylecorry.com/Trail-Sense/offline_maps.html
- A physical map near the trailhead or at an information center.
- The website for the trail, which might have an image or PDF.

## Import a map
To import or create a map, do the following:

1. Tap the **+** button in the bottom-right corner.
2. Select an option.
3. Enter a name for the map.
4. Tap **OK**.

The import options are:

- **File (JPG, PNG, PDF, Mapsforge)**: Choose an image, PDF, or Mapsforge `.map` file from your device. If your PDF contains geospatial data, Trail Sense uses it to calibrate the photo map automatically.
- **Camera**: Take a picture of a map. Make sure the entire map is in the frame.
- **Blank**: Generates a blank photo map from a location and a map size. By default, the map is centered on your current location. The size is the distance from the center to the corner. This option doesn't require calibration.
- **Group**: Creates a group for organizing maps.

If you import a map from an image or a non-geospatial PDF, Trail Sense opens the calibration screen. See the Calibrate a photo map section.

Trail Sense supports Mapsforge maps v3, v4, and v5.

### Keep or delete the original file
After you import a map, you can delete the original file to save space.

If you turn off **Settings > Offline Maps > Copy maps to Trail Sense**, trail maps use the original file instead of a copy. You must keep the file on your device. These maps show an **External** tag in the map list. To copy one into Trail Sense later, tap the menu on the map's row, and select **Copy to Trail Sense**.

## Calibrate a photo map
To calibrate a photo map with two known locations, do the following:

1. Crop the photo to the boundaries of the map. Trail Sense tries to detect the map in the image automatically. If needed, drag the corners of the crop box to the edges of the map.
2. Optional: To start the crop over, tap **Reset**.
3. Optional: To see the cropped map, tap **Preview**.
4. Tap **Next**. You can't change the crop after this step.
5. Enter the real-world location of the first calibration point in the field, such as a trail sign, trailhead, or point of interest.
6. Tap the map to select where that location is.
7. Repeat steps 5 and 6 for the second calibration point. To switch between calibration points, tap **Previous** or **Next**.
8. Optional: When you have two calibration points, tap **Preview** to see the calibrated map with nearby paths and beacons.
9. Tap **Finish calibration** to save the calibration.

To select locations more accurately, pinch to zoom or use the zoom buttons in the bottom-right corner. To recenter the map on your screen, tap the recenter button in the top-right corner.

Calibration automatically aligns the map with north facing up. The calculated rotation is shown under the map name at the top of the screen.

To save an incomplete calibration at any point, tap **Next**, and then tap **Save draft**.

If Trail Sense reports that the calibration is invalid, check the following:

- The two map points are different.
- The two real-world coordinates are different.
- The distance between the selected map points is roughly proportional to their real-world distance.

### Calibrate while hiking
Use the following points:

- **Point 1**: The trail sign where you photographed the map, or the trailhead.
- **Point 2**: The first trail fork you reach. If there is no trail fork, use a point of interest on the map, such as a lake, peak, or landmark.

### Calibrate from the map grid
If your map has gridlines, do the following:

1. Find the UTM zone number, which is a one-digit or two-digit number followed by a letter, for example "16T". If the map doesn't show it, estimate it by entering your approximate latitude and longitude in the Convert tool, for example 10 for latitude and 45 for longitude.
2. Find the Easting and Northing values on the borders of the map. They are usually marked and can have 3 to 7 digits. Easting is at the top or bottom, and Northing is on the left or right.
3. If a value has only 3 digits, add three zeros. For example, 123 becomes 123000.
4. Use the grid lines on the map to find the grid zone that contains your location. The lines show the first digits of your location, for example 123000E and 234000N.
5. Optional: To increase the precision, divide the grid into 10 equal sections vertically and horizontally.
6. Optional: Find the line your location is closest to within the grid. For accuracy, use a ruler, such as the Ruler tool in Trail Sense. For example, if your location is two-tenths to the right and one-tenth up from the bottom-right corner of the grid, your location is 123200E and 234100N.
7. In Trail Sense, enter the UTM location, for example "16T 123200E 234100N", and tap the same point on the photo map.
8. Repeat the steps for the second calibration point. For the best accuracy, use a point that is far from the first point.

The intersections of gridlines are the easiest calibration points.

### Calibrate from an online source
If you have Internet access, look up the coordinates of a map feature, such as a mountain summit or trailhead. In Trail Sense, tap the same location on the map, and enter the coordinates.

### Fix a photo map that doesn't align
If the points on your photo map don't align correctly while you calibrate, do the following:

1. Calibrate again with different points.
2. If the points still don't align, open the map, and tap the menu button in the top-right corner.
3. Select **Change projection**.

Changing the projection while you calibrate clears any unsaved changes.

### Recalibrate a photo map
To recalibrate a photo map, open the map, tap the menu button in the top-right corner, and select **Calibrate**. Then follow the steps in the Calibrate a photo map section.

## View a map
To open a map, tap it in the list.

### View a photo map
Photo maps open in the map viewer. The viewer has the following controls:

- **Pan**: Drag the map.
- **Zoom**: Pinch the map, or tap the zoom buttons in the bottom-right corner.
- **Recenter**: Tap the recenter button in the top-right corner.
- **Scale**: The map's scale is shown in the bottom-right corner.
- **Compass**: The compass icon in the top-right corner always points north.

To center a photo map on your location, tap the GPS button in the bottom-right corner. The button has three states, and each tap moves to the next state:

1. The map is centered on your location.
2. The map is locked to your location and orientation.
3. The map is unlocked.

Photo map settings:

- **Settings > Offline Maps > Keep map facing up**: By default, photo maps are aligned so the map is square with the screen, with north roughly facing up. Disable this setting to align the map with north exactly facing up.
- **Settings > Offline Maps > Show on lock screen while open**: Keeps a photo map visible over your lock screen while it is open.
- **Settings > Offline Maps > High detail mode**: By default, Trail Sense loads more tiles for higher quality when you view a photo map. Disable this setting to reduce memory use at the cost of lower quality.

### View a trail map
Trail maps open in a preview viewer. You can drag to pan, pinch to zoom, or tap the zoom buttons in the bottom-right corner. The preview viewer is limited. For regular use, use the Map or Navigation tool with the Trail maps layer.

## Use layers
Layers display information on the map. For more information, see the Map Layers guide. The following layers are specific to maps:

- **Photo maps layer**: Shows photo maps. You can't disable this layer while you view a photo map.
- **Trail maps layer**: Shows trail maps (Mapsforge).

## Organize maps
You can organize maps into groups.

### Create a group
To create a group, do the following:

1. Tap the **+** button in the bottom-right corner of the map list.
2. Select **Group**.
3. Enter a name for the group.
4. Tap **OK**.

To add maps to the group, tap the group in the list, and follow the steps in the Import a map section. The map is added to the group you chose.

### Move a map to another group
To move a map to another group, do the following:

1. Tap the menu button on the map's row.
2. Select **Move to**.
3. Select the group to move the map to.

### Rename a group
To rename a group, do the following:

1. Tap the menu button on the group's row.
2. Select **Rename**.
3. Enter a new name.

### Delete a group
**Warning:** Deleting a group also deletes all of the maps in it.

To delete a group, tap the menu button on the group's row, and select **Delete**.

## Search and sort maps
To search your maps, use the search bar at the top of the map list. The search includes the current group and all of its subgroups.

To sort maps by distance, time, or name, tap the menu button in the top-right corner, and select **Sort**.

The map list shows a preview of each image or PDF photo map on the left side of its row. To turn off the previews, disable **Settings > Offline Maps > Show map previews**.

### Understand the Draft tag
A **Draft** tag means the map isn't shown on the map layers:

- **Photo map**: Calibrate the map before you can use it.
- **Trail map**: The file is probably missing or invalid. Delete the map, and import it again.

## Rename a map
To rename a map, do the following:

1. Tap the menu button on the map's row.
2. Select **Rename**.
3. Enter a new name.

To rename a photo map from inside the map, open the map, tap the menu button in the top-right corner, and select **Rename**.

## Show or hide maps
To choose which maps are visible on the map layers, tap the eye icon on the right side of the map's row.

To show or hide all maps in a group, tap the menu button on the group's row, and select **Show all** or **Hide all**.

## Delete a map
To delete a map, tap the menu button on the map's row, and select **Delete**.

To delete a photo map from inside the map, open the map, tap the menu button in the top-right corner, and select **Delete**.

## Use beacons on a photo map
Your beacons appear on photo maps.

To navigate to a beacon, do one of the following:

- Tap a beacon.
- Long-press a point on the map, and select **Navigate**.

While you navigate, a line is drawn from your location to the beacon, and the bottom of the screen shows the distance, direction, and estimated time of arrival (ETA). To open the beacon, tap its name in the navigation sheet. To cancel navigation, tap the **X** button.

To create a beacon from a photo map, do the following:

1. Long-press a point on the map.
2. Select **Beacon**.

The **Create Beacon** screen opens with the location filled in. For more information, see the Beacons guide.

## Use paths on a photo map
Your paths appear on photo maps. To create a path from a photo map, see the Measure a distance on a photo map section. For more information, see the Paths guide.

## Measure a distance on a photo map
To measure a distance on a photo map, do the following:

1. Open the map.
2. Tap the menu button in the top-right corner.
3. Select **Measure** or **Create path**.
4. Tap the map to place points.

The total distance is shown at the bottom of the screen. To cancel, tap the **X** button in the bottom-right corner. To move, insert, or undo points, see the Edit the points of a measurement section of the Map guide.

To convert the points you placed into a saved path, tap **Create path** at the bottom of the screen.

To measure the distance from your location to a point, long-press the point on the photo map, and tap **Distance**.

## Check the elevation on a photo map
To see the elevation from the DEM, long-press a location on a photo map.

## Trace a photo map
To trace a photo map onto paper, do the following:

1. Open the map.
2. Tap the menu button in the top-right corner.
3. Select **Trace**.
4. Move into a shady area, or block out the sun with a cloth.
5. Place paper over your screen so you can see the map through the paper.
6. Trace the map with a pen or pencil. Avoid markers or pens that can bleed through the paper, and be careful not to slide the paper.
7. When you finish, tap the lock icon in the bottom-right corner to turn off trace mode.

The bottom navigation is disabled until you turn off trace mode.

## Change the resolution of a photo map
**Warning:** Changing the resolution is permanent and can't be undone.

To change the resolution of a photo map, do the following:

1. Tap the menu button on the map's row.
2. Select **Change resolution**.
3. Select a resolution.

The resolutions are:

- **Low**: The lowest quality and smallest file size.
- **Medium**: Moderate quality and file size.
- **High**: The highest quality and largest file size.

By default, Trail Sense reduces the map resolution when you import a map. To turn this off, disable **Settings > Offline Maps > Reduce photo resolution** or **Reduce PDF resolution**.

## Export a photo map or trail map
To export a map, do one of the following:

- Tap the menu button on the map's row, and select **Export**.
- Open the map, tap the menu button in the top-right corner, and select **Export**.

Photo maps are exported as a PDF. If the photo map is calibrated, it is converted into a geospatial PDF.

You can't export an external trail map.

## Print a photo map
To print a photo map, do one of the following:

- Tap the menu button on the map's row, and select **Print**.
- Open the map, tap the menu button in the top-right corner, and select **Print**.

The system print dialog opens so you can print the map.

## Edit trail map attribution
Some trail map files include attribution. To edit it, do the following:

1. Tap the menu button on the map's row.
2. Select **Attribution**.
3. Enter the attribution text.

If a trail map has attribution, it is shown on the map when the layer is displayed.

## Quick action
To use the Open Photo Map quick action, enable it in the settings for the tab you want it on. The quick action has the following gestures:

- **Tap**: Opens the active photo map and locks it to your location. If no photo map is active, the map list is displayed.
- **Long-press**: Opens the Offline Maps tool.

## Video guide
For a video guide to photo maps, see this video by a Trail Sense user: [YouTube: Turn Photos into Navigational Maps with Trail Sense! by DeathfireD](https://www.youtube.com/watch?v=RT4PmBODdzw)
