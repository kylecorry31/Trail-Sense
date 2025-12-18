package com.kylecorry.trail_sense.tools.beacons.map_layers

import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon

class BeaconLayer(private val onBeaconClick: (beacon: Beacon) -> Boolean = { false }) :
    GeoJsonLayer<BeaconGeoJsonSource>(BeaconGeoJsonSource()) {

    override fun onClick(feature: GeoJsonFeature): Boolean {
        val beacon = source.getBeacon(feature)
        return if (beacon != null) {
            onBeaconClick(beacon)
        } else {
            false
        }
    }

    fun setPreferences(prefs: BeaconMapLayerPreferences) {
        percentOpacity = prefs.opacity.get() / 100f
    }

    fun setOutlineColor(color: Int){
        source.setOutlineColor(color)
        invalidate()
    }

    fun highlight(beacon: Beacon?) {
        source.highlight(beacon)
        invalidate()
    }
}