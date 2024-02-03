package com.kylecorry.trail_sense.tools.augmented_reality.ui.layers

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath
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
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.SphericalARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.ui.ARLine
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.tools.paths.ui.IPathLayer
import kotlin.math.atan2
import kotlin.math.sqrt

class ARPathLayer : ARLayer, IPathLayer {

    private val lineLayer = ARLineLayer()
    private var lastLocation = Coordinate.zero

    private val squareViewDistance = SolMath.square(VIEW_DISTANCE_METERS)
    private val degreesPerMeter = 75f / VIEW_DISTANCE_METERS
    private val center = PixelCoordinate(VIEW_DISTANCE_METERS, VIEW_DISTANCE_METERS)
    private val bounds = Rectangle(
        0f,
        VIEW_DISTANCE_METERS * 2,
        VIEW_DISTANCE_METERS * 2,
        0f,
    )
    private val clipper = LineClipper()
    private val navigation = NavigationService()
    private val interpolator = LineInterpolator()

    private val pointSpacing = 0.5f // meters
    private val pathSimplification = 0.5f // meters (high quality)

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

        val lines = paths.flatMap {
            getNearbyARPoints(it, location).map { points ->
                ARLine(points, it.color, 16f)
            }
        }

        lineLayer.setLines(lines)
    }

    private fun getNearbyARPoints(
        path: IMappablePath,
        location: Coordinate
    ): List<List<ARPoint>> {
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

        // Step 4: Convert the points back to geographic coordinates and split the lines
        val lines = mutableListOf<List<ARPoint>>()
        var currentLine = mutableListOf<ARPoint>()

        var lastPixel: PixelCoordinate? = null

        for (i in output2.indices step 4) {
            val x1 = output2[i]
            val y1 = output2[i + 1]
            val x2 = output2[i + 2]
            val y2 = output2[i + 3]

            val pixel1 = PixelCoordinate(x1, y1)
            val pixel2 = PixelCoordinate(x2, y2)

            if (lastPixel != null && !lastPixel.isSamePixel(pixel1) || currentLine.isEmpty()) {
                // There's a split or this is the first point
                lines.add(currentLine)
                currentLine = mutableListOf()
                // Add the first point
                val spherical = toSpherical(pixel1)

                if (spherical == null) {
                    // The start point is too far away, skip this line segment (no need to modify the current line)
                    lastPixel = null
                    continue
                }
            }

            // The line continues
            val spherical = toSpherical(pixel2)
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

    private fun toSpherical(pixel: PixelCoordinate): ARPoint? {
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

        // Otherwise add the point
        return SphericalARPoint(
            Bearing.getBearing(angle),
            -90 + sqrt(squareDistance) * degreesPerMeter
        )
    }

    companion object {
        const val VIEW_DISTANCE_METERS = 6f
    }

}