package com.kylecorry.trail_sense.tools.maps.infrastructure.layers

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate

interface ILayerManager {

    fun start()
    fun stop()

    fun onLocationChanged(location: Coordinate, accuracy: Float? = null)
    fun onBearingChanged(bearing: Bearing)
    fun onBoundsChanged(bounds: CoordinateBounds)

}