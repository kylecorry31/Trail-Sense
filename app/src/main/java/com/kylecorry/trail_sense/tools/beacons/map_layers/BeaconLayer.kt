package com.kylecorry.trail_sense.tools.beacons.map_layers

import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.trail_sense.shared.andromeda_temp.BackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator

class BeaconLayer(private val onBeaconClick: (beacon: Beacon) -> Boolean = { false }) :
    GeoJsonLayer<BeaconGeoJsonSource>(BeaconGeoJsonSource(), layerId = LAYER_ID) {
        
    private val navigator = AppServiceRegistry.get<Navigator>()
    private var task = BackgroundTask {
        navigator.destination.collect {
            highlight(it)
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

    override fun onClick(feature: GeoJsonFeature): Boolean {
        val beacon = source.getBeacon(feature)
        return if (beacon != null) {
            onBeaconClick(beacon)
        } else {
            false
        }
    }

    fun highlight(beacon: Beacon?) {
        source.highlight(beacon)
        invalidate()
    }

    companion object {
        const val LAYER_ID = "beacon"
    }
}