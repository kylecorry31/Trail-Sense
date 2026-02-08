package com.kylecorry.trail_sense.tools.beacons.map_layers

import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.luna.coroutines.BackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator

class BeaconLayer :
    GeoJsonLayer<BeaconGeoJsonSource>(
        BeaconGeoJsonSource(),
        BeaconGeoJsonSource.SOURCE_ID
    ) {
    private val navigator = AppServiceRegistry.get<Navigator>()
    private var task = BackgroundTask {
        navigator.destination.collect {
            source.highlight(it)
            invalidate()
            notifyListeners()
        }
    }

    override fun start() {
        super.start()
        task.start()
    }

    override fun stop() {
        super.stop()
        task.stop()
    }

}
