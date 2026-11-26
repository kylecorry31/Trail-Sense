package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import com.kylecorry.trail_sense.shared.map_layers.MapViewState

interface IAsyncLayer : ILayer {
    fun setHasUpdateListener(listener: (() -> Unit)?)

    fun onViewChanged(state: MapViewState) {
    }
}
