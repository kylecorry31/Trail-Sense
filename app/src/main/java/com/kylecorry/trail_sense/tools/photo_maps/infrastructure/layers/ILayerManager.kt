package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate

interface ILayerManager {

    fun start()
    fun stop()

    fun onLocationChanged(location: Coordinate, accuracy: Float? = null)
    fun onBearingChanged(bearing: Float)
    fun onBoundsChanged(bounds: CoordinateBounds?)

}