package com.kylecorry.trail_sense.tools.augmented_reality.ui.layers

import android.graphics.Color
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector3
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.ARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.AugmentedRealityCoordinate
import com.kylecorry.trail_sense.tools.augmented_reality.ui.ARMarker
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import com.kylecorry.trail_sense.tools.augmented_reality.ui.CanvasCircle
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Debug layer that draws points on the estimated terrain within 20 feet of the user.
 * Green points are heights measured by the terrain map, orange points are the flat ground estimate.
 */
class ARGroundDebugLayer : ARLayer {

    private val markerLayer = ARMarkerLayer(1f, 20f, true)
    private var lastKey: List<Int?>? = null

    private class GridPoint(
        val coordinate: AugmentedRealityCoordinate,
        private val diameter: Float
    ) : ARPoint {
        override fun getAugmentedRealityCoordinate(view: AugmentedRealityView) = coordinate

        override fun getAngularDiameter(view: AugmentedRealityView): Float {
            val p = coordinate.position
            return AugmentedRealityUtils.getAngularSize(diameter, p.magnitude())
        }
    }

    override suspend fun update(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        val terrain = view.terrain
        val (px, py) = view.terrainPosition
        val ground = view.groundOffset
        val key = listOf(
            terrain?.version,
            (px * 4).roundToInt(),
            (py * 4).roundToInt(),
            ground?.let { (it * 10).roundToInt() }
        )
        if (key != lastKey) {
            lastKey = key
            markerLayer.setMarkers(buildMarkers(view, px, py, ground ?: DEFAULT_GROUND))
        }
        markerLayer.update(drawer, view)
    }

    private fun buildMarkers(
        view: AugmentedRealityView,
        px: Float,
        py: Float,
        fallback: Float
    ): List<ARMarker> {
        val terrain = view.terrain
        val markers = mutableListOf<ARMarker>()
        val minX = floor((px - RADIUS) / SPACING).toInt()
        val maxX = floor((px + RADIUS) / SPACING).toInt() + 1
        val minY = floor((py - RADIUS) / SPACING).toInt()
        val maxY = floor((py + RADIUS) / SPACING).toInt() + 1
        for (gx in minX..maxX) {
            for (gy in minY..maxY) {
                val x = gx * SPACING
                val y = gy * SPACING
                if (hypot(x - px, y - py) > RADIUS) continue
                val measured = terrain?.heightAt(x, y)
                val z = measured ?: fallback
                val color = if (measured != null) Color.GREEN else Color.rgb(255, 160, 0)
                markers.add(
                    ARMarker(
                        GridPoint(
                            AugmentedRealityCoordinate(Vector3(x - px, y - py, z), view.isTrueNorth),
                            POINT_SIZE
                        ),
                        CanvasCircle(color, Color.BLACK)
                    )
                )
            }
        }
        return markers
    }

    override fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        markerLayer.draw(drawer, view)
    }

    override fun invalidate() {
        lastKey = null
        markerLayer.invalidate()
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        view: AugmentedRealityView,
        pixel: PixelCoordinate
    ) = false

    override fun onFocus(drawer: ICanvasDrawer, view: AugmentedRealityView) = false

    private companion object {
        const val RADIUS = 6.096f // 20 feet
        const val SPACING = 0.5f
        const val POINT_SIZE = 0.08f
        const val DEFAULT_GROUND = -1.5f
    }
}
