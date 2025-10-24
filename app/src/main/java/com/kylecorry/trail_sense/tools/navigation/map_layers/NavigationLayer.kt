package com.kylecorry.trail_sense.tools.navigation.map_layers

import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.BaseLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationStrategy
import com.kylecorry.trail_sense.tools.navigation.ui.MappableLocation
import com.kylecorry.trail_sense.tools.navigation.ui.MappablePath
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.tools.paths.map_layers.PathLayer

class NavigationLayer : BaseLayer() {

    private val pathLayer = PathLayer()

    private var _myLocation: Coordinate? = null
    private var _navigationStrategy: NavigationStrategy? = null

    fun setMyLocation(location: Coordinate?) {
        _myLocation = location
        updatePathLayer()
        invalidate()
    }

    fun setNavigation(navigationStrategy: NavigationStrategy?) {
        _navigationStrategy = navigationStrategy
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
        val navigationStrategy = _navigationStrategy

        val paths = if (navigationStrategy != null && myLocation != null) {
            createPath(myLocation, navigationStrategy)
        } else {
            emptyList()
        }

        pathLayer.setPaths(paths)
    }

    private fun createPath(
        myLocation: Coordinate,
        strategy: NavigationStrategy
    ): List<MappablePath> {
        return when (strategy) {
            is NavigationStrategy.Beacon -> createBeaconPath(myLocation, strategy)
            is NavigationStrategy.Bearing -> createBearingPath(myLocation, strategy)
        }
    }

    private fun createBeaconPath(
        myLocation: Coordinate,
        beacon: NavigationStrategy.Beacon
    ): List<MappablePath> {
        return listOf(
            createPath(myLocation, beacon.beacon.coordinate, beacon.beacon.color)
        )
    }

    private fun createBearingPath(
        myLocation: Coordinate,
        bearing: NavigationStrategy.Bearing
    ): List<MappablePath> {
        // TODO: Load bearing color
        // TODO: Line from my location to the bearing line
        val adjustedBearing = if (bearing.isTrueNorth) {
            bearing.bearing
        } else {
            bearing.bearing.withDeclination(-bearing.declination)
        }
        return if (bearing.startingLocation != null) {
            listOf(
                createPath(
                    bearing.startingLocation,
                    bearing.startingLocation.plus(Distance.kilometers(80f), adjustedBearing),
                    AppColor.Orange.color
                )
            )
        } else {
            listOf(
                createPath(
                    myLocation,
                    myLocation.plus(Distance.kilometers(80f), adjustedBearing),
                    AppColor.Orange.color
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