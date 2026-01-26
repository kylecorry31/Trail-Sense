package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.units.Coordinate

interface IMapViewProjection : IMapProjection {
    val resolutionPixels: Float
    val zoom: Float
    val resolution: Float
    val center: Coordinate
}