---
title: "Settings"
---

## Units
The units can be adjusted in Settings > Units. By default, the units are set based on your device's language settings.

- **Distance**: The unit to display distance measurements.
- **Pressure**: The unit to display barometric pressure measurements.
- **Temperature**: The unit to display temperature measurements.
- **Weight**: The unit to display weight measurements.
- **Coordinate format**: The format to display geographic coordinates.
- **Location share map**: The link to use when sharing a location.
- **Use 24-hour format**: Whether to use a 24-hour clock.
- **Add leading zero to time**: Whether to add a leading zero to the time (for example, 01:00 instead of 1:00).

## Sensors > Compass
Compass settings can be found in Trail Sense Settings > Sensors > Compass. Some devices may not have a compass sensor, but you will still be able to set declination.

### Azimuth
The current compass direction (azimuth) is displayed at the top of the compass settings. As you change settings, it will update to show the new direction.

### Calibrate
The reported accuracy of the compass is shown. If it is low, you can click this option to view instructions on how to calibrate.

### Source

- **Magnetometer + Gyro**: This is the default source and should work on most devices. It uses the magnetometer and gyroscope sensors to determine the direction. When the gyroscope is added in, the reading is typically less noisy. This source is provided by your phone's manufacturer.
- **Magnetometer**: This source only uses the magnetometer sensor to determine the direction. This source is provided by your phone's manufacturer.
- **Magnetometer + Gyro (Custom)**: This source is similar to the Magnetometer + Gyro source, but the calculations are done by Trail Sense. If the Magnetometer + Gyro source is not working correctly, try this source.
- **Legacy (Trail Sense)**: This source is similar to the Magnetometer source, but the calculations are done by Trail Sense. If the Magnetometer source is not working correctly, try this source.
- **Legacy (Android)**: This source is provided by Android and is not recommended to use. It is only included for compatibility with older devices, but could potentially work well on some devices.

### Smoothing
To reduce noise in compass readings, you can increase the smoothing setting. If using a source that has a gyroscope, a value of 1 is recommended. Otherwise, experiment with increasing this value until the compass appears to be working correctly when you rotate your phone. However, if you increase the smoothing too much, Trail Sense may ignore real compass changes.

### True north
To display true north instead of magnetic north, enable the "True north" setting. This will adjust the compass reading to account for magnetic declination, and it will match most maps.

### Magnetic declination
Magnetic declination is the difference between magnetic north and true north. You should ensure this is correct regardless of whether you are using true north or magnetic north.

### Auto adjust declination
Enabling the "Auto adjust declination" setting will automatically adjust the declination based on your location.

### Declination override
If you would prefer to manually set the declination, you can disable the "Auto adjust declination" setting and use the "Declination override" setting to set the declination. You can find the declination for your location on most maps or online. You can also click the "Set override from GPS" option to set the declination based on your current location (only sets it once).

## Sensors > GPS
GPS settings can be found in Trail Sense Settings > Sensors > GPS. If your phone supports it, multiple GNSS systems will be used (such as GPS, GLONASS, Galileo, and BeiDou).

### Location Source
You can choose to mock your location by disabling the "Auto adjust location" setting. This will enable the "Location override" setting where you can manually set your location.

When "Auto adjust location" is enabled, the location will be determined by your device's GPS.

### Require satellite fix
To get a more accurate location, Trail Sense requires at least 4 satellites to have a fix. If you are having trouble getting a location, you can disable this setting to allow for a location with fewer satellites (or if your phone does not support satellite counts).

### Cache
Trail Sense automatically caches your last location to quickly load details in certain tools. You can clear this cache temporarily using the "Clear cache" option in the GPS settings.

## Sensors > Altimeter
The altimeter is used to measure altitude. To adjust altimeter settings, go to Trail Sense Settings > Sensors > Altimeter.

### Elevation
The current elevation is displayed at the top of the altimeter settings. As you change settings, it will update to show the new elevation.

### Source

- **GPS + Barometer**: This is the default source and should work on most devices with a barometer. It combines the GPS and barometer to determine the altitude.
- **GPS**: This source only uses the GPS to determine the altitude. It is less accurate than the GPS + Barometer source.
- **Barometer**: This source only uses the barometer to determine the altitude. It is more accurate than the GPS source, but may drift over time and you will need to calibrate it before use.
- **DEM + Barometer**: Combines a digital elevation model (DEM) with the barometer to determine altitude.
- **DEM**: A digital elevation model (DEM) that determines elevation using your GPS location and a map.
- **Manual**: This source allows you to manually set the altitude. This is useful if you know the altitude of your location and it will not change.

### GPS + Barometer settings

- **Force calibration interval**: You can choose to force the altimeter to recalibrate using the GPS every so often. This is useful to compensate for drift in the barometer, which may be due to changes in weather or location.
- **Continuous calibration**: You can choose to continuously calibrate the altimeter using the GPS. This is useful if you are moving around a lot and want the most accurate altitude reading. When this is enabled, it prefers the barometer reading, but will slightly adjust it based on the GPS reading.
- **Reset calibration**: You can choose to reset the calibration of the altimeter. This is useful if you are in a new location and want to recalibrate the barometer.

### GPS settings

- **Samples**: The number of GPS samples to use when determining the altitude. A higher number will give a more accurate reading, but will take longer to calculate.
- **NMEA elevation correction**: The elevation provided by the GPS needs to be corrected to match the actual elevation. If this setting is enabled, it will use the correction factor provided by the GPS. If it is disabled, it will use the correction factor provided by Trail Sense, which may be more accurate.

### DEM settings

- **Digital elevation model (DEM)**: Clicking this will prompt you to download and import a digital elevation model. Models are available in a variety of sizes at https://kylecorry.com/Trail-Sense/dem.html. Once you import the model, you can delete the file from your device. If you already have a model loaded and select a new model, the old one will be removed. If no model is loaded, a low accuracy built-in model will be used. It is recommended to download one of the larger models if you are able to. The DEM will not be included in the backup due to the size.
- **Remove DEM**: Clicking this will prompt you to delete the DEM file. This is irreversible and you will need to import another file to use the DEM.

### Barometer / manual settings

- **Elevation override**: If you are using the barometer or manual source, you can set the elevation manually. This is useful if you know the elevation of your location. If using the barometer source, this setting is used to calibrate the barometer and will change as you move.
- **Set override from GPS**: You can choose to set the elevation override using the GPS if you don't know your current elevation (only sets it once).
- **Set override from barometer**: If you know the current sea level pressure for your location, you can set the elevation override using the barometer (only sets it once). Only available on devices with a barometer.

## Sensors > Barometer
The barometer is used to measure air pressure. It can be used to predict the weather or determine your altitude. To adjust barometer settings, go to Trail Sense Settings > Sensors > Barometer.

If you have the Weather Monitor (see the Weather tool) enabled, you will see a chart of the barometric pressure history as recorded by your device. The colored line represents the calibrated pressure while the gray line represents the raw pressure. When you adjust the settings, the chart will update to show the new calibrated pressure. The goal is to have the colored line smoothly follow the gray line.

### Sea level pressure
Barometric pressure changes with altitude, which makes it difficult to compare pressure readings from different locations. To make pressure readings more consistent, enable the "Sea level pressure" setting. This will adjust the pressure reading to what it would be at sea level. Using this setting will increase the accuracy of weather predictions.

### Smoothing
To reduce noise in pressure readings, increase the smoothing setting. A value of around 15% is recommended. However, if you increase the smoothing too much, Trail Sense may ignore real pressure changes.

### Factor in temperature
To get a more accurate pressure reading, you can factor in the temperature. This will make the reading more prone to noise from your phone's thermometer, but it will also give you a more accurate reading of the actual air pressure.

### Barometer offset
To calibrate the barometer, you can adjust the barometer offset. After tapping on the 'Barometer offset' setting, enter the current pressure as reported by a reliable source (must match your "sea level pressure" setting - most weather websites report sea level pressure). This will adjust the current pressure to match the reported pressure. Note, if you are using sea level pressure the calibration accuracy will vary with the GPS accuracy.

You can click 'Reset calibration' to reset the calibration of the barometer. This will remove any offset that was previously set.

## Sensors > Thermometer
Thermometer settings can be found in Trail Sense Settings > Sensors > Thermometer.

### Source
On most phones, you should use the historic temperature source as it will be more accurate.

- **Historic** temperature is estimated from 30 years of historical data. It's usually accurate, but not during extreme weather. You can calibrate it if it's wrong for your location.
- **Sensor** temperature is read from the phone's built-in thermometer. It's often inaccurate because it's affected by the phone's CPU and battery. You can calibrate it to improve its accuracy. If you power off your phone for a while, you will get a more accurate reading from the sensor.

### Smoothing
To reduce noise in temperature readings, you can increase the smoothing setting. If using the historic temperature source, this is normally not needed.

If the Weather Monitor (see the Weather tool) is enabled, you will see a chart of the temperature history as recorded by your device. The colored line represents the calibrated temperature while the gray line represents the raw temperature. When you adjust the settings, the chart will update to show the new calibrated temperature. The goal is to have the colored line smoothly follow the gray line.

#### Updating recorded temperatures

If you change the temperature source to "Historic", you can choose to update all previously recorded temperatures. This will replace the old temperatures with the estimated temperature from the historic data. This is useful if you previously recorded temperatures with the sensor and then switched to historic temperatures.

### Calibrating

1. Reset the thermometer calibration in Trail Sense.
2. Put your phone in a cold place for 10 minutes.
3. Note the temperature shown in Trail Sense as the minimum phone temperature.
4. Note the actual temperature of the cold place using an actual thermometer.
5. Put your phone in a warm place for 10 minutes.
6. Note the temperature shown in Trail Sense as the maximum phone temperature.
7. Note the actual temperature of the warm place using an actual thermometer.
8. Go to the Thermometer sensor settings in Trail Sense and enter the minimum and maximum temperatures you recorded, along with the actual temperatures you recorded.

**Caution:** Refer to your phone's manual to identify the designed operating temperatures, do not exceed these when performing calibration.

## Sensors > Camera
Camera settings can be found in Trail Sense Settings > Sensors > Camera.

### Augmented reality projection
The projection method determines how points get displayed onto the camera view. If you are experiencing issues with the points not aligning with the real world, try changing the projection method.

- **Estimated Intrinsics**: This is the default method and should work on most devices. It uses the camera's properties to estimate the projection.
- **Manual Intrinsics**: This is similar to the estimated intrinsics method, except that the calculations are done by the manufacturer. On some devices this does not work correctly.
- **Perspective**: This is a simple projection method that factors in the distance of the point from the camera. It is similar to intrinsics, but may be less accurate in some cases.
- **Linear**: This is the simplest projection method (also the fastest). It does not factor in distances.

## Sensors > Cell signal
Cell signal settings can be found in Trail Sense Settings > Sensors > Cell signal.

### Refresh signal cache
Your phone reports the current cell signal strength, but it may be outdated. You can choose to forcefully refresh the cell signal cache to get the current signal strength. This feature is only used by the Paths tool (Backtrack) when Settings > Paths > Record cell signal is enabled.

## Privacy
Privacy settings can be adjusted in Settings > Privacy.

- **Location**: Clicking this will take you to the GPS settings, where you can choose to use a mocked location.
- **Prevent screenshots**: Prevents screenshots from being taken while the app is open.

**Note**: Trail Sense does not use the Internet and all data is stored locally on your device.

## Errors
Some tools such as Navigation and Astronomy display error banners at the top of the screen. These banners can be disabled in Settings > Errors.

- **Compass unavailable**: The compass is not available on your device.
- **GPS unavailable**: The GPS is not available on your device.
- **Location not set**: The location is mocked but has not been set (uses 0, 0).
- **Compass accuracy**: The compass accuracy is low.
- **GPS timeouts**: The GPS has timed out and was unable to determine your location.

## Experimental
Experimental features can be enabled in Settings > Experimental. These features are not ready for general use and may not work as expected.

### Notification grouping
Android 16 introduces forced notification grouping for apps. This normally puts all of Trail Sense's notifications into one group, which can be hard to read at a glance.

- **System**: Let Android control the grouping.
- **Ungroup all**: Workaround for ungrouping all notifications in Trail Sense.
- **Ungroup alerts**: Workaround for ungrouping alerts (ex. storm, sunset) in Trail Sense.

This is a workaround to bypass the forced behavior on Android 16, so it may not work consistently.

## Backup / restore
When switching to a new device or reinstalling the app, it may be helpful to backup your data and settings. This can be done in Settings > Backup / Restore.

- **Backup**: Creates a backup of your data and settings. This will save as a zip file which is unencrypted and contains location data, so only send it to people you trust.
- **Restore**: Restores a backup of your data and settings. This will permanently overwrite your current data and settings. You can restore from an older version of the app, but cannot restore from a newer version.
- **Automatic backup**: Creates a backup of your data and settings every day in the folder of your choosing. It will only keep the last 2 backups.

## Tools
Settings for each tool can be adjusted in the Settings > Tools section. For more information, see the guide for each tool.

## Theme
Theme related settings can be adjusted in Settings > Theme.

- **Theme**: The theme to use for the app.
  - **Light**: A theme with a white background.
  - **Dark**: A theme with a dark background.
  - **Black**: A theme with a black background.
  - **System**: A theme that follows the system settings.
  - **System (black)**: A theme that follows the system settings with a black background when dark mode is enabled.
  - **Sunrise/sunset**: A theme that changes between light and dark based on the time of day.
  - **Night**: A theme with a black background and a red filter to help maintain night vision.
- **Dynamic colors**: Whether to use your device's dynamic colors in the app, if disabled or unavailable colors in Trail Sense will be a shade of orange.
- **Compass dynamic colors**: Whether to use your device's dynamic colors in the compass (cardinal directions and certain markers on maps).
- **Compact mode**: Whether to use a more compact layout for the bottom bar (thinner and does not display labels).

## About
You can find additional information about Trail Sense in Settings > About.

- **Version**: The version of the app you are using.
- **Privacy policy**: A link to the privacy policy.
- **Email developer**: A link to send an email to the developer for feedback or support.
- **GitHub**: A link to the GitHub repository (source code) for Trail Sense.
- **Licenses**: A list of open source licenses used in Trail Sense.

## Quick action
You can enable the Settings quick action in the settings for the tab where you want it to appear.

To use the quick action, tap the quick action button and settings page for the active tool will be opened. Long press the quick action to open the app settings.

## Safe mode
If Trail Sense crashes multiple times in a row, it will enter Safe Mode. For now, this only disables the Photo Maps layer in the Navigation tool, but more features may be disabled in future updates. You will be able to access Settings and disable any features that may be causing the issue. Safe mode will disable automatically after 20 seconds.
