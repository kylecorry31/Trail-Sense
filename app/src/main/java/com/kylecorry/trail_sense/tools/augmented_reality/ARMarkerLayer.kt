package com.kylecorry.trail_sense.tools.augmented_reality

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.canvas.PixelCircle

class ARMarkerLayer(
    private val minimumDpSize: Float = 0f,
    private val maximumDpSize: Float? = null,
) : ARLayer {

    private val markers = mutableListOf<ARMarker>()
    private val lock = Any()
    private val potentialFocusPoints = mutableListOf<Pair<ARMarker, PixelCircle>>()

    fun setMarkers(markers: List<ARMarker>) {
        synchronized(lock) {
            this.markers.clear()
            this.markers.addAll(markers)
        }
    }

    fun addMarker(marker: ARMarker) {
        synchronized(lock) {
            markers.add(marker)
        }
    }

    fun removeMarker(marker: ARMarker) {
        synchronized(lock) {
            markers.remove(marker)
        }
    }

    fun clearMarkers() {
        synchronized(lock) {
            markers.clear()
        }
    }

    override fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        val markers = synchronized(lock) {
            markers.toList()
        }

        val center = PixelCoordinate(view.width / 2f, view.height / 2f)
        val reticle = PixelCircle(center, view.reticleDiameter / 2f)

        potentialFocusPoints.clear()

        // TODO: Save these
        val minimumPixelSize = drawer.dp(minimumDpSize)
        val maximumPixelSize = maximumDpSize?.let { drawer.dp(it) } ?: Float.MAX_VALUE

        markers.forEach {
            val circle = getCircle(it, view, minimumPixelSize, maximumPixelSize)

            it.draw(view, drawer, circle)

            if (reticle.intersects(circle)) {
                potentialFocusPoints.add(it to circle)
            }
        }
    }

    override fun invalidate() {
        // Do nothing
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        view: AugmentedRealityView,
        pixel: PixelCoordinate
    ): Boolean {
        val markers = synchronized(lock) {
            markers.toList()
        }
        val click = PixelCircle(
            pixel,
            view.reticleDiameter / 2f
        )
        // potentialFocusPoints is ordered by farthest to closest to the camera
        // Focus on the closest marker

        val minimumPixelSize = drawer.dp(minimumDpSize)
        val maximumPixelSize = maximumDpSize?.let { drawer.dp(it) } ?: Float.MAX_VALUE

        val sorted = markers
            .mapNotNull {
                val circle = getCircle(it, view, minimumPixelSize, maximumPixelSize)
                if (circle.intersects(click)) {
                    it to circle
                } else {
                    null
                }
            }
            .reversed()
            .sortedBy {
                // The point is centered (which means if the point is closer to the camera it will be first in the list)
                if (it.second.contains(pixel)) {
                    return@sortedBy 0f
                }
                // The circle does not overlap with the center, so calculate the distance to the nearest point on the circle
                it.second.center.distanceTo(pixel) - it.second.radius
            }

        for (marker in sorted) {
            if (marker.first.onClick()) {
                return true
            }
        }

        return false
    }

    private fun getCircle(
        marker: ARMarker,
        view: AugmentedRealityView,
        minimumPixelSize: Float,
        maximumPixelSize: Float
    ): PixelCircle {
        val circle = marker.getViewLocation(view)
        return circle.copy(
            radius = circle.radius.coerceIn(minimumPixelSize / 2f, maximumPixelSize / 2f)
        )
    }

    override fun onFocus(drawer: ICanvasDrawer, view: AugmentedRealityView): Boolean {
        val center = PixelCoordinate(view.width / 2f, view.height / 2f)
        // potentialFocusPoints is ordered by farthest to closest to the camera
        // Focus on the closest marker
        val sorted = potentialFocusPoints
            .reversed()
            .sortedBy {
                // The point is centered (which means if the point is closer to the camera it will be first in the list)
                if (it.second.contains(center)) {
                    return@sortedBy 0f
                }
                // The circle does not overlap with the center, so calculate the distance to the nearest point on the circle
                it.second.center.distanceTo(center) - it.second.radius
            }
        for (marker in sorted) {
            if (marker.first.onFocused()) {
                return true
            }
        }

        return false
    }

}