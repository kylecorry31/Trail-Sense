package com.kylecorry.trail_sense.shared.dem.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer

class ContourLayer : GeoJsonLayer<ContourGeoJsonSource>(
    ContourGeoJsonSource(),
    ContourGeoJsonSource.SOURCE_ID,
    minZoomLevel = 13
)
