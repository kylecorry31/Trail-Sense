package com.kylecorry.trail_sense.navigation.ui.layers.compass

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.ui.IMappableBearing
import com.kylecorry.trail_sense.navigation.ui.IMappableReferencePoint

interface ICompassView {

    val compassCenter: Coordinate
    val useTrueNorth: Boolean
    val declination: Float

    fun addCompassLayer(layer: ICompassLayer)
    fun removeCompassLayer(layer: ICompassLayer)
    fun setCompassLayers(layers: List<ICompassLayer>)

    // TODO: Replace mappable reference point with a compass marker
    fun draw(reference: IMappableReferencePoint, size: Int? = null)

    fun draw(bearing: IMappableBearing, stopAt: Coordinate? = null)
}