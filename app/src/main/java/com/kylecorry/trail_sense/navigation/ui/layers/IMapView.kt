package com.kylecorry.trail_sense.navigation.ui.layers

interface IMapView {
    fun addLayer(layer: ILayer)
    fun removeLayer(layer: ILayer)
    fun setLayers(layers: List<ILayer>)
}