package com.kylecorry.trail_sense.tools.navigation.ui.layers.compass

import com.kylecorry.andromeda.canvas.ICanvasDrawer

interface ICompassLayer {
    fun draw(drawer: ICanvasDrawer, compass: ICompassView)
    fun invalidate()
}