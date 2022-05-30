package com.kylecorry.trail_sense.navigation.ui.layers

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.trail_sense.navigation.ui.markers.Marker

open class BaseLayer : ILayer {

    private val markers = mutableListOf<Marker>()

    fun addMarker(marker: Marker) {
        markers.add(marker)
    }

    fun removeMarker(marker: Marker) {
        markers.remove(marker)
    }

    fun clearMarkers() {
        markers.clear()
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        markers.forEach {
            val anchor = map.toPixel(it.location)
            it.draw(drawer, anchor, map.layerScale, map.mapRotation)
        }
    }

    override fun invalidate() {

    }
}