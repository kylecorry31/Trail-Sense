package com.kylecorry.trail_sense.shared.map_layers.ui.layers

interface IAsyncLayer : ILayer {
    fun setHasUpdateListener(listener: (() -> Unit)?)
}