package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate

abstract class BaseLayerManager : ILayerManager {

    protected var bounds: CoordinateBounds? = null
    protected var location: Coordinate? = null
    protected var bearing: Float? = null
    protected var accuracy: Float? = null

    override fun onBearingChanged(bearing: Float) {
        this.bearing = bearing
    }

    override fun onBoundsChanged(bounds: CoordinateBounds?) {
        this.bounds = bounds
    }

    override fun onLocationChanged(location: Coordinate, accuracy: Float?) {
        this.location = location
        this.accuracy = accuracy
    }
}