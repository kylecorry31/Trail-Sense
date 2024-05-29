package com.kylecorry.trail_sense.tools.maps.infrastructure.layers

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Speed

interface ILayerManager {

    fun start()
    fun stop()

    fun onLocationChanged(location: Coordinate, accuracy: Float? = null)
    fun onSpeedChanged(speed: Speed)
    fun onCOGChanged(cog: Bearing?)
    fun onBearingChanged(bearing: Float)
    fun onBoundsChanged(bounds: CoordinateBounds?)

}