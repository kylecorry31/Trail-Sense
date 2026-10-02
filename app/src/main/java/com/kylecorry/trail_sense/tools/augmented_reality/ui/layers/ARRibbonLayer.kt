package com.kylecorry.trail_sense.tools.augmented_reality.ui.layers

import android.graphics.Path
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.geometry.Geometry
import com.kylecorry.sol.math.geometry.Polygon
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.AugmentedRealityCoordinate
import com.kylecorry.trail_sense.tools.augmented_reality.ui.ARLine
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import kotlin.math.hypot

class ARRibbonLayer(private val widthMeters: Float) : ARLayer {

    class Ribbon(val line: ARLine, val onFocus: () -> Boolean)

    private class RenderedRibbon(
        @ColorInt val color: Int,
        val onFocus: () -> Boolean,
        val path: Path,
        val quads: List<Polygon>
    )

    private val lock = Any()
    private var ribbons: List<Ribbon> = emptyList()
    private var rendered: List<RenderedRibbon> = emptyList()

    fun setRibbons(ribbons: List<Ribbon>) {
        synchronized(lock) {
            this.ribbons = ribbons
        }
    }

    override suspend fun update(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        val current = synchronized(lock) { ribbons }
        val newRendered = current.map { buildRibbon(it, view) }
        synchronized(lock) {
            rendered = newRendered
        }
    }

    override fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        val current = synchronized(lock) { rendered }
        drawer.noStroke()
        for (ribbon in current) {
            drawer.fill(ribbon.color)
            drawer.path(ribbon.path)
        }
    }

    private fun buildRibbon(ribbon: Ribbon, view: AugmentedRealityView): RenderedRibbon {
        val coordinates = ribbon.line.points.map { it.getAugmentedRealityCoordinate(view) }
        val positions = coordinates.map { it.position }

        // Left and right edge of the ribbon at each point
        val edges = positions.indices.map { i ->
            val offset = sideOffset(positions, i, widthMeters / 2f)
            (positions[i] + offset) to (positions[i] - offset)
        }

        // One quad per segment, clipped so the part in front of the camera is still drawn
        val path = Path()
        val quads = mutableListOf<Polygon>()
        for (i in 0 until edges.size - 1) {
            val (left, right) = edges[i]
            val (nextLeft, nextRight) = edges[i + 1]
            val isTrueNorth = coordinates[i].isTrueNorth
            val corners = clipToNearPlane(listOf(left, nextLeft, nextRight, right), isTrueNorth, view)
                .map { view.toPixel(AugmentedRealityCoordinate(it, isTrueNorth), true) }
            if (corners.size < 3 || corners.any { it.x.isNaN() || it.y.isNaN() }) continue
            corners.forEachIndexed { index, corner ->
                if (index == 0) path.moveTo(corner.x, corner.y) else path.lineTo(corner.x, corner.y)
            }
            path.close()
            quads.add(Polygon(corners.map { Vector2(it.x, it.y) }))
        }
        val color = ColorUtils.setAlphaComponent(ribbon.line.color, RIBBON_ALPHA)
        return RenderedRibbon(color, ribbon.onFocus, path, quads)
    }

    private fun clipToNearPlane(
        corners: List<Vector3>,
        isTrueNorth: Boolean,
        view: AugmentedRealityView
    ): List<Vector3> {
        val depths = corners.map {
            AugmentedRealityUtils.enuToAr(
                view.getActualPoint(it, isTrueNorth),
                view.rotationMatrix
            ).z
        }
        val isInside = depths.map { it >= NEAR_PLANE_METERS }
        val clipped = mutableListOf<Vector3>()
        for (i in corners.indices) {
            val j = (i + 1) % corners.size
            if (isInside[i]) {
                clipped.add(corners[i])
            }
            if (isInside[i] != isInside[j]) {
                val t = (NEAR_PLANE_METERS - depths[i]) / (depths[j] - depths[i])
                clipped.add(corners[i] + (corners[j] - corners[i]) * t)
            }
        }
        return clipped
    }

    /**
     * The horizontal vector perpendicular to the path at a point. The direction is taken from the
     * neighboring points so adjacent segments join without gaps.
     */
    private fun sideOffset(positions: List<Vector3>, index: Int, length: Float): Vector3 {
        val prev = positions[maxOf(index - 1, 0)]
        val next = positions[minOf(index + 1, positions.lastIndex)]
        val dx = next.x - prev.x
        val dy = next.y - prev.y
        val norm = hypot(dx, dy)
        if (norm < 0.0001f) return Vector3.zero
        return Vector3(-dy / norm * length, dx / norm * length, 0f)
    }

    override fun invalidate() {
        // Do nothing
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        view: AugmentedRealityView,
        pixel: PixelCoordinate
    ): Boolean {
        return false
    }

    override fun onFocus(drawer: ICanvasDrawer, view: AugmentedRealityView): Boolean {
        val center = Vector2(view.width / 2f, view.height / 2f)
        val reticleRadius = view.reticleDiameter / 2f
        // Try the ribbon closest to the center of the view first
        return synchronized(lock) { rendered }
            .mapNotNull { ribbon ->
                val distance = ribbon.quads.minOfOrNull { distanceToQuad(center, it) }
                    ?: return@mapNotNull null
                if (distance <= reticleRadius) ribbon to distance else null
            }
            .sortedBy { it.second }
            .any { it.first.onFocus() }
    }

    /** 0 if the point is inside the quad, otherwise the distance to its nearest edge */
    private fun distanceToQuad(point: Vector2, quad: Polygon): Float {
        if (Geometry.contains(quad, point)) {
            return 0f
        }
        val vertices = quad.vertices
        return vertices.indices.minOf { i ->
            distanceToLine(point, vertices[i], vertices[(i + 1) % vertices.size])
        }
    }

    private fun distanceToLine(point: Vector2, start: Vector2, end: Vector2): Float {
        return point.distanceTo(Geometry.snapToLine(point.x, point.y, start.x, start.y, end.x, end.y))
    }


    private companion object {
        const val RIBBON_ALPHA = 200
        const val NEAR_PLANE_METERS = 0.05f
    }
}
