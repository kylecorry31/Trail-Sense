package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate

class MultiLayerManager(private val managers: List<ILayerManager>) : ILayerManager {

    private var lastBounds: CoordinateBounds? = null

    override fun start() {
        managers.forEach { it.start() }
    }

    override fun stop() {
        managers.forEach { it.stop() }
    }

    override fun onBoundsChanged(bounds: CoordinateBounds?) {
        if (bounds == lastBounds) {
            return
        }
        lastBounds = bounds
        managers.forEach {
            it.onBoundsChanged(bounds)
        }
    }

    override fun onLocationChanged(location: Coordinate, accuracy: Float?) {
        managers.forEach {
            it.onLocationChanged(location, accuracy)
        }
    }

    override fun onBearingChanged(bearing: Float) {
        managers.forEach {
            it.onBearingChanged(bearing)
        }
    }
}