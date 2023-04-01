package com.kylecorry.trail_sense.navigation.ui.layers.compass

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.trail_sense.navigation.ui.IMappableBearing

class BearingCompassLayer : ICompassLayer {

    private val bearings = mutableListOf<IMappableBearing>()
    private val lock = Any()

    fun addBearing(bearing: IMappableBearing) {
        synchronized(lock) {
            bearings.add(bearing)
        }
    }

    fun removeBearing(bearing: IMappableBearing) {
        synchronized(lock) {
            bearings.remove(bearing)
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