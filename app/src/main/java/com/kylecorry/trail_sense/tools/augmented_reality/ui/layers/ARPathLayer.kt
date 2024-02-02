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
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.ARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.GeographicARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.ui.ARMarker
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import com.kylecorry.trail_sense.tools.augmented_reality.ui.CanvasCircle
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.navigation.ui.IMappableLocation
import com.kylecorry.trail_sense.tools.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.tools.paths.ui.IPathLayer
import kotlin.math.atan2
import kotlin.math.sqrt

class ARPathLayer(viewDistance: Distance) : ARLayer, IPathLayer {

    private val pointLayer = ARMarkerLayer(0.1f, 6f)
    private var lastLocation = Coordinate.zero
    private val viewDistanceMeters = viewDistance.meters().distance

    override fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        lastLocation = view.location
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

        val markers = paths.flatMap { path ->
            val nearby = getNearbyARPoints(path, location, viewDistanceMeters)
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
        viewDistance: Float
    ): List<ARPoint> {
        // Step 1: Project the path points
        val projected = project(path.points, location)
        val hasElevation = path.points.firstOrNull()?.elevation != null
        val z = if (hasElevation) path.points.map { it.elevation ?: 0f } else null

        // Step 2: Clip the projected points
        val clipper = LineClipper()
        val bounds = Rectangle(-viewDistance, viewDistance, viewDistance, -viewDistance)
        val output = mutableListOf<Float>()
        val zOutput = if (hasElevation) mutableListOf<Float>() else null
        clipper.clip(
            projected,
            bounds,
            output,
            zValues = z,
            zOutput = zOutput,
            rdpFilterEpsilon = 10f
        )

        // Step 3: Interpolate between the points for a higher resolution
        // TODO: Not yet implemented

        // Step 4: Convert the clipped points back to geographic points
        var lastX: Float? = null
        var lastY: Float? = null

        val finalPoints = mutableListOf<GeographicARPoint>()

        for (i in output.indices step 2) {
            val x = output[i]
            val y = output[i + 1]
            if (x == lastX && y == lastY) {
                continue
            }
            lastX = x
            lastY = y
            val elevation = zOutput?.getOrNull(i / 2)
            val projectedCoordinate = inverseProject(PixelCoordinate(x, y), location)
            finalPoints.add(GeographicARPoint(projectedCoordinate, elevation))
        }

        return finalPoints

    }

    private fun project(
        points: List<IMappableLocation>,
        location: Coordinate
    ): List<PixelCoordinate> {
        return points.map {
            project(it.coordinate, location)
        }
    }

    private val navigation = NavigationService()
    private fun project(location: Coordinate, myLocation: Coordinate): PixelCoordinate {
        val vector = navigation.navigate(myLocation, location, 0f, true)
        val angle = Trigonometry.toUnitAngle(vector.direction.value, 90f, false)
        val pixelDistance = vector.distance // Assumes 1 meter = 1 pixel
        val xDiff = SolMath.cosDegrees(angle) * pixelDistance
        val yDiff = SolMath.sinDegrees(angle) * pixelDistance
        return PixelCoordinate(xDiff, -yDiff)
    }

    private fun inverseProject(pixel: PixelCoordinate, myLocation: Coordinate): Coordinate {
        val xDiff = pixel.x
        val yDiff = -pixel.y
        val pixelDistance = sqrt(xDiff * xDiff + yDiff * yDiff)
        val angle = atan2(yDiff, xDiff).toDegrees()
        val direction = Trigonometry.remapUnitAngle(angle, 90f, false)
        return myLocation.plus(pixelDistance.toDouble(), Bearing(direction))
    }
}