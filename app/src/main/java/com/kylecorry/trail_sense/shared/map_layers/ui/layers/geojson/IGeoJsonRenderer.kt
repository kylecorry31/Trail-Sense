package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView

interface IGeoJsonRenderer {
    fun setHasUpdateListener(listener: (() -> Unit)?)
    fun draw(drawer: ICanvasDrawer, map: IMapView)
    fun invalidate()
    fun onClick(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean
}