package com.kylecorry.trail_sense.tools.augmented_reality.ui.layers

import androidx.annotation.ColorInt
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.AugmentedRealityCoordinate

/**
 * A filled shape in the AR world
 * @param parts the convex polygons that make up the shape, drawn and focused together
 * @param color the fill color
 * @param onFocus called when the shape is focused, return true if focus is claimed. Null if the shape cannot be focused.
 */
class ARPolygon(
    val parts: List<List<AugmentedRealityCoordinate>>,
    @ColorInt val color: Int,
    val onFocus: (() -> Boolean)? = null
)
