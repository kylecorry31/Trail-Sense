package com.kylecorry.trail_sense.tools.maps.infrastructure.layers

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Speed

abstract class BaseLayerManager : ILayerManager {

    protected var bounds: CoordinateBounds? = null
    protected var location: Coordinate? = null
    protected var speed: Speed? = null
    protected var bearing: Float? = null
    protected var accuracy: Float? = null

    override fun onBearingChanged(bearing: Float) {
        this.bearing = bearing
    }

    override fun onSpeedChanged(speed: Speed) {
        this.speed = speed
    }

    override fun onBoundsChanged(bounds: CoordinateBounds?) {
        this.bounds = bounds
    }

    override fun onLocationChanged(location: Coordinate, accuracy: Float?) {
        this.location = location
        this.accuracy = accuracy
    }
}