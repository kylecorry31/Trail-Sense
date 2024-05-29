package com.kylecorry.trail_sense.tools.maps.infrastructure.layers

import androidx.annotation.ColorInt
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Speed
import com.kylecorry.trail_sense.tools.navigation.ui.layers.CourseLayer

class CourseLayerManager(private val layer: CourseLayer, @ColorInt private val cogColor: Int, @ColorInt private val headingColor: Int) :
    BaseLayerManager() {

    override fun start() {
        layer.setCOGColor(cogColor)
        layer.setHeadingColor(headingColor)
    }

    override fun stop() {
    }

    override fun onSpeedChanged(speed: Speed) {
        super.onSpeedChanged(speed)
        layer.setSpeed(speed)
    }

    override fun onBearingChanged(bearing: Float) {
        super.onBearingChanged(bearing)
        layer.setBearing(bearing)
    }

    override fun onCOGChanged(cog: Bearing?) {
        super.onCOGChanged(cog)
        layer.setCOG(cog)
    }

    override fun onLocationChanged(location: Coordinate, accuracy: Float?) {
        super.onLocationChanged(location, accuracy)
        layer.setLocation(location)
    }
}