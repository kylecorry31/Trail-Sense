package com.kylecorry.trail_sense.tools.beacons.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration

class BeaconLayer :
    GeoJsonLayer<BeaconGeoJsonSource>(
        BeaconGeoJsonSource(),
        BeaconGeoJsonSource.SOURCE_ID,
        refreshBroadcasts = listOf(
            NavigationToolRegistration.BROADCAST_DESTINATION_CHANGED
        )
    )