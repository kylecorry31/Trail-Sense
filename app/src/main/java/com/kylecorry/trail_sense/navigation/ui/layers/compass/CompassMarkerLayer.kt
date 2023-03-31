package com.kylecorry.trail_sense.navigation.ui.layers.compass

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.trail_sense.navigation.ui.IMappableReferencePoint

open class CompassMarkerLayer : ICompassLayer {

    private val markers = mutableListOf<Pair<IMappableReferencePoint, Int?>>()
    private val lock = Any()

    fun addMarker(marker: IMappableReferencePoint, size: Int? = null) {
        synchronized(lock) {
            markers.add(marker to size)
        }
    }

    fun removeMarker(marker: IMappableReferencePoint) {
        synchronized(lock) {
            markers.removeIf { it.first == marker }
        }
    }

    fun clearMarkers() {
        synchronized(lock) {
            markers.clear()
        }
    }

    override fun draw(drawer: ICanvasDrawer, compass: ICompassView) {
        val markers = synchronized(lock) {
            markers.toList()
        }
        markers.forEach {
            compass.draw(it.first, it.second)
        }
    }

    override fun invalidate() {

    }
}