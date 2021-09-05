package com.kylecorry.trail_sense.navigation.ui

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.kylecorry.sol.units.Distance

data class BearingIndicator(val bearing: Float, @DrawableRes val icon: Int, @ColorInt val tint: Int? = null, val opacity: Float = 1f, val distance: Distance? = null)