package com.kylecorry.trail_sense.tools.navigation.ui.compass.layers

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.trail_sense.shared.domain.IMappableBearing
import com.kylecorry.trail_sense.tools.navigation.ui.compass.ICompassView

class BearingCompassLayer : ICompassLayer {

    private val bearings = mutableListOf<IMappableBearing>()
    private val lock = Any()

    fun addBearing(bearing: IMappableBearing) {
        synchronized(lock) {
            bearings.add(bearing)
        }
    }

    fun clearBearings() {
        synchronized(lock) {
            bearings.clear()
        }
    }

    override fun draw(drawer: ICanvasDrawer, compass: ICompassView) {
        val bearings = synchronized(lock) {
            bearings.toList()
        }
        bearings.forEach {
            compass.draw(it)
        }
    }

    override fun invalidate() {
    }
}
