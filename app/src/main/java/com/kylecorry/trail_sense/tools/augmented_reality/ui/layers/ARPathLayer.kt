package com.kylecorry.trail_sense.tools.augmented_reality.ui.layers

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.math.analysis.Trigonometry
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.canvas.LineClipper
import com.kylecorry.trail_sense.shared.canvas.LineInterpolator
import com.kylecorry.trail_sense.shared.extensions.squaredDistanceTo
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.ARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.GeographicARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.ui.ARMarker
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import com.kylecorry.trail_sense.tools.augmented_reality.ui.CanvasCircle
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.navigation.ui.IMappableLocation
import com.kylecorry.trail_sense.tools.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.tools.paths.ui.IPathLayer
import java.util.stream.Collectors.toList
import kotlin.math.atan2
import kotlin.math.sqrt

class ARPathLayer(viewDistance: Distance) : ARLayer, IPathLayer {

    private val pointLayer = ARMarkerLayer(0.2f, 16f)
    private var lastLocation = Coordinate.zero
    private var lastElevation: Float? = null
    private val viewDistanceMeters = viewDistance.meters().distance

    // A limit to ensure performance is not impacted
    private val nearbyLimit = 20

    // The distance at which the elevation of the closest point is used to adjust the elevation of all points
    private val elevationOverrideDistance = 30f // meters
    private val squareElevationOverrideDistance = SolMath.square(elevationOverrideDistance)

    private val pointSpacing = 7f // meters
    private val pathSimplification = 2f // meters (high quality)

    override fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        lastLocation = view.location
        lastElevation = view.altitude
        pointLayer.draw(drawer, view)
    }

    override fun invalidate() {
        pointLayer.invalidate()
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
        val elevation = lastElevation

        val markers = paths.flatMap { path ->
            val nearby = getNearbyARPoints(path, location, elevation, viewDistanceMeters)
            nearby.map {
                ARMarker(
                    it,
                    CanvasCircle(path.color)
                )
            }
        }

        pointLayer.setMarkers(markers)
    }

    private fun getNearbyARPoints(
        path: IMappablePath,
        location: Coordinate,
        elevation: Float?,
        viewDistance: Float
    ): List<ARPoint> {
        val bounds = Rectangle(
            0f,
            viewDistance * 2,
            viewDistance * 2,
            0f,
        )

        // Step 1: Project the path points
        val projected = project(path.points, location, viewDistance, viewDistance)
        val fallbackElevation = path.points.firstOrNull { it.elevation != null }?.elevation
        val hasElevation = fallbackElevation != null
        val z =
            if (hasElevation) path.points.map { it.elevation ?: fallbackElevation ?: 0f } else null

        // Step 2: Clip the projected points
        val clipper = LineClipper()
        val output = mutableListOf<Float>()
        val zOutput = if (hasElevation) mutableListOf<Float>() else null
        clipper.clip(
            projected,
            bounds,
            output,
            zValues = z,
            zOutput = zOutput,
            rdpFilterEpsilon = pathSimplification
        )


        // Step 3: Interpolate between the points for a higher resolution
        val interpolator = LineInterpolator()
        val output2 = mutableListOf<Float>()
        val zOutput2 = if (hasElevation) mutableListOf<Float>() else null
        interpolator.increaseResolution(
            output,
            output2,
            pointSpacing,
            z = zOutput,
            zOutput = zOutput2
        )

        // Reduce the memory footprint
        output.clear()
        zOutput?.clear()

        // Step 4: Cleanup and sorting
        val points = mutableListOf<Pair<PixelCoordinate, Float?>>()
        for (i in output2.indices step 2) {
            val x = output2[i]
            val y = output2[i + 1]
            val pointZ = zOutput2?.getOrNull(i / 2)
            points.add(PixelCoordinate(x, y) to pointZ)
        }

        // Reduce the memory footprint
        output2.clear()
        zOutput2?.clear()

        val center = PixelCoordinate(viewDistance, viewDistance)

        // Remove duplicate points and take top n closest points
        val nearby = points
            .stream()
            .distinct()
            .sorted { p1, p2 ->
                p1.first.squaredDistanceTo(center).compareTo(p2.first.squaredDistanceTo(center))
            }
            .limit(nearbyLimit.toLong())
            .collect(toList())
            .reversed()

        // Step 5: Adjust the elevation of the points
        val closest = nearby.lastOrNull()
        val closestSquareDist = closest?.first?.squaredDistanceTo(center)

        // If the closest point is within the elevation override distance, override the elevation of all points
        val elevationOffset =
            if (hasElevation && elevation != null && closestSquareDist != null && closestSquareDist < squareElevationOverrideDistance) {
                closest.second?.minus(elevation) ?: 0f
            } else {
                null
            }

        // Step 6: Convert the points to AR points
        return nearby.map {
            GeographicARPoint(
                inverseProject(
                    it.first,
                    location,
                    viewDistance,
                    viewDistance
                ),
                it.second?.minus(elevationOffset ?: 0f)
            )
        }
    }

    private fun project(
        points: List<IMappableLocation>,
        location: Coordinate,
        centerX: Float,
        centerY: Float
    ): List<PixelCoordinate> {
        return points.map {
            project(it.coordinate, location, centerX, centerY)
        }
    }

    private val navigation = NavigationService()
    private fun project(
        location: Coordinate,
        myLocation: Coordinate,
        centerX: Float,
        centerY: Float
    ): PixelCoordinate {
        val vector = navigation.navigate(myLocation, location, 0f, true)
        val angle = Trigonometry.toUnitAngle(vector.direction.value, 90f, false)
        val pixelDistance = vector.distance // Assumes 1 meter = 1 pixel
        val xDiff = SolMath.cosDegrees(angle) * pixelDistance
        val yDiff = SolMath.sinDegrees(angle) * pixelDistance
        return PixelCoordinate(centerX + xDiff, centerY - yDiff)
    }

    private fun inverseProject(
        pixel: PixelCoordinate,
        myLocation: Coordinate,
        centerX: Float,
        centerY: Float
    ): Coordinate {
        val xDiff = pixel.x - centerX
        val yDiff = centerY - pixel.y
        val pixelDistance = sqrt(xDiff * xDiff + yDiff * yDiff)
        val angle = atan2(yDiff, xDiff).toDegrees()
        val direction = Trigonometry.remapUnitAngle(angle, 90f, false)
        return myLocation.plus(pixelDistance.toDouble(), Bearing(direction))
    }
}