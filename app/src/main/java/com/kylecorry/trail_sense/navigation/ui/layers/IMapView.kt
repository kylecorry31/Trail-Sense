package com.kylecorry.trail_sense.navigation.ui.layers

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Coordinate

interface IMapView {
    fun addLayer(layer: ILayer)
    fun removeLayer(layer: ILayer)
    fun setLayers(layers: List<ILayer>)

    fun toPixel(coordinate: Coordinate): PixelCoordinate

    /**
     * The scale in meters per pixel
     */
    var metersPerPixel: Float

    /**
     * The scale for each layer element
     * This is likely to be temporary until meters per pixel can be used
     */
    val layerScale: Float

    /**
     * The center of the map
     */
    var centerLocation: Coordinate

    /**
     * The rotation of the map around the center (i.e. the direction the "top" of the map is pointing)
     */
    var mapRotation: Float
}