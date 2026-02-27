package com.kylecorry.trail_sense.tools.navigation.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration
import com.kylecorry.trail_sense.tools.sensors.SensorsToolRegistration

class NavigationLayer :
    GeoJsonLayer<NavigationGeoJsonSource>(
        NavigationGeoJsonSource(),
        NavigationGeoJsonSource.SOURCE_ID,
        refreshBroadcasts = listOf(
            SensorsToolRegistration.BROADCAST_LOCATION_CHANGED,
            NavigationToolRegistration.BROADCAST_DESTINATION_CHANGED
        )
    )
