package com.kylecorry.trail_sense.tools.maps.infrastructure.layers

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate

class MultiLayerManager(private val managers: List<ILayerManager>) : ILayerManager {

    override fun start() {
        managers.forEach { it.start() }
    }

    override fun stop() {
        managers.forEach { it.stop() }
    }

    override fun onBoundsChanged(bounds: CoordinateBounds?) {
        managers.forEach {
            it.onBoundsChanged(bounds)
        }
    }

    override fun onLocationChanged(location: Coordinate, accuracy: Float?) {
        managers.forEach {
            it.onLocationChanged(location, accuracy)
        }
    }

    override fun onBearingChanged(bearing: Bearing) {
        managers.forEach {
            it.onBearingChanged(bearing)
        }
    }
}