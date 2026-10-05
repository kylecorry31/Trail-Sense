package com.kylecorry.trail_sense.tools.augmented_reality.ui.layers

import android.graphics.Path
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Geometry
import com.kylecorry.sol.math.geometry.Polygon
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.AugmentedRealityCoordinate
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView

/**
 * An augmented reality layer that fills polygons, clipping the part behind the camera.
 * The polygons are requested from the source on every update, so they can depend on the view.
 */
class ARPolygonLayer : ARLayer {

    private class RenderedPolygon(
        val color: Int,
        val onFocus: (() -> Boolean)?,
        val path: Path,
        val parts: List<Polygon>
    )

    @Volatile
    private var source: (AugmentedRealityView) -> List<ARPolygon> = { emptyList() }

    @Volatile
    private var rendered: List<RenderedPolygon> = emptyList()

    fun setSource(source: (AugmentedRealityView) -> List<ARPolygon>) {
        this.source = source
    }

    override suspend fun update(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        rendered = source(view).map { render(it, view) }
    }

    override fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        drawer.noStroke()
        for (polygon in rendered) {
            drawer.fill(polygon.color)
            drawer.path(polygon.path)
        }
    }

    private fun render(polygon: ARPolygon, view: AugmentedRealityView): RenderedPolygon {
        val path = Path()
        val parts = mutableListOf<Polygon>()
        for (part in polygon.parts) {
            val corners = project(part, view) ?: continue
            corners.forEachIndexed { index, corner ->
                if (index == 0) path.moveTo(corner.x, corner.y) else path.lineTo(corner.x, corner.y)
            }
            path.close()
            parts.add(Polygon(corners.map { Vector2(it.x, it.y) }))
        }
        return RenderedPolygon(polygon.color, polygon.onFocus, path, parts)
    }

    /** The pixel corners of the part of the polygon in front of the camera, or null if not drawable */
    private fun project(
        vertices: List<AugmentedRealityCoordinate>,
        view: AugmentedRealityView
    ): List<PixelCoordinate>? {
        val pixels = clipToNearPlane(vertices, view)
            .map { view.toPixel(it, true) }
        if (pixels.size < 3 || pixels.any { it.x.isNaN() || it.y.isNaN() }) return null
        return pixels
    }

    private fun clipToNearPlane(
        vertices: List<AugmentedRealityCoordinate>,
        view: AugmentedRealityView
    ): List<AugmentedRealityCoordinate> {
        val depths = vertices.map {
            AugmentedRealityUtils.enuToAr(
                view.getActualPoint(it.position, it.isTrueNorth),
                view.rotationMatrix
            ).z
        }
        val isInside = depths.map { it >= NEAR_PLANE_METERS }
        val clipped = mutableListOf<AugmentedRealityCoordinate>()
        for (i in vertices.indices) {
            val j = (i + 1) % vertices.size
            if (isInside[i]) {
                clipped.add(vertices[i])
            }
            if (isInside[i] != isInside[j]) {
                val t = (NEAR_PLANE_METERS - depths[i]) / (depths[j] - depths[i])
                val position = vertices[i].position + (vertices[j].position - vertices[i].position) * t
                clipped.add(AugmentedRealityCoordinate(position, vertices[i].isTrueNorth))
            }
        }
        return clipped
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
        // Try the polygon closest to the center of the view first
        return rendered
            .mapNotNull { polygon ->
                val onFocus = polygon.onFocus ?: return@mapNotNull null
                val distance = polygon.parts.minOfOrNull { distanceToPolygon(center, it) }
                    ?: return@mapNotNull null
                if (distance <= reticleRadius) onFocus to distance else null
            }
            .sortedBy { it.second }
            .any { it.first() }
    }

    /** 0 if the point is inside the polygon, otherwise the distance to its nearest edge */
    private fun distanceToPolygon(point: Vector2, polygon: Polygon): Float {
        if (Geometry.contains(polygon, point)) {
            return 0f
        }
        val vertices = polygon.vertices
        return vertices.indices.minOf { i ->
            distanceToLine(point, vertices[i], vertices[(i + 1) % vertices.size])
        }
    }

    private fun distanceToLine(point: Vector2, start: Vector2, end: Vector2): Float {
        return point.distanceTo(Geometry.snapToLine(point.x, point.y, start.x, start.y, end.x, end.y))
    }

    private companion object {
        const val NEAR_PLANE_METERS = 0.05f
    }
}
