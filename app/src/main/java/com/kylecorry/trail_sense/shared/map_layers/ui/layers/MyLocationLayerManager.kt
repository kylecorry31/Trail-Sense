package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import com.kylecorry.sol.units.Coordinate

class MyLocationLayerManager(
    private val layer: MyLocationLayer
) :
    BaseLayerManager() {
    override fun start() {
    }

    override fun stop() {
    }

    override fun onLocationChanged(location: Coordinate, accuracy: Float?) {
        super.onLocationChanged(location, accuracy)
        layer.setLocation(location)
        layer.setAccuracy(accuracy)
    }

    override fun onBearingChanged(bearing: Float) {
        super.onBearingChanged(bearing)
        layer.setAzimuth(bearing)
    }


}