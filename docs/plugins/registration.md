# Plugin registration

NOTE: Plugins are only available in debug mode right now and are not production ready. The interfaces are prone to changing. This message will be removed once plugins are stable.

Trail Sense discovers plugin resource services through the Android service action `com.kylecorry.trail_sense.PLUGIN_SERVICE`.

When a user connects a plugin, Trail Sense requests the plugin's registration payload from the `/registration` endpoint. The response is cached by plugin package version code, so changes to the registration payload require a plugin version code update before Trail Sense will request it again.

See the [sample plugin](https://github.com/kylecorry31/Trail-Sense-Sample-Plugin) for an example. Once you create a plugin, you can open Trail Sense > Settings > Plugins and connect your plugin by clicking on it and selecting OK on the dialog. Once connected, click on your plugin in the list again and you should see some dtails about your plugin. If you exposed features that Trail Sense recognizes, they will show up on this page. You can click the "Reload" if you make changes to your plugin and want them to appear in Trail Sense. Once you are ready to test, you should see the features available in the respective tool (ex. map layers on the Map tool, in the "Additional layers" picker by default).

The registration payload must be no larger than 64 KiB. Trail Sense ignores registrations that exceed this limit.

## Request

Trail Sense sends a request to:

```text
/registration
```

No payload is sent.

## Response

The response payload must be JSON string matching this schema:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "required": ["features"],
  "properties": {
    "features": {
      "type": "object",
      "properties": {
        "mapLayers": {
          "type": "array",
          "maxItems": 25,
          "default": [],
          "items": {
            "$ref": "#/$defs/mapLayer"
          },
          "description": "Map layers exposed by this plugin."
        }
      },
      "additionalProperties": true
    }
  },
  "additionalProperties": true,
  "$defs": {
    "mapLayer": {
      "type": "object",
      "required": ["endpoint", "name", "layerType"],
      "properties": {
        "endpoint": {
          "type": "string",
          "pattern": "^/[A-Za-z0-9/_-]{1,99}$",
          "minLength": 2,
          "maxLength": 100,
          "description": "The plugin route Trail Sense calls to load this layer."
        },
        "name": {
          "type": "string",
          "minLength": 1
          "maxLength": 100,
          "description": "The user-visible layer name. Trail Sense prefixes this with the plugin name."
        },
        "layerType": {
          "type": "string",
          "enum": ["feature", "tile"],
          "description": "Use feature for GeoJSON layers and tile for bitmap tile layers."
        },
        "attribution": {
          "anyOf": [
            {
              "$ref": "#/$defs/attribution"
            },
            {
              "type": "null"
            }
          ],
          "default": null
        },
        "description": {
          "type": ["string", "null"],
          "maxLength": 1000,
          "default": null,
          "description": "A user-visible layer description."
        },
        "minZoomLevel": {
          "type": ["integer", "null"],
          "minimum": 0,
          "maximum": 20,
          "default": null,
          "description": "The minimum zoom level where this layer should be shown."
        },
        "isTimeDependent": {
          "type": "boolean",
          "default": false,
          "description": "Whether the layer can vary by the requested time."
        },
        "refreshInterval": {
          "type": ["integer", "null"],
          "minimum": 30000,
          "maximum": 3600000,
          "default": null,
          "description": "Automatic refresh interval in milliseconds."
        },
        "shouldMultiply": {
          "type": "boolean",
          "default": false,
          "description": "Whether tile pixels should be multiplied into the map instead of drawn normally. Only applies to tile layers."
        }
      },
      "additionalProperties": true
    },
    "attribution": {
      "type": "object",
      "required": ["attribution"],
      "properties": {
        "attribution": {
          "type": "string",
          "maxLength": 500,
          "description": "Short attribution text."
        },
        "longAttribution": {
          "type": ["string", "null"],
          "maxLength": 2000,
          "default": null,
          "description": "Long-form attribution text."
        },
        "alwaysShow": {
          "type": "boolean",
          "default": false,
          "description": "Whether attribution should always be displayed."
        }
      },
      "additionalProperties": true
    }
  }
}
```

If you change the endpoint, users will need to re-add the layer to see it on their maps.

Trail Sense skips map layers with invalid endpoints or blank names. Values longer than the documented limits are truncated.
