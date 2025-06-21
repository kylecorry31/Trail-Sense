package com.kylecorry.trail_sense.shared.dem

import android.graphics.Color
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.tools.navigation.ui.layers.ILayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.IMapView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ElevationLayer : ILayer {

    private val runner = CoroutineQueueRunner(2)
    private val scope = CoroutineScope(Dispatchers.Default)
    private var lastBounds: CoordinateBounds = CoordinateBounds.empty
    private var lastMetersPerPixel: Float? = null

    private var elevations = listOf<Pair<Coordinate, Float>>()

    override fun draw(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        val bounds = map.mapBounds
        if (!areBoundsEqual(
                lastBounds,
                bounds
            ) || map.metersPerPixel != lastMetersPerPixel
        ) {
            scope.launch {
                // TODO: Debounce loader
                runner.enqueue {
                    // 10 x 10 grid of coordinates
                    val coordinates = mutableListOf<Coordinate>()

                    var lat = bounds.south
                    while (lat <= bounds.north) {
                        var lon = bounds.west
                        while (lon <= bounds.east) {
                            coordinates.add(Coordinate(lat.roundPlaces(5), lon.roundPlaces(5)))
                            lon += (bounds.east - bounds.west) / 5
                        }
                        lat += (bounds.north - bounds.south) / 5
                    }

                    elevations = DEM.getElevations(coordinates).map {
                        it.first to it.second.convertTo(DistanceUnits.Feet).distance
                    }

                }
            }

            lastBounds = bounds
            lastMetersPerPixel = map.metersPerPixel
        }

        for (elevation in elevations) {
            val pixel = map.toPixel(elevation.first)
            val elevationValue = elevation.second
            drawer.textMode(TextMode.Center)
            drawer.fill(Color.BLACK)
            drawer.noStroke()
            drawer.textSize(drawer.sp(12f))
            drawer.text(
                "${elevationValue.roundToInt()}",
                pixel.x,
                pixel.y
            )
        }

    }

    override fun drawOverlay(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        // Do nothing
    }

    override fun invalidate() {
        // Do nothing
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        map: IMapView,
        pixel: PixelCoordinate
    ): Boolean {
        return false
    }

    private fun areBoundsEqual(bounds1: CoordinateBounds, bound2: CoordinateBounds): Boolean {
        return bounds1.north == bound2.north &&
                bounds1.south == bound2.south &&
                bounds1.east == bound2.east &&
                bounds1.west == bound2.west
    }
}