Use Settings to customize Trail Sense, calibrate your sensors, and back up your data. Each section below describes a group of settings and the path to open it.

## Units
Open **Settings > Units** to change the units. By default, the units are set from your device's language settings. You can change the following units:

- **Distance**: The unit for distance measurements.
- **Pressure**: The unit for barometric pressure measurements.
- **Temperature**: The unit for temperature measurements.
- **Weight**: The unit for weight measurements.
- **Coordinate format**: The format for geographic coordinates.
- **Location share map**: The link to use when you share a location.
- **Use 24-hour format**: Uses a 24-hour clock.
- **Add leading zero to time**: Adds a leading zero to the time, for example 01:00 instead of 1:00.

## Compass settings
Open **Settings > Sensors > Compass** to change the compass settings. If your device doesn't have a compass sensor, you can still set the declination.

### Check the compass direction and accuracy
The compass settings show the following:

- **Azimuth**: The current compass direction is shown at the top of the compass settings. It updates as you change settings.
- **Calibrate**: Shows the reported accuracy of the compass. If the accuracy is low, tap this option to see instructions for calibrating the compass.

### Choose a compass source
Choose from the following sources:

- **Magnetometer + Gyro**: The default source, which should work on most devices. It uses the magnetometer and gyroscope to determine the direction. Adding the gyroscope typically makes the reading less noisy. Your phone's manufacturer provides this source.
- **Magnetometer**: Uses only the magnetometer to determine the direction. Your phone's manufacturer provides this source.
- **Magnetometer + Gyro (Custom)**: Similar to **Magnetometer + Gyro**, but Trail Sense does the calculations. If **Magnetometer + Gyro** doesn't work correctly, try this source.
- **Legacy (Trail Sense)**: Similar to **Magnetometer**, but Trail Sense does the calculations. If **Magnetometer** doesn't work correctly, try this source.
- **Legacy (Android)**: Provided by Android. It isn't recommended. It is included only for compatibility with older devices, but it might work well on some devices.

### Reduce compass noise
To reduce noise in the compass readings, increase **Smoothing**. If your source uses a gyroscope, a value of 1 is recommended. Otherwise, increase the value until the compass works correctly when you rotate your phone. If you increase the smoothing too much, Trail Sense might ignore real compass changes.

### Change the update frequency
**Update frequency** sets how often the compass updates in the Navigation, Map, Photo Maps, and Augmented Reality tools. The options are:

- **Normal**: Reduces sensor updates to save battery. This is approximately 17 Hz.
- **Fast**: Updates more often for smoother movement but might use more battery. This is approximately 50 Hz.

### Show true north
To show true north instead of magnetic north, enable **True north**. This adjusts the compass reading for magnetic declination, and it matches most maps.

### Set the declination
Magnetic declination is the difference between magnetic north and true north. Make sure it is correct, whether you use true north or magnetic north. The declination settings are:

- **Auto adjust declination**: Adjusts the declination automatically based on your location.
- **Declination override**: Sets the declination manually. To use it, disable **Auto adjust declination**. You can find the declination for your location on most maps or online.
- **Set override from GPS**: Sets the declination override from your current location. It sets the value only once.

## GPS settings
Open **Settings > Sensors > GPS** to change the GPS settings. If your phone supports it, Trail Sense uses multiple GNSS systems, such as GPS, GLONASS, Galileo, and BeiDou.

### Choose a GPS source
Choose from the following sources:

- **GPS**: Determines your location with your device's GPS.
- **Manual**: Lets you set your location manually. Use **Location override** to set the location.

### Tune the GPS
Use the following settings to tune the GPS:

- **Smoothing**: Use the 0–100% slider to reduce GPS position noise. At 0%, smoothing is disabled. Low values closely follow new GPS readings, and 100% provides the strongest smoothing. Higher values can delay responses to changes in movement.
- **Accuracy filter**: Sets the GPS accuracy you prefer before a reading is accepted, and the maximum time Trail Sense waits for it. If the preferred accuracy isn't available in that time, Trail Sense accepts the best recent reading.
- **GPS power usage**: Controls the power hint for GPS updates (**Battery saving**, **Balanced**, or **High accuracy**). When you use reduced power settings, some devices shut down the GPS while stationary or take other actions that can reduce location accuracy.
- **Clear cache**: Trail Sense caches your last location to load details quickly in some tools. This option clears the cache temporarily.

## Altimeter settings
Open **Settings > Sensors > Altimeter** to change the altimeter settings. The altimeter measures altitude.

The current elevation is shown at the top of the altimeter settings. It updates as you change settings.

### Choose an altimeter source
Choose from the following sources:

- **GPS + Barometer**: The default source, which should work on most devices that have a barometer. It combines the GPS and barometer to determine the altitude.
- **GPS**: Uses only the GPS to determine the altitude. It is less accurate than **GPS + Barometer**.
- **Barometer**: Uses only the barometer to determine the altitude. It is more accurate than **GPS**, but it can drift over time, and you must calibrate it before you use it.
- **DEM + Barometer**: Combines a digital elevation model (DEM) with the barometer to determine the altitude.
- **DEM**: Uses a digital elevation model (DEM) to determine the elevation from your GPS location and a map.
- **Manual**: Lets you set the altitude manually. Use this if you know the altitude of your location and it won't change.

### GPS + Barometer settings
The following settings are available:

- **Force calibration interval**: Forces the altimeter to recalibrate with the GPS regularly. Use this to compensate for barometer drift caused by changes in weather or location.
- **Continuous calibration**: Calibrates the altimeter with the GPS continuously. Use this if you move around a lot and want the most accurate altitude. When it is enabled, Trail Sense prefers the barometer reading and adjusts it slightly based on the GPS reading.
- **Reset calibration**: Resets the calibration of the altimeter. Use this when you are in a new location and want to recalibrate the barometer.

### GPS settings for the altimeter
The following setting is available:

- **Samples**: The number of GPS samples used to determine the altitude. A higher number gives a more accurate reading but takes longer to calculate.

### Import a digital elevation model (DEM)
To import a digital elevation model, do the following:

1. Download a model from https://kylecorry.com/Trail-Sense/dem.html.
2. Open **Settings > Sensors > Altimeter**.
3. Tap **Digital elevation model (DEM)**.
4. Select the file you downloaded.

After you import the model, you can delete the downloaded file from your device.

If you already have a model loaded and you select a new model, the old model is removed. If no model is loaded, Trail Sense uses a low accuracy built-in model. The DEM isn't included in backups because of its size.

To delete the DEM, tap **Remove DEM**, and confirm. This can't be undone. To use the DEM again, you must import another file.

### Barometer and manual settings
The following settings are available:

- **Elevation override**: Sets the elevation manually when you use the barometer or manual source. Use this if you know the elevation of your location. With the barometer source, this setting calibrates the barometer and changes as you move.
- **Set override from GPS**: Sets the elevation override from the GPS if you don't know your current elevation. It sets the value only once.
- **Set override from barometer**: Sets the elevation override from the barometer if you know the current sea level pressure for your location. It sets the value only once. This is available only on devices that have a barometer.

## Barometer settings
Open **Settings > Sensors > Barometer** to change the barometer settings. The barometer measures air pressure. You can use it to predict the weather or determine your altitude.

If the weather monitor is on (see the Weather guide), a chart shows the barometric pressure history recorded by your device. The colored line is the calibrated pressure, and the gray line is the raw pressure. The chart updates as you change settings. Adjust the settings until the colored line follows the gray line smoothly.

### Use sea level pressure
Barometric pressure changes with altitude, which makes it hard to compare pressure readings from different locations. To make readings more consistent, enable **Sea level pressure**. This adjusts the reading to what it would be at sea level, and it improves the accuracy of weather predictions.

### Reduce pressure noise
To reduce noise in the pressure readings, increase **Smoothing**. A value of about 15% is recommended. If you increase the smoothing too much, Trail Sense might ignore real pressure changes.

### Factor in temperature
To get a more accurate air pressure reading, enable **Factor in temperature**. The reading is then more affected by noise from your phone's thermometer, but it more accurately reflects the actual air pressure.

### Calibrate the barometer
To calibrate the barometer, do the following:

1. Find the current pressure from a reliable source, such as a weather website. The pressure must match your **Sea level pressure** setting.
2. Open **Settings > Sensors > Barometer**.
3. Tap **Barometer offset**.
4. Enter the pressure you found.

Trail Sense adjusts the current pressure to match the pressure you entered. If you use sea level pressure, the calibration accuracy varies with the GPS accuracy.

To remove the offset you set, tap **Reset calibration**.

## Thermometer settings
Open **Settings > Sensors > Thermometer** to change the thermometer settings.

### Choose a temperature source
On most phones, use the historic temperature source because it is more accurate. Choose from the following sources:

- **Historic**: Estimates the temperature from 30 years of historic data. It is usually accurate, but not during extreme weather. You can calibrate it if it's wrong for your location.
- **Sensor**: Reads the temperature from the phone's built-in thermometer. It is often inaccurate because the phone's CPU and battery affect it. You can calibrate it to improve its accuracy. If you power off your phone for a while, the sensor gives a more accurate reading.

### Reduce temperature noise
To reduce noise in the temperature readings, increase **Smoothing**. You normally don't need this with the historic source.

If the weather monitor is on (see the Weather guide), a chart shows the temperature history recorded by your device. The colored line is the calibrated temperature, and the gray line is the raw temperature. The chart updates as you change settings. Adjust the settings until the colored line follows the gray line smoothly.

### Update recorded temperatures
If you change the temperature source to **Historic**, you can update all previously recorded temperatures. This replaces the old temperatures with the temperature estimated from the historic data. Use this if you recorded temperatures with the sensor and then switched to historic temperatures.

### Calibrate the thermometer
**Warning:** Check your phone's manual for its designed operating temperatures, and don't exceed them during calibration.

To calibrate the thermometer, do the following:

1. Reset the thermometer calibration in Trail Sense.
2. Put your phone in a cold place for 10 minutes.
3. Note the temperature Trail Sense shows as the minimum phone temperature.
4. Note the actual temperature of the cold place with a real thermometer.
5. Put your phone in a warm place for 10 minutes.
6. Note the temperature Trail Sense shows as the maximum phone temperature.
7. Note the actual temperature of the warm place with a real thermometer.
8. Open **Settings > Sensors > Thermometer**.
9. Enter the minimum and maximum temperatures you noted, along with the actual temperatures.

## Camera settings
Open **Settings > Sensors > Camera** to change the camera settings.

### Choose an augmented reality projection
The projection method sets how points are displayed on the camera view. If points don't align with the real world, try changing the projection method. Choose from the following methods:

- **Estimated intrinsics**: The default method, which should work on most devices. It uses the camera's properties to estimate the projection.
- **Manufacturer intrinsics**: Similar to **Estimated intrinsics**, but the manufacturer does the calculations. On some devices, this method doesn't work correctly.
- **Perspective**: A simple method that factors in the distance of the point from the camera. It is similar to intrinsics but might be less accurate in some cases.
- **Linear**: The simplest and fastest method. It doesn't factor in distances.

## Cell signal settings
Open **Settings > Sensors > Cell signal** to change the following cell signal settings:

- **Refresh signal cache**: Your phone reports the current cell signal strength, but the report might be outdated. This option forces a refresh of the cell signal cache to get the current signal strength. The Paths tool (Backtrack) uses this only when **Settings > Paths > Record cell signal** is enabled.

## Sensor details
Open **Settings > Sensors > Sensor details** to open the Sensors tool, which lists the values of the sensors on your device. For more information, see the Sensors guide.

## Privacy
**Note:** Trail Sense doesn't use the Internet, and all data is stored locally on your device.

Open **Settings > Privacy** to change the following privacy settings:

- **Location**: Opens the GPS settings, where you can choose to use a mocked location.
- **Prevent screenshots**: Prevents screenshots while the app is open.

## Errors
Some tools, such as Navigation and Astronomy, show error banners at the top of the screen. To turn off banners, open **Settings > Errors**. The following banners can appear:

- **Compass unavailable**: The compass isn't available on your device.
- **GPS unavailable**: The GPS isn't available on your device.
- **Location not set**: The location is mocked but hasn't been set. The location is 0, 0.
- **Compass accuracy**: The compass accuracy is low.
- **GPS timeouts**: The GPS timed out and couldn't determine your location.
- **Location permission**: Trail Sense doesn't have permission to access your location.

## Back up and restore your data
When you switch to a new device or reinstall the app, back up your data and settings first. Open **Settings > Backup / Restore** to use the following options:

- **Backup**: Creates a backup of your data and settings as an unencrypted zip file. The file contains location data, so send it only to people you trust.
- **Restore**: Restores a backup of your data and settings. **Warning:** This permanently overwrites your current data and settings. You can restore from an older version of the app but not from a newer version.
- **Automatic backup**: Creates a backup of your data and settings every day in the folder you choose. It keeps only the last 2 backups.
- **Include trail maps in backups**: Includes Mapsforge (.map) files in backups. These files can be large.

## Tool settings
The settings for each tool are listed in the Tools section of the settings page. For more information, see the guide for each tool.

The **Tools** entry at the top of that section lets you customize the bottom navigation, quick actions, and in-app widgets. For more information, see the Tools guide.

## Theme
Open **Settings > Theme** to change the following theme settings:

- **Theme**: The theme for the app.
  - **Light**: A theme with a white background.
  - **Dark**: A theme with a dark background.
  - **Black**: A theme with a black background.
  - **System**: A theme that follows the system settings.
  - **System (black)**: A theme that follows the system settings and uses a black background when dark mode is enabled.
  - **Sunrise/sunset**: A theme that changes between light and dark based on the time of day.
  - **Night**: A theme with a black background and a red filter to help maintain night vision.
- **Dynamic colors**: Uses your device's dynamic colors in the app. If this is disabled or unavailable, the colors in Trail Sense are shades of orange.
- **Compass dynamic colors**: Uses your device's dynamic colors in the compass (cardinal directions and certain markers on maps).
- **Default widget theme**: The default theme for widgets. You can override it in the widget customization options on your home screen.
- **Compact mode**: Uses a more compact layout for the bottom bar, which is thinner and doesn't show labels.
- **Show all bottom navigation labels**: Shows labels for all items in the bottom navigation bar instead of only the selected item.
- **Fullscreen in night mode**: Uses fullscreen mode when the night theme is enabled. To show the system UI, swipe from the top.

## About
Open **Settings > About** to find the following information about Trail Sense:

- **Version**: The version of the app you are using.
- **Privacy policy**: A link to the privacy policy.
- **Email developer**: A link to email the developer for feedback or support.
- **GitHub**: A link to the GitHub repository (source code) for Trail Sense.
- **Diagnostics**: Opens the Diagnostics tool, which detects common issues that affect the tools in Trail Sense.
- **Licenses**: A list of the open source licenses used in Trail Sense.

## Recover from crashes with safe mode
If Trail Sense crashes several times in a row, it enters safe mode. Safe mode disables map tile layers and opens to Settings by default, so you can disable any features that might be causing the issue. Safe mode turns off automatically after 20 seconds.

## Experimental settings
Experimental features aren't ready for general use and might not work as expected. To use them, open **Settings > Experimental**.

### Experimental tools
The following experimental options are available:

- **Show metal direction**: Shows the experimental metal direction indicator in the Metal Detector tool. This requires a compass and gyroscope. For more information, see the Metal Detector guide.
- **Cliff Height**: Adds the Cliff Height tool to the tool list.
- **Show pressure forecast on chart**: Adds the forecasted pressure to the pressure chart in the Weather tool.

### Zero shutter lag
Prioritizes capture speed over image quality when you take photos in Trail Sense.

### Use large map tile cache
By default, each map layer keeps a tile cache sized for the current screen. Enable this setting to let each map layer keep more tiles. This can reduce tile reloading while you use maps, but it can significantly increase memory use when several map layers are enabled.

### Use legacy file picker
If the system file picker doesn't work correctly on your device, enable this setting to use the legacy file picker instead. This is only a possible workaround for an upstream issue, and it might not help.

### Widget icon compatibility mode
Enable this if icons are missing from your widgets on some devices.

### Notification grouping
Android 16 forces notification grouping for apps. This normally puts all of Trail Sense's notifications into one group, which can be hard to read at a glance. These options work around that behavior, so they might not work consistently. Choose from the following options:

- **System**: Lets Android control the grouping.
- **Ungroup all**: Ungroups all notifications in Trail Sense.
- **Ungroup alerts**: Ungroups alerts, such as storm and sunset alerts, in Trail Sense.

### Plugins
Plugins let other installed apps add features to Trail Sense. Right now, plugins are limited to additional map layers.

**Warning:** Connect only plugins that you trust.

**Note:** Plugins aren't currently available in the Google Play version of Trail Sense.

To connect a plugin, do the following:

1. Enable **Settings > Experimental > Plugins**. A **Plugins** page appears in Settings.
2. Open **Settings > Plugins**.
3. In the **Available plugins** section, tap the plugin.

To see what features a connected plugin adds, reload it, open its app, or disconnect it, tap the connected plugin.

#### Plugin privacy and security
Trail Sense handles plugins as follows:

- Trail Sense doesn't communicate with plugins you haven't explicitly connected.
- After you connect a plugin, Trail Sense asks the plugin what features it adds. Trail Sense doesn't send any of your data to the plugin.
- When a map layer plugin is connected and its layer is enabled on the map, Trail Sense checks that it has the precise location permission. If it doesn't, you must grant the permission to the plugin, and Trail Sense doesn't load map data from the plugin until you do. After you grant it, Trail Sense sends the viewport bounds of the map to the plugin to load features and tiles.
- If the plugin's signature changes, Trail Sense disconnects the plugin, and you must reconnect it.

## Quick action
To use the Settings quick action, enable it in the settings for the tab you want it on. The quick action has the following gestures:

- **Tap**: Opens the settings page for the active tool.
- **Long-press**: Opens the app settings.
