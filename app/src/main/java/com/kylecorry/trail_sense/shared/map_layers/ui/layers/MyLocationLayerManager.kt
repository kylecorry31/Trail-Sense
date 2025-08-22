package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import androidx.annotation.ColorInt
import com.kylecorry.sol.units.Coordinate

class MyLocationLayerManager(
    private val layer: MyLocationLayer,
    @ColorInt private val color: Int,
    @ColorInt private val accuracyColor: Int
) :
    BaseLayerManager() {
    override fun start() {
        layer.setColor(color)
        layer.setAccuracyColor(accuracyColor)
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