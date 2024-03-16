package com.kylecorry.trail_sense.tools.augmented_reality.ui.layers

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.canvas.InteractionUtils
import com.kylecorry.trail_sense.shared.canvas.PixelCircle
import com.kylecorry.trail_sense.tools.augmented_reality.ui.ARMarker
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView

class ARMarkerLayer(
    private val minimumDpSize: Float = 0f,
    private val maximumDpSize: Float? = null,
    private val renderMarkersBelowMinSize: Boolean = true
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

    fun clearMarkers() {
        synchronized(lock) {
            markers.clear()
        }
    }

    private var renderedMarkers: List<Pair<ARMarker, PixelCircle>> = emptyList()

    override suspend fun update(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        val minimumPixelSize = drawer.dp(minimumDpSize)
        val maximumPixelSize = maximumDpSize?.let { drawer.dp(it) } ?: Float.MAX_VALUE
        renderedMarkers = synchronized(lock) {
            markers.mapNotNull {
                val circle = getCircle(it, view, minimumPixelSize, maximumPixelSize)
                    ?: return@mapNotNull null
                it to circle
            }
        }
    }

    override fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        potentialFocusPoints.clear()
        val center = PixelCoordinate(view.width / 2f, view.height / 2f)
        val reticle = PixelCircle(center, view.reticleDiameter / 2f)
        val markers = synchronized(lock) {
            renderedMarkers.toList()
        }
        markers.forEach {
            it.first.draw(view, drawer, it.second)
            if (reticle.intersects(it.second)) {
                potentialFocusPoints.add(it)
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

        val minimumPixelSize = drawer.dp(minimumDpSize)
        val maximumPixelSize = maximumDpSize?.let { drawer.dp(it) } ?: Float.MAX_VALUE

        val points = markers.mapNotNull {
            val circle =
                getCircle(it, view, minimumPixelSize, maximumPixelSize) ?: return@mapNotNull null
            it to circle
        }

        val sorted = InteractionUtils.getClickedPoints(click, points)

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
    ): PixelCircle? {
        val circle = marker.getViewLocation(view)
        if (!renderMarkersBelowMinSize && circle.radius < minimumPixelSize) {
            return null
        }
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