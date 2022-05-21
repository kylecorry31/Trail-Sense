package com.kylecorry.trail_sense.navigation.ui.layers

import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.maps.ICoordinateToPixelStrategy

interface IMapView {
    fun addLayer(layer: ILayer)
    fun removeLayer(layer: ILayer)
    fun setLayers(layers: List<ILayer>)
    fun setLocation(location: Coordinate)
    fun setAzimuth(azimuth: Bearing)
    fun setDeclination(declination: Float)

    // TODO: Make this a method rather than a strategy
    val coordinateToPixelStrategy: ICoordinateToPixelStrategy
    // TODO: pixel to coordinate strategy
}