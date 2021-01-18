If you are in an area which doesn't receive GPS signals, you can determine your approximate location via a few different options. Note: This location will not be as accurate as getting a GPS reading, so be careful navigating using these options.

Once an approximate location is determined, you can set it as the GPS override in Trail Sense's GPS settings.

## From a map

If you know your current location on a physical map (ex. a topographical map), you may be able to transfer those coordinates into Trail Sense. Trail Sense currently supports the following coordinate formats: decimal degrees, degrees decimal minutes, degrees minutes seconds, and UTM. Many maps (but not all) will show have coordinates or grids in either decimal degrees or UTM.

### Determining your location from a map in UTM

If your map displays a UTM grid, it is first important to identify the UTM zone this map covers. This should be present on the map as a one or two digit number followed by a letter, for example, 16T. If your map does not have this information, you can get an estimate of what this is by entering your approximate latitude and longitude in the Coordinate Conversion tool in Trail Sense (for the most part, a latitude and longitude without a decimal place is fine, ex. 10, 45). If your map provides the zone number but not a letter, you can use 'C' or 'X' for the letter depending on if you are in the Southern or Northern hemisphere respectively. Once you have the zone, you will need to find the Easting and Northing values.

On a map, the Easting and Northing are typically marked along the borders of the map, and depending on the scale, may be from 3 digits to 6 or 7. If only 3 digits are given (ex. 123), you will likely need to add three zeros to the end of it (ex. 123000) to obtain the correct value. Using the grid lines on the map, you can identify which grid zone your current location is in - those will represent the first digits of your location. To obtain further precision, you can divide the grid into 10 equal sections and determine which line your current position is closest to. For example, you determine your location lies in the grid that starts at 123000E and 234000N (always round down here). You then divide that into 10 sections (both vertical and horizontal), and determine your location is two 10ths to the right and one 10th up from the bottom right corner. Your new location will then be 123200E (added 200, which is 100 _ the number of 10ths right) and 234100N (added 100, which is 100 _ the number of 10ths up). You can further divide the grid cell that your location is to get a more accurate value.

In Trail Sense, you can then use the GPS override feature (Settings > GPS) and enter your current UTM coordinates in the following format (using above example): 16T 123200E 234100N. This should be your current position.

## Triangulation

If you have marked beacons prior to hiking, and you can see at least two of them from your current position (ex. mountain peaks), then you can use triangulation to determine your location. Ideally, the two beacons are at least 60 degrees apart for the best accuracy. You can triangulate using the following steps within Trail Sense:

1. Select the two beacons you can see
2. Point your phone at the first beacon (as you would when navigating), and select "MARK" to record the bearing (this will not include declination).
3. Point your phone at the second beacon (as you would when navigating), and select "MARK" to record the bearing (this will not include declination)
4. Your current location should not be displayed
5. If you have auto GPS disabled (in GPS calibration settings), you will have the option of using the calculated location as your current location.

## Celestial navigation

If you are in the Northern hemisphere, the altitude (or inclination angle) of the North Star (Polaris) is equal to your latitude. Unfortunately, your phone can only give you an approximate estimate of this (using the Inclinometer tool) as typically a device known as a sextant is used.

Determining longitude is a bit more difficult. If your phone has it's time configured properly, you can use the clock tool to get the current time in UTC. You will need to know the exact time of solar noon at your location in UTC time, which can be determined by either when the sun is directly South or North (in the Northern or Southern hemisphere respectively) or when a shadow cast by a stick reaches a minimum length and starts to grow. Once the time of local noon is known in UTC, you can set your GPS override to 0, 0 and get the time of noon at that location using the astronomy tab. Your longitude is the UTC time difference between solar noon at 0, 0 and local noon (represented in hours, with a decimal place) multiplied by 15. In summary, the steps are:

1. Record the time of local solar noon in UTC
2. Set your GPS override to 0, 0
3. Using the astronomy tab, record the time of solar noon
4. Convert the recorded times to hour format: hour + minute / 60 (ex. 13:30 -> 13.5)
5. Calculate the hour offset: noon_at_00 - local_noon
6. Calculate your longitude: hour_offset x 15
