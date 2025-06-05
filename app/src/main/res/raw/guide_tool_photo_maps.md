The Photo Maps tool can be used to convert a photo into a map. It's essential to always carry a physical map as a backup and validate Trail Sense's accuracy. Photo Maps complements other map apps and physical maps, serving as a convenient way to convert photos into maps rather than a replacement for them.

## Creating a map
To create a map, you have three options: take a photo of an existing map, import a file, or generate a blank map.

1. Tap the '+' button located in the bottom-right corner and select your preferred method for importing the map:
    - **Camera**: Capture a picture of your map, ensuring the entire map is within the frame.
    - **File**: Choose a file from your device (JPG, PNG, or PDF). If your PDF contains geospatial data, it will be used for automatic map calibration.
    - **Blank**: Generate a blank map based on a specified location and map size. By default, it centers the map on your current location. The size is the distance from the center to the corner. No further calibration is required when using this option.
2. Enter a name for the map and click 'OK'.
3. Crop the photo to match the map's boundaries by dragging the crop box's corners to the map edges. You can click 'Preview' to visualize the cropped map. Note that changes to the crop cannot be made after clicking 'Next'.
4. Calibrate the map using two known locations:
    - Input the real-world location in the provided field, such as a trail sign, trailhead, or point of interest.
    - Tap on the map to select where the location is.
    - Utilize 'Previous'/'Next' to switch between calibration points.
    - Once you have two calibration points, click 'Preview' to preview the calibrated map, displaying nearby paths and beacons.
    - You can recenter the map on your screen by clicking the recenter button in the top-right corner.
    - Use pinch-to-zoom or the zoom buttons in the bottom-right to more accurately select locations.
    - Calibration automatically aligns the map with North facing up, with the calculated rotation amount displayed under the map name at the top.
    - At any point, you can save the calibration by clicking 'Next' + 'Done'.
5. Save the calibration by tapping 'Done'.

## Calibration tips

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

### Finding a geospatial PDF
A good source for geospatial PDFs is [CalTopo](https://caltopo.com), though there are many other sources available online.

## Using a map
Once you've created a map, you can use it for navigation. The map displays your location, elevation, paths, and beacons.

You can drag to pan, pinch to zoom, or tap the zoom buttons in the bottom-right. To recenter the map on your screen, click the recenter button in the top-right. The map's scale is shown in the bottom-right.

Click the GPS button in the bottom-right to center the map on your location. Clicking it again will lock both your location and orientation, while a final click unlocks it. The compass icon in the top-right always points North.

By default, the map will align with North, roughly facing up so that the map is square with the screen. You can change this so the map is aligned with North facing up (rotated) by disabling Settings > Photo Maps > 'Keep map facing up'. Note: There's currently a bug where panning and zooming are a bit off when this setting is disabled.

### Using beacons and navigating
If you've created beacons, they will appear on the map.

When navigating to a beacon, the distance, direction, and estimated time of arrival (ETA) are displayed at the bottom. A line is drawn from your location to the beacon. To cancel navigation, click the 'X' button in the bottom-right.

You can initiate navigation from the map by tapping a beacon or long-pressing a map point and selecting 'Navigate'.

To create a beacon from the map, long-press a map point and choose 'Beacon.' This opens the 'Create Beacon' screen with the location filled in.

For further details on beacons, refer to the 'Beacons' guide.

### Using paths
If you've created paths, they will be visible on the map.

To create a path from the map, follow the instructions in the 'Measuring distance on a map' section below.

For further details on paths, refer to the 'Paths' guide.

## Measuring distance on a map
You can measure distances on a map by opening the map, clicking the menu button in the top-right, and selecting 'Measure' or 'Create path'. Tap the map to place markers, and the total distance will be displayed at the bottom. To undo the last marker, click the undo button in the bottom left. Cancel by clicking the 'X' button in the bottom-right. You can also convert the drawn path into a saved path by clicking the 'Create path' button at the bottom.

For a quick measurement from your location to a point, long-press that point on the map and click 'Distance'.

## Recalibrating a map
To recalibrate a map, open the map, click the menu button in the top-right, and choose 'Calibrate'. Follow the instructions above to recalibrate.

## Changing map projection
If your map points are not aligning correctly after calibrating (try calibrating again with different points first), consider changing the map projection. To do this, open the map, click the menu button in the top-right, and select 'Change projection'.

## Rename a map
To rename a map, click the menu button on the map row you wish to rename, then select 'Rename' and provide a new name. Alternatively, open the map, click the menu button in the top-right, and choose 'Rename'.

## Map visibility
You can choose which maps are visible on the Navigation tool by clicking the eye icon on the right side of the map row.

## Delete a map
To delete a map, click the menu button on the map row you want to remove, then select 'Delete'. Alternatively, open the map, click the menu button in the top-right, and choose 'Delete'.

## Export a map
To export a map, click the menu button on the map row you want to export, then select 'Export'. Alternatively, open the map, click the menu button in the top-right, and choose 'Export'. This action exports the map as a PDF, and if calibrated, it will convert it into a geospatial PDF.

## Print a map
To print a map, click the menu button on the map row you want to print, then select 'Print'. Alternatively, open the map, click the menu button in the top-right, and choose 'Print'. This opens the system print dialog, enabling you to print the map.

## Trace a map
To trace a map, open it, click the menu button in the top-right, and choose 'Trace'. Move into a shady area or block out the sun using a cloth and place paper over your screen so that you can see the map through the paper. Use a pen or pencil to trace the map, but avoid markers or pens that can bleed through the paper. Be careful not to slide the paper while tracing.

Bottom navigation will be disabled until you turn off trace mode.

When you are finished tracing, click the lock icon in the bottom-right to turn off trace mode.

## Change the resolution of a map
To alter the resolution of a map, click the menu button on the map row you want to adjust, then select 'Change resolution'. A dialog will appear, allowing you to switch between low (lowest quality and smallest file size), medium (moderate quality and file size), and high (highest quality and largest file size) resolutions. Keep in mind that changing the resolution is a permanent action and cannot be undone.

By default, Trail Sense will automatically reduce the map resolution on import. To change this, disable Settings > Photo Maps > 'Reduce photo resolution' or 'Reduce PDF resolution'.

## Organizing maps
You can organize maps into groups. To create a group, click the '+' button in the bottom-right of the map list and select 'Group'. Give the group a name and click 'OK'. To add maps to the group, click on the group in the list and follow the map creation instructions. The map will be added to the chosen group.

To change the group of an existing map, click the menu button on the map row you want to move, select 'Move to', and choose the target group.

To rename a group, click the menu button on the group row you want to rename, then select 'Rename' and provide a new name.

You can delete a group (along with all maps within it) by clicking the menu button on the group row you wish to remove, then selecting 'Delete'.

## Searching for maps
To search through your created maps, use the search bar at the top of the map list. This search encompasses the current group and all subgroups. Additionally, you can sort maps by distance, time, or name by clicking the menu button in the top-right and selecting 'Sort'.

The preview of the map is displayed on the left side of the map row. You can disable this preview in Settings > Photo Maps > 'Show map previews'.

## Quick action
You can enable the Open Photo Map quick action in the settings for the tab where you want it to appear.

To use the quick action, tap the quick action button and the active map will be opened and locked to your location. If no map is active, the map list will be displayed. Long press the quick action to open the Photo Maps tool.

## Video guide
If you prefer a video guide on using Photo Maps, here's a video by one of Trail Sense's users: [YouTube: Turn Photos into Navigational Maps with Trail Sense! by DeathfireD](https://www.youtube.com/watch?v=RT4PmBODdzw)