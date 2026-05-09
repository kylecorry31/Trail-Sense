The Offline Maps tool can be used to import and manage maps for use without an Internet connection. This tool supports using images, PDFs, and Mapsforge (.map) files.

## Finding maps
Map files are not included with Trail Sense and must be created or imported. Here are some common sources of trail maps:

- This site contains a list of Mapsforge and Geospatial PDFs download servers: https://kylecorry.com/Trail-Sense/offline_maps.html
- Check for a physical map near the trailhead or at an information center.
- Check the website for the trail for an image or PDF.

## Importing maps
To import or create a map:

1. Tap the '+' button in the bottom-right corner.
2. Select one of the following options:
    - **File (JPG, PNG, PDF, Mapsforge)**: Choose an image, PDF, or Mapsforge `.map` file from your device. If your PDF contains geospatial data, it will be used for automatic photo map calibration.
    - **Camera**: Capture a picture of a map, ensuring the entire map is within the frame.
    - **Blank**: Generate a blank photo map based on a specified location and map size. By default, it centers the map on your current location. The size is the distance from the center to the corner. No further calibration is required when using this option.
    - **Group**: Create a group for organizing maps.
3. Enter a name for the map and click 'OK'.

Once a map is imported, the original file can be deleted if you want to save space.

If a map is imported from an image or non-geospatial PDF, Trail Sense will open the calibration screen.

## Calibrating photo maps
To calibrate a photo map:

1. Crop the photo to match the map's boundaries by dragging the crop box's corners to the map edges. You can click 'Preview' to visualize the cropped map. Changes to the crop cannot be made after clicking 'Next'.
2. Calibrate the map using two known locations:
    - Input the real-world location in the provided field, such as a trail sign, trailhead, or point of interest.
    - Tap on the map to select where the location is.
    - Utilize 'Previous'/'Next' to switch between calibration points.
    - Once you have two calibration points, click 'Preview' to preview the calibrated map, displaying nearby paths and beacons.
    - You can recenter the map on your screen by clicking the recenter button in the top-right corner.
    - Use pinch-to-zoom or the zoom buttons in the bottom-right to more accurately select locations.
    - Calibration automatically aligns the map with North facing up, with the calculated rotation amount displayed under the map name at the top.
    - At any point, you can save the calibration by clicking 'Next' + 'Done'.
3. Save the calibration by tapping 'Done'.

If Trail Sense reports that calibration is invalid, make sure the two map points are different, the two real-world coordinates are different, and the distance between the selected map points is roughly proportional to their real-world distance.

### Calibrating while hiking
- Point 1: Choose the trail sign where you photographed the map or the trailhead.
- Point 2: Select the first trail fork you encounter. If no trail fork is present, use a point of interest on the map, such as a lake, peak, or landmark.

### Calibrating from the map grid
If your map features gridlines:

1. Find the UTM zone number, which is a one or two-digit number followed by a letter (ex. "16T"). If it's not present on the map, estimate it by entering your approximate latitude and longitude in Trail Sense's Convert tool. For example, you can enter 10 for latitude and 45 for longitude.
2. Locate the Easting and Northing values on the map's borders. They are usually marked and can have 3 to 6 or 7 digits. Add three zeros if there are only 3 digits (e.g., 123 becomes 123000). Easting will be on the top or bottom, Northing will be on the left or right.
3. Look at the grid lines on the map to identify the grid zone where your location is. These lines represent the first digits of your location (e.g., 123000E and 234000N).
4. For increased precision, divide the grid into 10 equal sections vertically and horizontally. Determine which line your location is closest to within the grid. Use a ruler, like the one in Trail Sense, for accuracy. For example, if your location is two-tenths to the right and one-tenth up from the bottom-right corner of the grid, your new location will be 123200E and 234100N.
5. In Trail Sense, enter the UTM location and tap the same point on the photo map. For example, enter "16T 123200E 234100N".
6. Repeat for the second calibration point. For the best accuracy, use a point that is far away from the first point.

The intersections of gridlines are the easiest calibration points.

### Calibrating from an online source
If you have Internet access, you can look up the coordinates of a map feature (e.g., mountain summit, trailhead). Then in Trail Sense, tap the same location on the map and enter the coordinates.

## Viewing maps
Tap a map in the list to open it.

Photo maps open in the map viewer. You can drag to pan, pinch to zoom, or tap the zoom buttons in the bottom-right. To recenter the map on your screen, click the recenter button in the top-right. The map's scale is shown in the bottom-right.

Click the GPS button in the bottom-right to center a photo map on your location. Clicking it again will lock both your location and orientation, while a final click unlocks it. The compass icon in the top-right always points North.

Vector maps open in a preview viewer. You can drag to pan, pinch to zoom, or tap the zoom buttons in the bottom-right. The preview viewer is limited, so use the Map or Navigation tool with the Vector maps layer for regular use.

By default, photo maps align with North roughly facing up so that the map is square with the screen. You can change this so the map is aligned with North facing up by disabling Settings > Offline Maps > 'Keep map facing up'.

By default, Trail Sense loads more tiles for higher quality when viewing a photo map. You can disable this in Settings > Offline Maps > 'High detail mode' to reduce memory usage at the cost of lower quality.

## Layers
Layers are used to display information on the map. For more information on layers, refer to the 'Map Layers' guide.

Photo maps are shown by the "Photo maps" layer. You can't disable this layer when viewing a photo map.

Vector maps (Mapsforge) are shown by the "Vector maps" layer.

## Organizing maps
You can organize maps into groups. To create a group, click the '+' button in the bottom-right of the map list and select 'Group'. Give the group a name and click 'OK'. To add maps to the group, click on the group in the list and follow the map import instructions. The map will be added to the chosen group.

To change the group of an existing map, click the menu button on the map row you want to move, select 'Move to', and choose the target group.

To rename a group, click the menu button on the group row you want to rename, then select 'Rename' and provide a new name.

You can delete a group, along with all maps within it, by clicking the menu button on the group row you wish to remove, then selecting 'Delete'.

## Searching and sorting maps
To search through your maps, use the search bar at the top of the map list. This search includes the current group and all subgroups.

To sort maps by distance, time, or name, click the menu button in the top-right and select 'Sort'.

The preview of a photo map is displayed on the left side of the map row. You can disable this preview in Settings > Offline Maps > 'Show map previews'. Map previews only display for images and PDFs.

## Renaming maps
To rename a map, click the menu button on the map row you want to rename, then select 'Rename' and provide a new name.

You can also rename a photo map by opening the map, clicking the menu button in the top-right, and choosing 'Rename'.

## Map visibility
You can choose which maps are visible on map layers by clicking the eye icon on the right side of the map row.

To show or hide all maps in a group, click the menu button on the group row and select 'Show all' or 'Hide all'.

## Deleting maps
To delete a map, click the menu button on the map row you want to remove, then select 'Delete'.

You can also delete a photo map by opening the map, clicking the menu button in the top-right, and choosing 'Delete'.

## Using beacons on photo maps
If you've created beacons, they will appear on photo maps.

When navigating to a beacon, the distance, direction, and estimated time of arrival (ETA) are displayed at the bottom. A line is drawn from your location to the beacon. To cancel navigation, click the 'X' button. You can open the beacon by tapping the beacon name in the navigation sheet.

You can initiate navigation from a photo map by tapping a beacon or long-pressing a map point and selecting 'Navigate'.

To create a beacon from a photo map, long-press a map point and choose 'Beacon'. This opens the 'Create Beacon' screen with the location filled in.

For further details on beacons, refer to the 'Beacons' guide.

## Using paths on photo maps
If you've created paths, they will be visible on photo maps.

To create a path from a photo map, follow the instructions in the 'Measuring distance on a photo map' section below.

For further details on paths, refer to the 'Paths' guide.

## Measuring distance on a photo map
You can measure distances on a photo map by opening the map, clicking the menu button in the top-right, and selecting 'Measure' or 'Create path'. Tap the map to place markers, and the total distance will be displayed at the bottom. To undo the last marker, click the undo button in the bottom left. Cancel by clicking the 'X' button in the bottom-right. You can also convert the drawn path into a saved path by clicking the 'Create path' button at the bottom.

For a quick measurement from your location to a point, long-press that point on the photo map and click 'Distance'.

## Measuring elevation on a photo map
Long press a location on a photo map to view the elevation from the DEM.

## Recalibrating a photo map
To recalibrate a photo map, open the map, click the menu button in the top-right, and choose 'Calibrate'. Follow the instructions above to recalibrate.

## Changing photo map projection
If your photo map points are not aligning correctly while calibrating, try calibrating again with different points first. If they still do not align, open the map, click the menu button in the top-right, and select 'Change projection'.

Changing the projection while calibrating will clear any unsaved changes.

## Exporting a photo map
To export a photo map, click the menu button on the map row you want to export, then select 'Export'. You can also open the map, click the menu button in the top-right, and choose 'Export'. This action exports the map as a PDF, and if calibrated, it will convert it into a geospatial PDF.

## Printing a photo map
To print a photo map, click the menu button on the map row you want to print, then select 'Print'. You can also open the map, click the menu button in the top-right, and choose 'Print'. This opens the system print dialog, enabling you to print the map.

## Tracing a photo map
To trace a photo map, open it, click the menu button in the top-right, and choose 'Trace'. Move into a shady area or block out the sun using a cloth and place paper over your screen so that you can see the map through the paper. Use a pen or pencil to trace the map, but avoid markers or pens that can bleed through the paper. Be careful not to slide the paper while tracing.

Bottom navigation will be disabled until you turn off trace mode.

When you are finished tracing, click the lock icon in the bottom-right to turn off trace mode.

## Changing photo map resolution
To alter the resolution of a photo map, click the menu button on the map row you want to adjust, then select 'Change resolution'. A dialog will appear, allowing you to switch between low (lowest quality and smallest file size), medium (moderate quality and file size), and high (highest quality and largest file size) resolutions. Keep in mind that changing the resolution is a permanent action and cannot be undone.

By default, Trail Sense will automatically reduce the map resolution on import. To change this, disable Settings > Offline Maps > 'Reduce photo resolution' or 'Reduce PDF resolution'.

## Editing vector map attribution
Some vector map files include attribution. To edit attribution, click the menu button on the map row, select 'Attribution', and provide the attribution text. If attribution is populated, it will be shown on the map when the layer is displayed.

## Quick action
You can enable the Open Photo Map quick action in the settings for the tab where you want it to appear.

To use the quick action, tap the quick action button and the active photo map will be opened and locked to your location. If no photo map is active, the map list will be displayed. Long press the quick action to open the Offline Maps tool.

## Video guide
If you prefer a video guide on using photo maps, here's a video by one of Trail Sense's users: [YouTube: Turn Photos into Navigational Maps with Trail Sense! by DeathfireD](https://www.youtube.com/watch?v=RT4PmBODdzw)

## Accuracy
Maps are only as accurate and current as the map file you imported or calibrated. Always carry a physical map and compass as a backup, and verify important navigation decisions with your surroundings.
