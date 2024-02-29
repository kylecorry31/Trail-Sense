package com.kylecorry.trail_sense.tools.augmented_reality.ui.layers

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.square
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.math.analysis.Trigonometry
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.units.Bearing
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
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.ui.IPathLayer
import kotlin.math.atan2
import kotlin.math.sqrt

class ARPathLayer(
    viewDistanceMeters: Float,
    private val onFocus: (path: IMappablePath) -> Boolean = { false },
) : ARLayer, IPathLayer {

    private val lineLayer = ARLineLayer(renderWithPaths = false)
    private val markerLayer = ARMarkerLayer(1f, 32f)
    private var lastLocation = Coordinate.zero

    private val squareViewDistance = square(viewDistanceMeters)
    private val center = PixelCoordinate(viewDistanceMeters, viewDistanceMeters)
    private val bounds = Rectangle(
        0f,
        viewDistanceMeters * 2,
        viewDistanceMeters * 2,
        0f,
    )
    private val clipper = LineClipper()
    private val navigation = NavigationService()
    private val interpolator = LineInterpolator()

    private val pointSpacing = 5f // meters
    private val pathSimplification = 0.5f // meters
    private val snapDistance = 2 * viewDistanceMeters / 3f // meters
    private val snapDistanceSquared = square(snapDistance)

    override suspend fun update(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        lastLocation = view.location
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
        val location = lastLocation

        val points = paths.map {
            it to getNearbyARPoints(it, location)
        }

        val nearestPoints = points.mapNotNull { getNearestPoint(it.second) }
        val nearest = nearestPoints.minByOrNull { it.squaredDistanceTo(center) }
        if (nearest != null) {
            points.forEach {
                recenterPoints(it.second, nearest, center)
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

    private fun getNearestPoint(points: List<Float>): PixelCoordinate? {
        var minPoint: PixelCoordinate? = null
        var minDistance = snapDistanceSquared

        points.forEachLine { x1, y1, x2, y2 ->
            val projected = projectOntoLine(center.x, center.y, x1, y1, x2, y2)
            val distance = projected.squaredDistanceTo(center)

            if (distance < minDistance) {
                minDistance = distance
                minPoint = projected
            }
        }

        return minPoint
    }

    private fun project(
        points: MutableList<Float>
    ): List<List<ARPoint>> {
        // Step 4: Convert the points back to geographic coordinates and split the lines
        val lines = mutableListOf<List<ARPoint>>()
        var currentLine = mutableListOf<ARPoint>()

        var lastPixel: PixelCoordinate? = null

        points.forEachLine { x1, y1, x2, y2 ->
            val pixel1 = PixelCoordinate(x1, y1)
            val pixel2 = PixelCoordinate(x2, y2)
            val last = lastPixel

            if (last != null && !last.isSamePixel(pixel1) || currentLine.isEmpty()) {
                // There's a split or this is the first point
                lines.add(currentLine)
                currentLine = mutableListOf()
                // Add the first point
                val spherical = toARPoint(pixel1)

                if (spherical == null) {
                    // The start point is too far away, skip this line segment (no need to modify the current line)
                    lastPixel = null
                    return@forEachLine
                }
            }

            // The line continues
            val spherical = toARPoint(pixel2)
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
        location: Coordinate
    ): MutableList<Float> {
        // It is easier to work with the points if they are projected onto a cartesian plane

        // Step 1: Project the path points
        val projected = path.points.map {
            project(it.coordinate, location)
        }

        // Step 2: Clip the projected points
        val output = mutableListOf<Float>()
        clipper.clip(
            projected,
            bounds,
            output,
            rdpFilterEpsilon = pathSimplification
        )

        // Step 3: Interpolate between the points for a higher resolution
        val output2 = mutableListOf<Float>()
        interpolator.increaseResolution(
            output,
            output2,
            pointSpacing
        )

        return output2
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
        x2: Float,
        y2: Float
    ): PixelCoordinate {
        val ab = square(x2 - x1) + square(y2 - y1)
        val ap = square(x - x1) + square(y - y1)
        val bp = square(x - x2) + square(y - y2)

        val t = ((ap - bp + ab) / (2 * ab)).coerceIn(0f, 1f)
        val projectedX = x1 + t * (x2 - x1)
        val projectedY = y1 + t * (y2 - y1)
        return PixelCoordinate(projectedX, projectedY)
    }

    // TODO: Extract this to sol (azimuthal equidistant projection)
    private fun project(
        location: Coordinate,
        myLocation: Coordinate
    ): PixelCoordinate {
        val vector = navigation.navigate(myLocation, location, 0f, true)
        val angle = Trigonometry.toUnitAngle(vector.direction.value, 90f, false)
        val pixelDistance = vector.distance // Assumes 1 meter = 1 pixel
        val xDiff = SolMath.cosDegrees(angle) * pixelDistance
        val yDiff = SolMath.sinDegrees(angle) * pixelDistance
        return PixelCoordinate(center.x + xDiff, center.y - yDiff)
    }

    private fun toLocation(pixel: PixelCoordinate): Coordinate? {
        // The line continues
        val angle = Trigonometry.toUnitAngle(
            atan2(center.y - pixel.y, pixel.x - center.x).toDegrees(),
            90f,
            false
        )
        val squareDistance = pixel.squaredDistanceTo(center)
        if (squareDistance > squareViewDistance) {
            // The point is too far away
            return null
        }

        val distance = sqrt(squareDistance)

        // Otherwise add the point
        return lastLocation.plus(distance.toDouble(), Bearing(angle))
    }

    private fun toARPoint(pixel: PixelCoordinate): ARPoint? {
        val location = toLocation(pixel) ?: return null
        return GeographicARPoint(
            location,
            -2f,
            isElevationRelative = true,
            actualDiameter = 0.25f
        )
    }
}