package com.kylecorry.trail_sense.navigation.ui.layers.compass

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.ui.IMappableBearing
import com.kylecorry.trail_sense.navigation.ui.MappableReferencePoint

class NavigationCompassLayer : ICompassLayer {

    private val beaconLayer = BeaconCompassLayer()
    private val markerLayer = MarkerCompassLayer()
    private val bearingLayer = BearingCompassLayer()
    private val bearingToLayer = BearingToCompassLayer()

    private var destination: Beacon? = null
    private var bearing: IMappableBearing? = null

    fun setDestination(destination: Beacon?) {
        this.destination = destination
        bearing = null
    }

    fun setDestination(bearing: IMappableBearing?) {
        this.bearing = bearing
        destination = null
    }

    override fun draw(drawer: ICanvasDrawer, compass: ICompassView) {
        val dest = destination
        val direction = bearing


        if (dest != null) {
            // Draw bearing
            bearingToLayer.setDestination(dest.coordinate, dest.color)
            bearingToLayer.draw(drawer, compass)

            // Draw marker
            beaconLayer.setBeacons(listOf(dest))
            beaconLayer.draw(drawer, compass)
        } else if (direction != null) {
            // Draw bearing
            bearingLayer.clearBearings()
            bearingLayer.addBearing(direction)
            bearingLayer.draw(drawer, compass)

            // Draw marker
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
            markerLayer.draw(drawer, compass)
        }
    }

    override fun invalidate() {
    }
}