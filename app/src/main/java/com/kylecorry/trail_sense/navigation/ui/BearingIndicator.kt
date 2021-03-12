package com.kylecorry.trail_sense.navigation.ui

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.kylecorry.trailsensecore.domain.geo.Bearing

data class BearingIndicator(val bearing: Bearing, @DrawableRes val icon: Int, @ColorInt val tint: Int? = null, val opacity: Float = 1f, val verticalOffset: Float = 0f)