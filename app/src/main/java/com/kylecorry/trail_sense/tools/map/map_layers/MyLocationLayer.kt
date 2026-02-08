package com.kylecorry.trail_sense.tools.map.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer

class MyLocationLayer : GeoJsonLayer<MyLocationGeoJsonSource>(
    MyLocationGeoJsonSource(),
    layerId = MyLocationGeoJsonSource.SOURCE_ID
)
