package com.kylecorry.trail_sense.diagnostics.status

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes

data class StatusBadge(val name: String, @ColorInt val color: Int, @DrawableRes val icon: Int)
