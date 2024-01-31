package com.kylecorry.trail_sense.tools.augmented_reality

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.tools.augmented_reality.position.GeographicARPoint
import com.kylecorry.trail_sense.tools.navigation.ui.IMappableLocation
import com.kylecorry.trail_sense.tools.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.tools.navigation.ui.MappablePath
import com.kylecorry.trail_sense.tools.paths.ui.IPathLayer

class ARPathLayer(private val viewDistance: Distance) : ARLayer, IPathLayer {

    private val lineLayer = ARLineLayer()
    private val pointLayer = ARMarkerLayer(0.1f, 6f)
    private var lastLocation = Coordinate.zero
    private val viewDistanceMeters = viewDistance.meters().distance

    override fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        lastLocation = view.location
        lineLayer.draw(drawer, view)
        pointLayer.draw(drawer, view)
    }

    override fun invalidate() {
        lineLayer.invalidate()
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

        val splitPaths = paths.flatMap { path ->
            clipPath(path, location, viewDistanceMeters)
        }

        lineLayer.setLines(splitPaths.map { path ->
            ARLine(
                path.points.map {
                    GeographicARPoint(it.coordinate, it.elevation)
                },
                path.color,
                1f,
                ARLine.ThicknessUnits.Dp
            )
        })

        // Only render the closest 20 points
        val nearby = splitPaths.flatMap { path ->
            path.points.map {
                it to path.color
            }
        }.sortedBy {
            it.first.coordinate.distanceTo(location)
        }.take(20).map {
            ARMarker(
                GeographicARPoint(it.first.coordinate, it.first.elevation),
                CanvasCircle(it.second)
            )
        }
        pointLayer.setMarkers(nearby)
    }

    private fun clipPath(path: IMappablePath, location: Coordinate, distance: Float): List<IMappablePath> {
        val clipped = mutableListOf<IMappablePath>()
        val currentPoints = mutableListOf<IMappableLocation>()

        for (point in path.points) {
            if (point.coordinate.distanceTo(location) < distance) {
                currentPoints.add(point)
            } else {
                // TODO: Clip instead of remove
                if (currentPoints.isNotEmpty()) {
                    clipped.add(MappablePath(path.id, currentPoints.toList(), path.color, path.style))
                    currentPoints.clear()
                }
            }
        }

        if (currentPoints.isNotEmpty()) {
            clipped.add(MappablePath(path.id, currentPoints.toList(), path.color, path.style))
        }

        return clipped
    }
}