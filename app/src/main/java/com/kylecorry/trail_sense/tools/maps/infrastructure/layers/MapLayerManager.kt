package com.kylecorry.trail_sense.tools.maps.infrastructure.layers

import android.content.Context
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.ui.layers.ILayer
import com.kylecorry.trail_sense.navigation.ui.layers.PathLayer

// TODO: Create a loader for each layer that keeps it up to date - this class will manage all loaders

class MapLayerManager(context: Context, layers: List<ILayer>) :
    ILayerManager {

    private var managers: List<ILayerManager>

    init {
        val managersToAdd = mutableListOf<ILayerManager>()

        val pathLayer = layers.firstOrNull { it is PathLayer } as? PathLayer
        if (pathLayer != null) {
            managersToAdd.add(PathLayerManager(context, pathLayer))
        }

        // TODO: Initialize other managers

        managers = managersToAdd
    }

    override fun start() {
        managers.forEach { it.start() }
    }

    override fun stop() {
        managers.forEach { it.stop() }
    }

    override fun onBoundsChanged(bounds: CoordinateBounds) {
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