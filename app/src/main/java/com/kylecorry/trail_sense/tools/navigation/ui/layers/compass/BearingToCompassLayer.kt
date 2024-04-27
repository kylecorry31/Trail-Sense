package com.kylecorry.trail_sense.tools.navigation.ui.layers.compass

import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import com.kylecorry.trail_sense.tools.navigation.ui.IMappableBearing
import com.kylecorry.trail_sense.tools.navigation.ui.MappableBearing

class BearingToCompassLayer : ICompassLayer {

    private var destination: Coordinate? = null
    private var bearing: IMappableBearing? = null
    @ColorInt
    private var color = 0

    fun setDestination(location: Coordinate, @ColorInt color: Int){
        destination = location
        bearing = null
        this.color = color
    }

    override fun draw(drawer: ICanvasDrawer, compass: ICompassView) {
        val destination = destination
        var direction = bearing
        if (destination != null && direction == null) {
            val trueDirection = compass.compassCenter.bearingTo(destination)
            val b = if (compass.useTrueNorth) {
                trueDirection
            } else {
                DeclinationUtils.fromTrueNorthBearing(trueDirection, compass.declination)
            }
            direction = MappableBearing(b, color)
            bearing = direction
        }

        if (destination != null && direction != null) {
            compass.draw(direction, destination)
        }

    }

    override fun invalidate() {
    }
}