package com.kylecorry.trail_sense.tools.navigation.map_layers

import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.BaseLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.ui.MappableLocation
import com.kylecorry.trail_sense.tools.navigation.ui.MappablePath
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.tools.paths.map_layers.PathLayer

class NavigationLayer : BaseLayer() {

    private val pathLayer = PathLayer()

    private var _myLocation: Coordinate? = null
    private var _destination: Destination? = null

    var useLocationWithBearing: Boolean = true
        set(value) {
            field = value
            updatePathLayer()
            invalidate()
        }

    fun setMyLocation(location: Coordinate?) {
        _myLocation = location
        updatePathLayer()
        invalidate()
    }

    fun setDestination(destination: Destination?) {
        _destination = destination
        updatePathLayer()
        invalidate()
    }

    fun setPreferences(prefs: NavigationMapLayerPreferences) {
        setPercentOpacity(prefs.opacity.get() / 100f)
        invalidate()
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        super.draw(drawer, map)
        pathLayer.draw(drawer, map)
    }

    private fun updatePathLayer() {
        val myLocation = _myLocation
        val destination = _destination

        val paths = if (destination != null && myLocation != null) {
            createPath(myLocation, destination)
        } else {
            emptyList()
        }

        pathLayer.setPaths(paths)
    }

    private fun createPath(
        myLocation: Coordinate,
        destination: Destination
    ): List<MappablePath> {
        return when (destination) {
            is Destination.Beacon -> createBeaconPath(myLocation, destination)
            is Destination.Bearing -> createBearingPath(myLocation, destination)
        }
    }

    private fun createBeaconPath(
        myLocation: Coordinate,
        beacon: Destination.Beacon
    ): List<MappablePath> {
        return listOf(
            createPath(myLocation, beacon.beacon.coordinate, beacon.beacon.color)
        )
    }

    private fun createBearingPath(
        myLocation: Coordinate,
        bearing: Destination.Bearing
    ): List<MappablePath> {
        return if (bearing.startingLocation != null && useLocationWithBearing) {
            listOf(
                createPath(
                    bearing.startingLocation,
                    bearing.targetLocation!!,
                    Destination.Bearing.defaultColor
                )
            )
        } else {
            listOf(
                createPath(
                    myLocation,
                    myLocation.plus(Destination.Bearing.bearingDistance, bearing.trueBearing),
                    Destination.Bearing.defaultColor
                )
            )
        }
    }

    private fun createPath(start: Coordinate, end: Coordinate, @ColorInt color: Int): MappablePath {
        return MappablePath(
            -1, listOf(
                MappableLocation(-1, start, color, null),
                MappableLocation(-2, end, color, null)
            ), color, LineStyle.Arrow, thicknessScale = 1.5f
        )
    }
}