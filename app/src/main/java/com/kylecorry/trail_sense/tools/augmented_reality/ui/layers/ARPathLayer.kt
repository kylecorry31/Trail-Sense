package com.kylecorry.trail_sense.tools.augmented_reality.ui.layers

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.sol.math.SolMath.square
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.science.geography.projections.AzimuthalEquidistantProjection
import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.canvas.LineClipper
import com.kylecorry.trail_sense.shared.canvas.LineInterpolator
import com.kylecorry.trail_sense.shared.extensions.isSamePixel
import com.kylecorry.trail_sense.shared.extensions.squaredDistanceTo
import com.kylecorry.trail_sense.shared.forEachLine
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.ARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.GeographicARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.ui.ARLine
import com.kylecorry.trail_sense.tools.augmented_reality.ui.ARMarker
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import com.kylecorry.trail_sense.tools.augmented_reality.ui.CanvasCircle
import com.kylecorry.trail_sense.tools.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.tools.paths.ui.IPathLayer

/**
 * An augmented reality layer that displays paths
 * @param viewDistanceMeters the maximum distance to display paths
 * @param adjustForPathElevation true if the layer should adjust for path elevation, otherwise paths will be 2 meters below the camera
 * @param updateEveryCycle true if the layer should update every cycle, otherwise the layer will only update when the paths change (snapping and clipping may become out of date if this is false)
 * @param onFocus the callback to call when a path is focused, return true if focus is claimed
 */
class ARPathLayer(
    viewDistanceMeters: Float,
    private val adjustForPathElevation: Boolean,
    private val onFocus: (path: IMappablePath) -> Boolean = { false },
) : ARLayer, IPathLayer {

    private val lineLayer = ARLineLayer(renderWithPaths = false)
    private val markerLayer = ARMarkerLayer(1f, 32f, false)
    private var lastElevation: Float? = null
    private var lastLocationAccuracySquared: Float? = null

    private val squareViewDistance = square(viewDistanceMeters)
    private val center = PixelCoordinate(viewDistanceMeters, viewDistanceMeters)
    private val bounds = Rectangle(
        0f,
        viewDistanceMeters * 2,
        viewDistanceMeters * 2,
        0f,
    )
    private val clipper = LineClipper()
    private val interpolator = LineInterpolator()

    private val pointSpacing = 5f // meters
    private val pathSimplification = 0.5f // meters
    private val snapDistance = 2 * viewDistanceMeters / 3f // meters
    private val snapDistanceSquared = square(snapDistance)

    private val maxElevationOffset = 5f // meters
    private val defaultElevationOffset = -2f // meters

    private var projection: IMapProjection? = null

    private var paths: List<IMappablePath> = listOf()

    private val hooks = Hooks()

    override suspend fun update(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        lastElevation = view.altitude
        lastLocationAccuracySquared = view.locationAccuracy?.let { square(it) }

        hooks.effect(
            "projection",
            view.location
        ) {
            projection = AzimuthalEquidistantProjection(
                view.location,
                Vector2(center.x, center.y),
                isYFlipped = true
            )
        }

        hooks.effect(
            "paths",
            view.location,
            if (adjustForPathElevation) view.altitude else null,
            view.locationAccuracy,
            paths
        ) {
            updatePaths()
        }

        lineLayer.update(drawer, view)
        markerLayer.update(drawer, view)
    }

    override fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        lineLayer.draw(drawer, view)
        markerLayer.draw(drawer, view)
    }

    override fun invalidate() {
        lineLayer.invalidate()
        markerLayer.invalidate()
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        view: AugmentedRealityView,
        pixel: PixelCoordinate
    ): Boolean {
        return false
    }

    override fun onFocus(drawer: ICanvasDrawer, view: AugmentedRealityView): Boolean {
        return markerLayer.onFocus(drawer, view)
    }

    override fun setPaths(paths: List<IMappablePath>) {
        this.paths = paths
    }

    private fun updatePaths() {
        val paths = paths
        val elevation = lastElevation

        val points = paths.map {
            it to getNearbyARPoints(it, elevation)
        }

        val nearestPoints = points.mapNotNull { getNearestPoint(it.second) }
        val nearest = nearestPoints.minByOrNull { it.point.squaredDistanceTo(center) }
        if (nearest != null) {
            // Add it to the path
            if (nearest.previousIndex != null && nearest.nextIndex != null) {
                nearest.addToPath()
            }


            points.forEach {
                recenterPoints(it.second.first, nearest.point, center)
            }

            // Set the elevation to the nearest point
            if (adjustForPathElevation) {
                val elevationDelta = elevation?.minus(nearest.elevation) ?: 0f
                points.forEach {
                    it.second.second.forEachIndexed { index, _ ->
                        it.second.second[index] = it.second.second[index] + elevationDelta
                    }
                }
            }
        }


        // Add the offset to all the points
        val lines = points.flatMap { (path, pts) ->
            project(pts).map {
                path to ARLine(it, path.color, 4f)
            }
        }

        val markers = lines.flatMap {
            it.second.points.map { point ->
                ARMarker(
                    point,
                    CanvasCircle(it.second.color),
                    onFocusedFn = {
                        onFocus(it.first)
                    }
                )
            }
        }

        markerLayer.setMarkers(markers)
        lineLayer.setLines(lines.map { it.second })
    }

    private fun getNearestPoint(points: Pair<MutableList<Float>, MutableList<Float>>): NearestPoint? {
        var minPoint: PixelCoordinate? = null
        var minPointIndex: Int? = null
        var minDistance =
            lastLocationAccuracySquared?.coerceAtLeast(snapDistanceSquared) ?: snapDistanceSquared
        var minPointElevation: Float? = null

        var i = 0
        points.first.forEachLine { x1, y1, x2, y2 ->
            val z1 = points.second.getOrNull(i) ?: lastElevation ?: 0f
            val z2 = points.second.getOrNull(i + 1) ?: lastElevation ?: 0f
            val projected = projectOntoLine(center.x, center.y, x1, y1, z1, x2, y2, z2)
            val distance = projected.first.squaredDistanceTo(center)

            if (distance < minDistance) {
                minDistance = distance
                minPoint = projected.first
                minPointElevation = projected.second
                minPointIndex = i
            }

            i += 2
        }

        return NearestPoint(
            minPoint ?: return null,
            minPointElevation ?: return null,
            points,
            minPointIndex,
            minPointIndex?.let { it + 1 }
        )
    }

    private fun project(
        points: Pair<MutableList<Float>, MutableList<Float>>
    ): List<List<ARPoint>> {
        // Step 4: Convert the points back to geographic coordinates and split the lines
        val lines = mutableListOf<List<ARPoint>>()
        var currentLine = mutableListOf<ARPoint>()

        var lastPixel: PixelCoordinate? = null
        var i = 0

        points.first.forEachLine { x1, y1, x2, y2 ->
            val pixel1 = PixelCoordinate(x1, y1)
            val pixel2 = PixelCoordinate(x2, y2)
            val last = lastPixel
            val elevation1 = points.second.getOrNull(i) ?: lastElevation
            val elevation2 = points.second.getOrNull(i + 1) ?: lastElevation
            i += 2

            if (last != null && !last.isSamePixel(pixel1) || currentLine.isEmpty()) {
                // There's a split or this is the first point
                lines.add(currentLine)
                currentLine = mutableListOf()
                // Add the first point
                val spherical = toARPoint(pixel1, elevation1)

                if (spherical == null) {
                    // The start point is too far away, skip this line segment (no need to modify the current line)
                    lastPixel = null
                    return@forEachLine
                }
            }

            // The line continues
            val spherical = toARPoint(pixel2, elevation2)
            if (spherical == null) {
                // The point is too far away, break the line and skip this point
                lines.add(currentLine)
                currentLine = mutableListOf()
                lastPixel = null
                return@forEachLine
            }

            // Otherwise add the point
            currentLine.add(spherical)
            lastPixel = pixel2
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }


        return lines.filterNot { it.size < 2 }
    }

    private fun getNearbyARPoints(
        path: IMappablePath,
        elevation: Float?
    ): Pair<MutableList<Float>, MutableList<Float>> {
        // It is easier to work with the points if they are projected onto a cartesian plane

        // Step 1: Project the path points
        val projected = path.points.map {
            project(it.coordinate)
        }

        val z = path.points.map { it.elevation ?: elevation ?: 0f }


        // Step 2: Clip the projected points
        val output = mutableListOf<Float>()
        val zOutput = mutableListOf<Float>()
        clipper.clip(
            projected,
            bounds,
            output,
            rdpFilterEpsilon = pathSimplification,
            zValues = z,
            zOutput = zOutput
        )

        // Step 3: Interpolate between the points for a higher resolution
        val output2 = mutableListOf<Float>()
        val zOutput2 = mutableListOf<Float>()
        interpolator.increaseResolution(
            output,
            output2,
            pointSpacing,
            zOutput,
            zOutput2
        )

        return output2 to zOutput2
    }

    private fun recenterPoints(
        points: MutableList<Float>,
        oldCenter: PixelCoordinate,
        newCenter: PixelCoordinate
    ) {
        for (i in points.indices step 2) {
            val x = points[i]
            val y = points[i + 1]
            points[i] = x - oldCenter.x + newCenter.x
            points[i + 1] = y - oldCenter.y + newCenter.y
        }
    }

    // TODO: Extract this to sol
    private fun projectOntoLine(
        x: Float,
        y: Float,
        x1: Float,
        y1: Float,
        z1: Float,
        x2: Float,
        y2: Float,
        z2: Float
    ): Pair<PixelCoordinate, Float> {
        val ab = square(x2 - x1) + square(y2 - y1)
        val ap = square(x - x1) + square(y - y1)
        val bp = square(x - x2) + square(y - y2)

        val t = ((ap - bp + ab) / (2 * ab)).coerceIn(0f, 1f)
        val projectedX = x1 + t * (x2 - x1)
        val projectedY = y1 + t * (y2 - y1)
        val projectedZ = z1 + t * (z2 - z1)
        return PixelCoordinate(projectedX, projectedY) to projectedZ
    }

    private fun project(
        location: Coordinate
    ): PixelCoordinate {
        return projection?.toPixels(location)?.let {
            PixelCoordinate(it.x, it.y)
        } ?: PixelCoordinate(0f, 0f)
    }

    private fun toLocation(pixel: PixelCoordinate): Coordinate? {
        val squareDistance = pixel.squaredDistanceTo(center)
        if (squareDistance > squareViewDistance) {
            // The point is too far away
            return null
        }

        return projection?.toCoordinate(Vector2(pixel.x, pixel.y))
    }

    private fun toARPoint(pixel: PixelCoordinate, elevation: Float?): ARPoint? {
        val location = toLocation(pixel) ?: return null
        val elevationOffset = if (adjustForPathElevation && elevation != null) {
            (elevation - (lastElevation ?: 0f) + defaultElevationOffset)
        } else {
            defaultElevationOffset
        }
        return GeographicARPoint(
            location,
            elevationOffset.coerceIn(-maxElevationOffset, maxElevationOffset),
            isElevationRelative = true,
            actualDiameter = 0.25f
        )
    }

    private class NearestPoint(
        val point: PixelCoordinate,
        val elevation: Float,
        val path: Pair<MutableList<Float>, MutableList<Float>>,
        val previousIndex: Int?,
        val nextIndex: Int?
    ) {
        fun addToPath() {
            if (previousIndex == null || nextIndex == null) {
                return
            }

            path.first.add(previousIndex * 2 + 2, point.x)
            path.first.add(previousIndex * 2 + 3, point.y)
            path.first.add(previousIndex * 2 + 4, point.x)
            path.first.add(previousIndex * 2 + 5, point.y)
            path.second.add(previousIndex + 1, elevation)
            path.second.add(previousIndex + 2, elevation)
        }
    }
}