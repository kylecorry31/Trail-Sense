package com.kylecorry.trail_sense.navigation.ui.layers

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.navigation.ui.markers.Marker
import com.kylecorry.trail_sense.shared.canvas.PixelCircle

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

    override fun onClick(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean {
        val clickSizeMultiplier = 2f
        val clicked = markers.map {
            val anchor = map.toPixel(it.location)
            val radius = drawer.dp(it.size * map.layerScale * clickSizeMultiplier) / 2f
            it to PixelCircle(anchor, radius)
        }
            .filter { it.second.contains(pixel) }
            .sortedBy { it.second.center.distanceTo(pixel) }

        for (marker in clicked) {
            if (marker.first.onClick()) {
                return true
            }
        }

        return false
    }
}