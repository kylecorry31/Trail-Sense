package com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes

data class ARGuidanceDisplayState(
    val name: String,
    @DrawableRes val icon: Int,
    @ColorInt val iconTint: Int? = null,
    @ColorInt val iconBackgroundTint: Int? = null,
    val iconRotation: Float = 0f
)
