package com.kylecorry.trail_sense.tools.augmented_reality.ui

import androidx.annotation.ColorInt
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.ARPoint

data class ARLine(
    val points: List<ARPoint>,
    @ColorInt val color: Int,
    val thickness: Float,
    val thicknessUnits: ThicknessUnits = ThicknessUnits.Dp
) {
    // TODO: Extract this
    enum class ThicknessUnits {
        Dp, Angle
    }
}