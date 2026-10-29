package com.kylecorry.trail_sense.tools.navigation.ui.layers.compass

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.navigation.ui.IMappableBearing
import com.kylecorry.trail_sense.tools.navigation.ui.MappableReferencePoint

class NavigationCompassLayer : ICompassLayer {

    private val beaconLayer = BeaconCompassLayer()
    private val markerLayer = MarkerCompassLayer()
    private val bearingLayer = BearingCompassLayer()
    private val bearingToLayer = BearingToCompassLayer()

    private var destination: Beacon? = null
    private var bearing: IMappableBearing? = null

    private var needsUpdate = true
    private var lastCenter: Coordinate? = null
    private var lastDeclination = 0f
    private var lastUseTrueNorth = false

    fun setDestination(destination: Beacon?) {
        this.destination = destination
        bearing = null
        needsUpdate = true
    }

    fun setDestination(bearing: IMappableBearing?) {
        this.bearing = bearing
        destination = null
        needsUpdate = true
    }

    override fun draw(drawer: ICanvasDrawer, compass: ICompassView) {
        updateLayers(compass)

        if (destination != null) {
            bearingToLayer.draw(drawer, compass)
            beaconLayer.draw(drawer, compass)
        } else if (bearing != null) {
            bearingLayer.draw(drawer, compass)
            markerLayer.draw(drawer, compass)
        }
    }

    private fun updateLayers(compass: ICompassView) {
        if (!hasInputsChanged(compass)) {
            return
        }
        needsUpdate = false
        lastCenter = compass.compassCenter
        lastDeclination = compass.declination
        lastUseTrueNorth = compass.useTrueNorth

        val dest = destination
        val direction = bearing
        if (dest != null) {
            bearingToLayer.setDestination(dest.coordinate, dest.color)
            beaconLayer.setBeacons(listOf(dest))
        } else if (direction != null) {
            bearingLayer.clearBearings()
            bearingLayer.addBearing(direction)

            markerLayer.clearMarkers()
            markerLayer.addMarker(
                MappableReferencePoint(
                    1,
                    R.drawable.ic_arrow_target,
                    direction.bearing,
                    direction.color
                ),
                24
            )
        }
    }

    private fun hasInputsChanged(compass: ICompassView): Boolean {
        return needsUpdate ||
                compass.compassCenter != lastCenter ||
                compass.declination != lastDeclination ||
                compass.useTrueNorth != lastUseTrueNorth
    }

    override fun invalidate() {
    }
}
