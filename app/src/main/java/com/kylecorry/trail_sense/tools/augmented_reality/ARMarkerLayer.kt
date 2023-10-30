package com.kylecorry.trail_sense.tools.augmented_reality

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.canvas.PixelCircle

class ARMarkerLayer : ARLayer {

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

        markers.forEach {
            val coordinates = it.getHorizonCoordinate(view)
            val angularDiameter = it.getAngularDiameter(view)
            val circle =
                PixelCircle(view.toPixel(coordinates), view.sizeToPixel(angularDiameter) / 2f)

            it.draw(drawer, circle)

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
        val clickSizeMultiplier = 2f
        val markers = synchronized(lock) {
            markers.toList()
        }
        // TODO: Check for intersection instead
        val clicked = markers.map {
            val anchor = view.toPixel(it.getHorizonCoordinate(view))
            val radius = view.sizeToPixel(it.getAngularDiameter(view) * clickSizeMultiplier) / 2f
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

    override fun onFocus(drawer: ICanvasDrawer, view: AugmentedRealityView): Boolean {
        val center = PixelCoordinate(view.width / 2f, view.height / 2f)

        // Focus on the closest marker
        val sorted = potentialFocusPoints
            .sortedBy { it.second.center.distanceTo(center) }
        for (marker in sorted) {
            if (marker.first.onFocused()) {
                return true
            }
        }

        return false
    }

}