package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import com.kylecorry.trail_sense.shared.map_layers.MapViewState

interface IAsyncLayer : ILayer {
    fun setHasUpdateListener(listener: (() -> Unit)?)

    /**
     * Called when the bounds or projection of the map view change
     */
    fun onViewChanged(state: MapViewState) {
        // Do nothing
    }
}
