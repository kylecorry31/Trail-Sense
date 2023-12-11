## How to transfer a location from a map into Trail Sense?

### From a UTM map
1. Find the UTM zone number, which is a one or two-digit number followed by a letter (ex. "16T"). If it's not present on the map, estimate it by entering your approximate latitude and longitude in Trail Sense's Convert tool. For example, you can enter 10 for latitude and 45 for longitude.

2. Locate the Easting and Northing values on the map's borders. They are usually marked and can have 3 to 6 or 7 digits. Add three zeros if there are only 3 digits (ex. 123 becomes 123000). Easting will be on the top or bottom, Northing will be on the left or right.

3. Look at the grid lines on the map to identify the grid zone where your location is. These lines represent the first digits of your location (ex. 123000E and 234000N).

4. For increased precision, divide the grid into 10 equal sections vertically and horizontally. Determine which line your location is closest to within the grid. Use a ruler, like the one in Trail Sense, for accuracy. For example, if your location is two-tenths to the right and one-tenth up from the bottom right corner of the grid, your new location will be 123200E and 234100N.

5. In Trail Sense, enter the UTM location as a new beacon or using the GPS override feature. For example, enter "16T 123200E 234100N".

## How to transfer a location from Trail Sense onto a map?

### To a UTM map
1. Use the Convert tool in Trail Sense to convert your current location (or beacon's location) to UTM format. Note: Leading zeros may be dropped if your map only shows 3 digits (ex. "16T 0123200E 234100N" becomes "16T 123200E 234100N").

2. Identify the number of digits in the UTM grid on the map's border (usually 3 to 6 or 7 digits). Take the same number of digits from both the Easting and Northing values obtained in step 1. For example, if your location is "16T 123200E 234100N" and the map has 3-digit numbers, your Easting would be "123E" and your Northing would be "234N". Note: The number of digits may not be the same for the Easting and Northing.

3. Plot the Easting and Northing values on the map by finding the intersection of the corresponding grid lines. Using the example in step 2, find the 123E grid line (vertical line with the number listed on the top or bottom) and the 234N line (horizontal line with the number listed on the left or right). The intersection point is "123E 234N".

4. For increased precision, divide the grid into 10 equal sections vertically and horizontally. Take the next digit from your UTM coordinate and move that many tenths up or right from the intersection. Repeat this process, dividing each tenth into 10 sections for each digit in your UTM coordinate. For example, suppose you narrowed your location down to the grid with the bottom left corner at "123E 234N". Split the space between 123E and 124E into 10 equal segments, and do the same for 234N and 235N. Take the next digit (fourth digit) from your UTM coordinate, which is 2 for the Easting and 1 for the Northing. Move 2 lines to the right from the intersection for Easting and 1 line up for Northing. This gives you a new intersection. Repeat this process, dividing the grid into smaller sections, until you accurately locate your position.