package com.kylecorry.trail_sense.tools.navigation.ui.layers

interface IAsyncLayer : ILayer {
    fun setHasUpdateListener(listener: (() -> Unit)?)
}