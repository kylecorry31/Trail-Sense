package com.kylecorry.trail_sense.shared.map_layers

import android.content.Context
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection

class MapViewState(
    val context: Context,
    val viewBounds: Rectangle,
    val bounds: CoordinateBounds,
    val projection: IMapViewProjection
)
