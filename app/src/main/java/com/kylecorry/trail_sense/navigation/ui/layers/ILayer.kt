package com.kylecorry.trail_sense.navigation.ui.layers

import com.kylecorry.andromeda.canvas.ICanvasDrawer

interface ILayer {
    fun draw(drawer: ICanvasDrawer, map: IMapView)
    fun invalidate()
}