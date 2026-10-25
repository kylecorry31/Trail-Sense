Use the Paths tool to record waypoints while you hike and to follow a trail.

## Record a path with Backtrack
Backtrack records a path automatically while you hike.

### Start and stop Backtrack
To start Backtrack, tap the start button in the bottom-right corner of the screen. A notification shows how far you have traveled.

To stop Backtrack, do one of the following:

- Tap the stop button in the notification.
- Tap the stop button in the bottom-right corner of the screen.

To open the path being recorded, tap the notification. The path shows a recording banner under the map with a stop button.

### Change the recording frequency
To change how often Backtrack records a point, do the following:

1. Tap the time under the Backtrack label in the bottom-left corner.
2. Enter a new interval.

You can also change the interval in **Settings > Paths > Backtrack interval**.

### Reduce power use
When **Keep device awake** is disabled, Backtrack uses the GPS to wake your phone and record a point. To control the power hint for these updates, set **Settings > Paths > Backtrack GPS power usage**. The default, **Inherit from GPS settings**, uses **Settings > Sensors > GPS > GPS power usage**.

When you use reduced power settings, some devices shut down the GPS while stationary or take other actions that can reduce the accuracy of the recorded path.

### Avoid recording points while stationary
To ignore a new point that is too close to the previous point, increase **Settings > Paths > Minimum distance between points**.

### Record cell signal
Backtrack can record the cell signal strength at each point. To turn this on, enable **Settings > Paths > Record cell signal**.

If **Settings > Paths > Last signal beacon** is also enabled, Trail Sense creates a beacon at the last known location where you had a cell signal.

### Keep or delete Backtrack paths
When you stop Backtrack, the path is kept forever. To make Backtrack paths temporary instead, disable **Settings > Paths > Keep path when stopped**.

Temporary paths, and the points of a path that is still being recorded, are deleted after the number of days set in **Settings > Paths > Backtrack history**. The default is 7 days.

To keep a temporary path forever, tap the menu button on the Backtrack path's row, and select **Keep forever**.

## Create a path
To create a path manually, do the following:

1. Tap the **+** button in the bottom-right corner of the screen.
2. Tap **Path**.
3. Enter a name for the path.
4. Tap **OK**.
5. Tap **+ Point** to add a point at your current location. Repeat this for each point you want to add.

To draw a path on the map instead, do the following:

1. Tap the **+** button in the bottom-right corner of the screen.
2. Tap **Draw on map**. The Map tool opens.
3. Tap the map to place points.
4. Tap **Create path** at the bottom of the screen.

To move, insert, or undo points before you create the path, see the Edit the points of a measurement section of the Map guide.

## Import paths from a GPX file
If you have tracks or routes saved in a GPX file, you can import them as paths. To import paths from a GPX file, do the following:

1. Tap the **+** button in the bottom-right corner of the screen.
2. Tap **GPX File**.
3. Select the GPX file.
4. Optional: Uncheck any paths you don't want to import.
5. Tap **OK** to confirm.

Trail Sense creates the paths in the current group.

To simplify imported paths automatically, enable **Settings > Paths > Simplify paths on import**.

## View a path
To open the path details page, tap the path in the list. The page shows the following information:

- **Name**: The name of the path.
- **Group**: The group the path is in. To move the path to a different group, tap the group name.
- **Map**: A map that shows the path and your current location. Drag to move the map, and pinch to zoom. To select a point, tap it on the map. A scale is shown in the bottom-right corner of the map. To make the map larger, tap the expand button in the bottom-left corner of the map.
- **Selected point**: The point you selected on the map. It shows the time, cell signal strength, elevation, and slope of the point. To navigate to the point, create a beacon for it, or delete it, tap the menu button on the point. To deselect the point, tap it on the map.
- **Duration**: The time it took to record the path.
- **Distance**: The distance of the path.
- **Points**: The number of points in the path.
- **Difficulty**: An estimate of the difficulty of the path, calculated from the distance, elevation change, and slope.
- **Ascent**: The total elevation gain of the path.
- **Descent**: The total elevation loss of the path.
- **Lowest point**: The lowest elevation of the path. Tap it to select it on the map.
- **Highest point**: The highest elevation of the path. Tap it to select it on the map.
- **Elevation chart**: A chart of the path's elevation over distance. Tap a point on the chart to select it.

The start of the path is a solid circle, and the end of the path is a flag in a circle.

### View the points of a path
To see the list of points in a path, do the following:

1. Open the path.
2. Tap the menu button in the top-right corner.
3. Select **Points**.

To select a point, tap it. To navigate to a point, create a beacon for it, or delete it, tap the menu button on the point.

## Change the style of a path
To change the appearance of a path, open the path, and use the dropdowns:

- **Line style** and **line color**: Use the dropdowns at the top of the screen.
- **Point style**: Use the dropdown in the top-right corner.

The point styles are:

- **None**: Shows no points.
- **Cell signal**: Shows the cell signal strength at each point.
- **Elevation**: Shows the elevation at each point.
- **Time**: Shows the time each point was recorded.
- **Slope**: Shows the slope at each point.

A key below the path map explains the colors.

To set the defaults, use **Settings > Paths > Default path style** for the line style and **Settings > Paths > Default path color** for the line color.

## Simplify a path
Simplifying a path reduces the number of points in it. This can improve the performance of the path rendering, and it can improve the accuracy of the path by removing GPS errors.

**Warning:** You can't undo a simplification.

To simplify a path, do the following:

1. Open the menu for the path. Tap the menu button on the path's row, or open the path and tap the menu button in the top-right corner.
2. Select **Simplify**.
3. Select the simplification quality. High quality removes fewer points, and low quality removes more points.
4. Check the preview. It shows the original path in transparent gray, the simplified path in the path's color, and the number of points that will be removed.
5. Tap **OK** to apply the simplification.

## Replace the elevations of a path
To replace all elevations on a path with elevations from the digital elevation model (DEM), do the following:

1. Open the path.
2. Tap the menu button in the top-right corner.
3. Select **Replace elevations**.

If loading takes too long, for example because the path has too many points, you can cancel. Your path stays unchanged.

## Smooth path elevations
Trail Sense smooths path elevations to reduce GPS noise. To adjust the smoothing, set **Settings > Paths > Elevation smoothing**. Higher values reduce more noise but can also remove real climbs and descents.

If you have a recent path with elevations, a preview shows its original elevations in gray and the smoothed elevations.

## Navigate with a path
To navigate with a path, do the following:

1. Open the path.
2. Tap a button at the bottom of the screen.

The buttons are:

- **Follow to end**: Follows the path in its recorded direction, from your nearest point on it to its end.
- **Follow to start**: Follows the path in the opposite direction, from your nearest point on it to its start.

The button for the direction you most likely want is highlighted.

To navigate to a specific point on the path, select the waypoint, and use its menu:

- **Follow to point**: Follows the path from your nearest point on it to the selected waypoint, using the shortest route.
- **Navigate directly**: Navigates straight to the selected waypoint without following the path.

When you start navigating, the Navigation tool opens and points you toward the next point on the path. It shows the distance, elevation gain and loss, and ETA, and a progress bar shows how far along the path you are. To open the path, tap the path name in the navigation panel.

## Show or hide paths on maps
To choose which paths are visible on maps, tap the eye icon on the right side of the path's row. Paths are shown on maps that have the Paths layer enabled.

You can also show or hide a path from inside the path. Open the path, tap the menu button in the top-right corner, and select **Show** or **Hide**.

To show or hide all paths in a group, tap the menu button on the group's row, and select **Show all** or **Hide all**.

## Rename a path
To rename a path, do one of the following:

- Tap the menu button on the path's row, and select **Rename**.
- Open the path, tap the menu button in the top-right corner, and select **Rename**.

Then enter a new name.

## Merge paths
To combine two paths, do the following:

1. Tap the menu button on the row of the path you want to merge.
2. Select **Merge**.
3. Choose the path to append it onto.

The two paths are replaced by a single path. It contains the points of the path you chose, followed by the points of the path you selected in step 1. The merged path uses the name, style, and group of the path you chose.

## Delete a path
To delete a path, tap the menu button on the path's row, and select **Delete**.

## Export paths
To export all of the paths in a group to a GPX file, do the following:

1. Open the group you want to export.
2. Tap the menu button in the top-right corner of the screen.
3. Select **Export**.
4. Optional: Unselect any paths you don't want to export.
5. Tap **OK**.

To export a single path, tap the menu button on the path's row, and select **Export**.

## Organize paths
You can organize paths into groups.

### Create a group
To create a group, do the following:

1. Tap the **+** button in the bottom-right corner of the path list.
2. Select **Group**.
3. Enter a name for the group.
4. Tap **OK**.

To view the paths in a group, tap the group in the list. If you create a path or group while a group is open, the new path or group is added to the open group.

### Move a path to another group
To move a path to another group, do the following:

1. Tap the menu button on the path's row.
2. Select **Move to**.
3. Select the group to move the path to.

### Rename a group
To rename a group, do the following:

1. Tap the menu button on the group's row.
2. Select **Rename**.
3. Enter a new name.

### Delete a group
**Warning:** Deleting a group also deletes all of the paths in it.

To delete a group, tap the menu button on the group's row, and select **Delete**.

## Search and sort paths
To search your paths, use the search bar at the top of the path list. The search includes the current group and all of its subgroups.

To sort paths by distance away, time, length, or name, tap the menu button in the top-right corner, and select **Sort**.

## Improve performance
If the Paths tool is slow, enable **Settings > Paths > Fast path rendering**. Paths render faster but look worse.

## Quick action
To use the Backtrack quick action, enable it in the settings for the tab you want it on. The quick action has the following gestures:

- **Tap**: Turns Backtrack on or off.
- **Long-press**: Opens the Paths tool.

## Widgets
You can place the following widgets on your device's home screen or view them in the app:

- **Backtrack**: Shows the distance traveled if Backtrack is on.

## Video guide
For a video guide to Backtrack, see this video by a Trail Sense user: [YouTube: Trail Sense: Learning How To Use BackTrack To Track Cell Service On Hikes by DeathfireD](https://www.youtube.com/watch?v=7IC69LgBcRk)
