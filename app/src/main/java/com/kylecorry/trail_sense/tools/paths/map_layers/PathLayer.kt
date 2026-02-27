package com.kylecorry.trail_sense.tools.paths.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.sensors.SensorsToolRegistration

class PathLayer :
    GeoJsonLayer<PathGeoJsonSource>(
        PathGeoJsonSource(),
        PathGeoJsonSource.SOURCE_ID,
        refreshBroadcasts = listOf(
            SensorsToolRegistration.BROADCAST_LOCATION_CHANGED,
            PathsToolRegistration.BROADCAST_PATHS_CHANGED
        )
    )