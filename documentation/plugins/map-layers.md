Map plugins allow Trail Sense to display additional map layers to users.

Trail Sense will invoke your plugin when it determines it is necessary to retrieve more data. Plugins are expected to handle their own caching to avoid spamming downstream services.

NOTE: Plugins are not currently available and a sample map layer plugin will be provided

TODO: Details on how to tell Trail Sense about these.

TODO: Preferences.

# GeoJSON features

GeoJSON layers allow for the display of vector geometry (points, lines, and polygons).

## Request contract

Trail Sense will request a region to be loaded. There will be a way to specify that your source does not vary meaning Trail Sense will only call it once (ex. plugin returns GeoJSON for the whole world). Trail Sense snaps the region to tiles, but will request the entire region when it changes, not just the changed area; you may need to cache.

TODO: More details

## Response contract

The body of the plugin response must be valid GeoJSON (https://geojson.org/). All geometry types are supported but GeometryCollection is not recommended since the feature properties vary by geometry type. Multi* geometry values are flattened into their base geometry type by Trail Sense, so use the property type of the base (ex. MultPoint -> Point).

The following JSON schemas outline what you can provide for the `properties` value for each geometry type.  Other properties in the future may include `description` and `coordinateProperties` (properties of coordinates in a LineString/Polygon).

### Point properties

Used for Point geometries with marker support:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "title": "Trail Sense Point Feature Properties",
  "properties": {
    "name": {
      "type": ["string", "null"],
      "default": null,
      "description": "The name of the feature. Depending on user settings this may be displayed on the map or when selected."
    },
    "color": {
      "type": "integer",
      "default": 0,
      "description": "The Android compatible ARGB color int of the marker fill."
    },
    "strokeColor": {
      "type": ["integer", "null"],
      "default": null,
      "description": "The Android compatible ARGB color int of the stroke."
    },
    "strokeWeight": {
      "type": "number",
      "default": 0.5,
      "description": "The stroke weight in dp."
    },
    "size": {
      "type": "number",
      "default": 12,
      "description": "The size of the marker in the units of sizeUnit."
    },
    "sizeUnit": {
      "type": "string",
      "enum": ["px", "dp", "m"],
      "default": "dp",
      "description": "The size unit of the marker. px = pixels, dp = density pixels, m = meters"
    },
    "icon": {
      "type": ["integer", "null"],
      "default": null,
      "description": "The ID of the icon to use. A full list of icons can be found [here](https://github.com/kylecorry31/Trail-Sense/blob/main/app/src/main/java/com/kylecorry/trail_sense/tools/beacons/domain/BeaconIcon.kt). This may be transitioned over to strings before the official plugin support is released."
    },
    "iconSize": {
      "type": "number",
      "description": "The size of the icon in dp. Default: same as size property."
    },
    "iconColor": {
      "type": "integer",
      "default": -16777216,
      "description": "The Android compatible ARGB color int of the icon."
    },
    "markerShape": {
      "type": "string",
      "enum": ["circle", "none"],
      "description": "The shape of the marker. Default: 'circle' if icon is not provided, else 'none'. When 'none', only the icon is rendered."
    },
    "isClickable": {
      "type": "boolean",
      "default": false,
      "description": "A boolean that indicates if the feature should be clickable."
    },
    "opacity": {
      "type": "integer",
      "minimum": 0,
      "maximum": 255,
      "default": 255,
      "description": "The opacity of the feature (between 0 and 255)."
    }
  },
  "additionalProperties": true
}
```

### LineString properties

Used for LineString geometries:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "title": "Trail Sense LineString Feature Properties",
  "properties": {
    "name": {
      "type": ["string", "null"],
      "default": null,
      "description": "The name of the feature. Depending on user settings this may be displayed on the map or when selected."
    },
    "lineStyle": {
      "type": "string",
      "enum": ["solid", "dotted", "arrow", "dashed", "square", "diamond", "cross"],
      "default": "solid",
      "description": "The style of the line."
    },
    "color": {
      "type": "integer",
      "default": 0,
      "description": "The Android compatible ARGB color int of the line."
    },
    "strokeWeight": {
      "type": "number",
      "default": 2.25,
      "description": "The stroke weight in dp."
    },
    "isClickable": {
      "type": "boolean",
      "default": false,
      "description": "A boolean that indicates if the feature should be clickable."
    },
    "opacity": {
      "type": "integer",
      "minimum": 0,
      "maximum": 255,
      "default": 255,
      "description": "The opacity of the feature (between 0 and 255)."
    }
  },
  "additionalProperties": true
}
```

### Polygon properties

Used for Polygon geometries:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "title": "Trail Sense Polygon Feature Properties",
  "properties": {
    "name": {
      "type": ["string", "null"],
      "default": null,
      "description": "The name of the feature. Depending on user settings this may be displayed on the map or when selected."
    },
    "color": {
      "type": "integer",
      "default": 0,
      "description": "The Android compatible ARGB color int of the fill."
    },
    "strokeColor": {
      "type": ["integer", "null"],
      "default": null,
      "description": "The Android compatible ARGB color int of the stroke."
    },
    "strokeWeight": {
      "type": "number",
      "default": 0,
      "description": "The stroke weight in dp."
    },
    "isClickable": {
      "type": "boolean",
      "default": false,
      "description": "A boolean that indicates if the feature should be clickable."
    },
    "opacity": {
      "type": "integer",
      "minimum": 0,
      "maximum": 255,
      "default": 255,
      "description": "The opacity of the feature (between 0 and 255)."
    }
  },
  "additionalProperties": true
}
```

# Tiles

Tile layers allow for the display of images.

## Request contract

Trail Sense will request the tile to load (x, y, z).

TODO: More details

## Response contract

The body of the plugin response must be a bitmap no larger than 256x256 pixels and be in ARGB_8888 format.

TODO: Specify how to say there is no tile available

TODO: Header for specifying pixel format?
