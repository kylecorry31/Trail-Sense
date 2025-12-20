Map plugins allow Trail Sense to display additional map layers to users.

Trail Sense will invoke your plugin when it determines it is necessary to retrieve more data. Plugins are expected to handle their own caching to avoid spamming downstream services.

NOTE: Plugins are not currently available and a sample map layer plugin will be provided

TODO: Details on how to tell Trail Sense about these.

TODO: Preferences.

# GeoJSON features

GeoJSON layers allow for the display of vector geometry (points, lines, and areas).

## Request contract

Trail Sense will request a region to be loaded. There will be a way to specify that your source does not vary meaning Trail Sense will only call it once (ex. plugin returns GeoJSON for the whole world). Trail Sense snaps the region to tiles, but will request the entire region when it changes, not just the changed area; you may need to cache.

TODO: More details

## Response contract

The body of the plugin response must be valid GeoJSON (https://geojson.org/). All geometry types are supported.

Trail Sense supports the following properties (additional properties can be sent, but are ignored):

- `name` (`string`, default `null`): The name of the feature. Depending on user settings this may be displayed on the map or when selected.
- `lineStyle` (`string`, default `"solid"`): The style of the line. Valid values are:
  - `solid`
  - `dotted`
  - `arrow`
  - `dashed`
  - `square`
  - `diamond`
  - `cross`
- `color` (`number`, default transparent): The Android compatible ARGB color int of the fill/LineString.
- `strokeColor` (`string`, default null): The Android compatible ARGB color int of the stroke. Does not apply to LineStrings.
- `strokeWeight` (`number`, default `0.5` for Points, `2.25` for LineStrings, `0` for Polygons): The stroke weight in dp.
- `size` (`number`, default `12`): The size of the marker in the units of `sizeUnit`. Only applies to Points.
- `sizeUnit` (`string`, default `"dp"`): The size unit of the marker. Only applies to Points. Valid values are:
  - `px`: Pixels
  - `dp`: Density pixels
  - `m`: Meters
- `icon` (`number`, default null): The ID of the icon to use. Only applies to Points. A full list of icons can be found [here](https://github.com/kylecorry31/Trail-Sense/blob/main/app/src/main/java/com/kylecorry/trail_sense/tools/beacons/domain/BeaconIcon.kt). This may be transitioned over to strings before the official plugin support is released.
- `iconSize` (`number`, default `size`): The size of the icon in dp. Only applies to Points.
- `iconColor` (`number`, default black): The Android compatible ARGB color int of the icon. Only applies to Points.
- `markerShape` (`string`, default `"circle"` if `icon` is not provided, else `"none"`): The shape of the marker. Valid values are:
  - `circle`
  - `none`: Only the icon is rendered
- `isClickable` (`boolean`, default `false`): A boolean that indicates if the feature should be clickable.
- `opacity` (`number`, default `255`): The opacity of the feature (between 0 and 255).

Other properties in the future may include `description` and `coordinateProperties` (properties of coordinates in a LineString/Polygon).

# Tiles

Tile layers allow for the display of images.

## Request contract

Trail Sense will request the tile to load (x, y, z).

TODO: More details

## Response contract

The body of the plugin response must be a bitmap no larger than 256x256 pixels and be in ARGB_8888 format.

TODO: Specify how to say there is no tile available

TODO: Header for specifying pixel format?
