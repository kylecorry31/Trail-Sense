package com.kylecorry.trail_sense.shared.map_layers.ui

import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView

interface IMapLayerManager {
    fun resume(view: IMapView, fragment: Fragment, onLayersChanged: () -> Unit = {})

    fun pause(view: IMapView)
}
