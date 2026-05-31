# Mapsforge

Trail Sense will support Mapsforge maps as the vector map format. This decision was made because the library is very small (under half a Megabyte), stable, customizable, and the maps are readily available to download.

A fork of the Mapsforge library will be used so that fixes or changes that optimize it for Trail Sense can be made. If the change is generic enough, I will attempt to contribute it upstream. This fork will only support Trail Sense.

No other vector map formats will have native support in Trail Sense, and users must use plugins to use different map types.

## Status

accepted

## Consequences

- Users need to download map files from an external source
- Mapsforge could stop publishing map files and I would need to figure out how to generate and host the maps myself or switch to a different format
- I will need to maintain a fork of the library and keep in sync with upstream