package com.kylecorry.trail_sense.tools.navigation.ui.layers

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.trail_sense.shared.canvas.InteractionUtils
import com.kylecorry.trail_sense.shared.canvas.PixelCircle
import com.kylecorry.trail_sense.shared.getBounds
import com.kylecorry.trail_sense.shared.toVector2
import com.kylecorry.trail_sense.tools.navigation.ui.markers.MapMarker

open class BaseLayer : ILayer {

    private val markers = mutableListOf<MapMarker>()
    private val lock = Any()

    fun addMarker(marker: MapMarker) {
        synchronized(lock) {
            markers.add(marker)
        }
    }

    fun clearMarkers() {
        synchronized(lock) {
            markers.clear()
        }
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        val bounds = getBounds(drawer)
        val markers = synchronized(lock) {
            markers.toList()
        }
        markers.forEach {
            val anchor = map.toPixel(it.location)
            if (bounds.contains(anchor.toVector2(bounds.top))) {
                it.draw(drawer, anchor, map.layerScale, map.mapAzimuth + map.mapRotation)
            }
        }
    }

    override fun drawOverlay(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        // Do nothing
    }

    override fun invalidate() {

    }

    override fun onClick(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean {
        val markers = synchronized(lock) {
            markers.toList()
        }
        val points = markers.map {
            val anchor = map.toPixel(it.location)
            val radius = drawer.dp(it.size * map.layerScale) / 2f
            it to PixelCircle(anchor, radius)
        }

        val clicked = InteractionUtils.getClickedPoints(
            PixelCircle(pixel, drawer.dp(InteractionUtils.CLICK_SIZE_DP)),
            points
        )

        for (marker in clicked) {
            if (marker.first.onClick()) {
                return true
            }
        }

        return false
    }

    private fun getBounds(drawer: ICanvasDrawer): Rectangle {
        // Rotating by map rotation wasn't working around 90/270 degrees - this is a workaround
        // It will just render slightly more of the path than needed, but never less (since 45 is when the area is at its largest)
        return drawer.getBounds(45f)
    }
}