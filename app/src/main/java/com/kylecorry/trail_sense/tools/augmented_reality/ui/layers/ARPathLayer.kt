package com.kylecorry.trail_sense.tools.augmented_reality.ui.layers

import android.graphics.Color
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
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.ARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.GeographicARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.ui.ARLine
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.tools.paths.ui.IPathLayer
import kotlin.math.atan2
import kotlin.math.sqrt

class ARPathLayer(
    private val viewDistanceMeters: Float
) : ARLayer, IPathLayer {

    private val lineLayer = ARLineLayer()
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

    override fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        lastLocation = view.location
        lineLayer.draw(drawer, view)
    }

    override fun invalidate() {
        lineLayer.invalidate()
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        view: AugmentedRealityView,
        pixel: PixelCoordinate
    ): Boolean {
        return false
    }

    override fun onFocus(drawer: ICanvasDrawer, view: AugmentedRealityView): Boolean {
        return false
    }

    override fun setPaths(paths: List<IMappablePath>) {
        val location = lastLocation

        val points = paths.map {
            getNearbyARPoints(it, location) to it.color
        }

        val nearestPoints = points.mapNotNull { getNearestPoint(it.first) }
        val nearest = nearestPoints.minByOrNull { it.squaredDistanceTo(center) }
        if (nearest != null){
            points.forEach {
                recenterPoints(it.first, nearest, center)
            }
        }


        // Add the offset to all the points
        val lines = points.flatMap { (pts, color) ->
            project(pts).map {
                ARLine(it, color, 16f, outlineColor = Color.WHITE)
            }
        }

        lineLayer.setLines(lines)
    }

    private fun getNearestPoint(points: List<Float>): PixelCoordinate? {
        var minIdx = -1
        var minDistance = snapDistanceSquared

        for (i in points.indices step 2) {
            val dx = points[i] - center.x
            val dy = points[i + 1] - center.y
            val distance = dx * dx + dy * dy
            if (distance < minDistance) {
                minDistance = distance
                minIdx = i
            }
        }

        if (minIdx == -1) {
            return null
        }

        val point = PixelCoordinate(points[minIdx], points[minIdx + 1])

        val previous = if (minIdx >= 2) {
            val start = PixelCoordinate(points[minIdx - 2], points[minIdx - 1])
            projectOntoLine(center, start, point)
        } else {
            null
        }
        val previousDistance = previous?.squaredDistanceTo(center) ?: Float.MAX_VALUE

        val next = if (minIdx < points.size - 2) {
            val end = PixelCoordinate(points[minIdx + 2], points[minIdx + 3])
            projectOntoLine(center, point, end)
        } else {
            null
        }
        val nextDistance = next?.squaredDistanceTo(center) ?: Float.MAX_VALUE

        val pointDistance = point.squaredDistanceTo(center)

        return when {
            previousDistance < nextDistance && previousDistance < pointDistance -> previous
            nextDistance < previousDistance && nextDistance < pointDistance -> next
            else -> point
        }
    }

    private fun project(
        points: MutableList<Float>
    ): List<List<ARPoint>> {
        // Step 4: Convert the points back to geographic coordinates and split the lines
        val lines = mutableListOf<List<ARPoint>>()
        var currentLine = mutableListOf<ARPoint>()

        var lastPixel: PixelCoordinate? = null

        for (i in points.indices step 4) {
            val x1 = points[i]
            val y1 = points[i + 1]
            val x2 = points[i + 2]
            val y2 = points[i + 3]

            val pixel1 = PixelCoordinate(x1, y1)
            val pixel2 = PixelCoordinate(x2, y2)

            if (lastPixel != null && !lastPixel.isSamePixel(pixel1) || currentLine.isEmpty()) {
                // There's a split or this is the first point
                lines.add(currentLine)
                currentLine = mutableListOf()
                // Add the first point
                val spherical = toARPoint(pixel1)

                if (spherical == null) {
                    // The start point is too far away, skip this line segment (no need to modify the current line)
                    lastPixel = null
                    continue
                }
            }

            // The line continues
            val spherical = toARPoint(pixel2)
            if (spherical == null) {
                // The point is too far away, break the line and skip this point
                lines.add(currentLine)
                currentLine = mutableListOf()
                lastPixel = null
                continue
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

    private fun recenterPoints(points: MutableList<Float>, oldCenter: PixelCoordinate, newCenter: PixelCoordinate){
        for (i in points.indices step 2){
            val x = points[i]
            val y = points[i + 1]
            points[i] = x - oldCenter.x + newCenter.x
            points[i + 1] = y - oldCenter.y + newCenter.y
        }
    }

    // TODO: This should be extracted
    private fun projectOntoLine(
        point: PixelCoordinate,
        lineStart: PixelCoordinate,
        lineEnd: PixelCoordinate
    ): PixelCoordinate? {
        val ab = lineEnd.distanceTo(lineStart)
        val ap = point.distanceTo(lineStart)
        val bp = point.distanceTo(lineEnd)

        val t = (ap * ap - bp * bp + ab * ab) / (2 * ab * ab)

        if (t < 0 || t > 1 || t.isNaN()) {
            return null
        }

        val x = lineStart.x + t * (lineEnd.x - lineStart.x)
        val y = lineStart.y + t * (lineEnd.y - lineStart.y)
        return PixelCoordinate(x, y)
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
            isElevationRelative = true
        )
    }
}