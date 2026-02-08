package com.kylecorry.trail_sense.tools.tides.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import java.time.Duration

class TideMapLayer : GeoJsonLayer<TideGeoJsonSource>(
    TideGeoJsonSource(),
    TideGeoJsonSource.SOURCE_ID,
    isTimeDependent = true,
    refreshInterval = Duration.ofMinutes(10)
)
