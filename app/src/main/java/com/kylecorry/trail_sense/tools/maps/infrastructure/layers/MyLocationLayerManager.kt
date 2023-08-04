package com.kylecorry.trail_sense.tools.maps.infrastructure.layers

import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.ui.layers.MyLocationLayer

class MyLocationLayerManager(private val layer: MyLocationLayer): BaseLayerManager() {
    override fun start() {
    }

    override fun stop() {
    }

    override fun onLocationChanged(location: Coordinate, accuracy: Float?) {
        super.onLocationChanged(location, accuracy)
        layer.setLocation(location)
    }

    override fun onBearingChanged(bearing: Bearing) {
        super.onBearingChanged(bearing)
        layer.setAzimuth(bearing)
    }


}