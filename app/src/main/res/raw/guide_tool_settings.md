## Units
The units can be adjusted in Settings > Units. By default, the units are set based on your device's language settings.

- **Distance**: The unit to display distance measurements.
- **Pressure**: The unit to display barometric pressure measurements.
- **Temperature**: The unit to display temperature measurements.
- **Weight**: The unit to display weight measurements.
- **Coordinate format**: The format to display geographic coordinates.
- **Location share map**: The link to use when sharing a location.
- **Use 24-hour format**: Whether to use a 24-hour clock.
- **Add leading zero to time**: Whether to add a leading zero to the time (ex. 01:00 instead of 1:00).

## Sensors > GPS
GPS settings can be found in Trail Sense Settings > Sensors > GPS. If your phone supports it, multiple GNSS systems will be used (such as GPS, GLONASS, Galileo, and BeiDou).

### Location Source
You can choose to mock your location by disabling the "Auto adjust location" setting. This will enable the "Location override" setting where you can manually set your location.

When "Auto adjust location" is enabled, the location will be determined by your device's GPS.

### Require satellite fix
To get a more accurate location, Trail Sense requires at least 4 satellites to have a fix. If you are having trouble getting a location, you can disable this setting to allow for a location with fewer satellites (or if your phone does not support satellite counts).

### Cache
Trail Sense automatically caches your last location to quickly load details in certain tools. You can clear this cache temporarily using the "Clear cache" option in the GPS settings.

## Sensors > Barometer
The barometer is used to measure air pressure. It can be used to predict the weather or determine your altitude. To adjust barometer settings, go to Trail Sense Settings > Sensors > Barometer.

If you have the Weather Monitor (see the Weather tool) enabled, you will see a chart of the barometric pressure history as recorded by your device. The colored line represents the calibrated pressure while the gray line represents the raw pressure. When you adjust the settings, the chart will update to show the new calibrated pressure. The goal is to have the colored line smoothly follow the gray line.

### Sea level pressure
Barometric pressure changes with altitude, which makes it difficult to compare pressure readings from different locations. To make pressure readings more consistent, enable the "Sea level pressure" setting. This will adjust the pressure reading to what it would be at sea level. Using this setting will increase the accuracy of weather predictions.

### Smoothing
To reduce noise in pressure readings, increase the smoothing setting. A value of around 15% is recommended. However, if you increase the smoothing too much, Trail Sense may ignore real pressure changes.

### Factor in temperature
To get a more accurate pressure reading, you can factor in the temperature. This will make the reading more prone to noise from your phone's thermometer, but it will also give you a more accurate reading of the actual air pressure.

## Sensors > Thermometer
Thermometer settings can be found in Trail Sense Settings > Sensors > Thermometer.

### Source
On most phones, you should use this historic temperature source as it will be more accurate.

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

## Backup / Restore
When switching to a new device or reinstalling the app, it may be helpful to backup your data and settings. This can be done in Settings > Backup / Restore.

- **Backup**: Creates a backup of your data and settings. This will save as a zip file which is unencrypted and contains location data, so only send it to people you trust.
- **Restore**: Restores a backup of your data and settings. This will permanently overwrite your current data and settings. You can restore from an older version of the app, but cannot restore from a newer version.

## Tools
Settings for each tool can be adjusted in the Settings > Tools section. For more information, see the guide for each tool.

## Theme
Theme related settings can be adjusted in Settings > Theme.

- **Theme**: The theme to use for the app.
  - **Light**: A theme with a white background.
  - **Dark**: A theme with a dark background.
  - **Black**: A theme with a black background.
  - **System**: A theme that follows the system settings.
  - **Sunrise/sunset**: A theme that changes between light and dark based on the time of day.
  - **Night**: A theme with a black background and a red filter to help maintain night vision.
- **Dynamic Colors**: Whether to use your device's dynamic colors in the app, if disabled or unavailable colors in Trail Sense will be a shade of orange.
- **Compass Dynamic Colors**: Whether to use your device's dynamic colors in the compass (cardinal directions and certain markers on maps).
- **Compact Mode**: Whether to use a more compact layout for the bottom bar (thinner and does not display labels).

## About
You can find additional information about Trail Sense in Settings > About.

- **Version**: The version of the app you are using.
- **Privacy policy**: A link to the privacy policy.
- **Email developer**: A link to send an email to the developer for feedback or support.
- **GitHub**: A link to the GitHub repository (source code) for Trail Sense.
- **Licenses**: A list of open source licenses used in Trail Sense.