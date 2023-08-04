package com.kylecorry.trail_sense.tools.maps.infrastructure.layers

import android.content.Context
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.ui.layers.ILayer

// TODO: Create a loader for each layer that keeps it up to date - this class will manage all loaders

class MapLayerManager(context: Context, layers: List<ILayer>) :
    ILayerManager {

    private val factory = LayerManagerFactory(context)
    private var managers: List<ILayerManager>

    init {
        managers = layers.mapNotNull { factory.getLayerManager(it) }
    }

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