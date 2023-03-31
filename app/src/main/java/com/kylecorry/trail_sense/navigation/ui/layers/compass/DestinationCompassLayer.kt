package com.kylecorry.trail_sense.navigation.ui.layers.compass

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.ui.IMappableBearing
import com.kylecorry.trail_sense.navigation.ui.MappableBearing
import com.kylecorry.trail_sense.navigation.ui.MappableReferencePoint
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils

class DestinationCompassLayer : ICompassLayer {

    private val beaconLayer = BeaconCompassLayer()
    private val markerLayer = MarkerCompassLayer()
    private val bearingLayer = BearingCompassLayer()

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
        var direction = bearing

        if (dest != null && direction == null) {
            val trueDirection = compass.compassCenter.bearingTo(dest.coordinate)

            val b = if (compass.useTrueNorth) {
                trueDirection
            } else {
                DeclinationUtils.fromTrueNorthBearing(trueDirection, compass.declination)
            }

            bearing = MappableBearing(b, dest.color)
            direction = bearing
        }

        if (dest != null) {
            beaconLayer.setBeacons(listOf(dest))
            beaconLayer.draw(drawer, compass)
        }

        if (direction != null) {
            bearingLayer.clearBearings()
            bearingLayer.addBearing(direction)
            bearingLayer.draw(drawer, compass)
        }

        if (dest == null && direction != null) {
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