package com.kylecorry.trail_sense.navigation.ui.layers.compass

import com.kylecorry.andromeda.canvas.ICanvasDrawer

interface ICompassLayer {
    fun draw(drawer: ICanvasDrawer, compass: ICompassView)

    @Suppress("EmptyMethod")
    fun invalidate()
}