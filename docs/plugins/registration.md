# Plugin registration

NOTE: Plugins are only available in debug mode right now and are not production ready. The interfaces are prone to changing without notice. This message will be removed once plugins are stable.

Trail Sense discovers plugin resource services through the Android service action `com.kylecorry.trail_sense.PLUGIN_SERVICE`.

When a user connects a plugin, Trail Sense requests the plugin's registration payload from the `/registration` endpoint. The response is cached by plugin package version code, so changes to the registration payload require a plugin version code update before Trail Sense will request it again.

See the [sample plugin](https://github.com/kylecorry31/Trail-Sense-Sample-Plugin) for an example.

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
          "description": "The plugin route Trail Sense calls to load this layer."
        },
        "name": {
          "type": "string",
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
          "default": null,
          "description": "A user-visible layer description."
        },
        "minZoomLevel": {
          "type": ["integer", "null"],
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
          "default": null,
          "description": "Automatic refresh interval in milliseconds."
        },
        "refreshBroadcasts": {
          "type": "array",
          "default": [],
          "items": {
            "type": "string"
          },
          "description": "Tool broadcasts that should refresh this layer."
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
          "description": "Short attribution text."
        },
        "longAttribution": {
          "type": ["string", "null"],
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

Tool broadcasts can be found in Trail Sense's source code via instances of `ToolRegistration`.

If you change the endpoint, users will need to re-add the layer to see it on their maps.