package com.kylecorry.trail_sense.tools.survival_guide.domain

import androidx.annotation.DrawableRes

data class Chapter(
    val title: String,
    val chapter: String,
    val resource: Int,
    @DrawableRes val icon: Int
)
