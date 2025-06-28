package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers

import androidx.annotation.ColorInt
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.navigation.ui.layers.MyLocationLayer

class MyLocationLayerManager(private val layer: MyLocationLayer, @ColorInt private val color: Int) :
    BaseLayerManager() {
    override fun start() {
        layer.setColor(color)
    }

    override fun stop() {
    }

    override fun onLocationChanged(location: Coordinate, accuracy: Float?) {
        super.onLocationChanged(location, accuracy)
        layer.setLocation(location)
    }

    override fun onBearingChanged(bearing: Float) {
        super.onBearingChanged(bearing)
        layer.setAzimuth(bearing)
    }


}