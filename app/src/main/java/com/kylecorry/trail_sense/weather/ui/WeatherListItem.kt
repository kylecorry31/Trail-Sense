package com.kylecorry.trail_sense.weather.ui

import androidx.annotation.ColorInt

data class WeatherListItem(
    val id: Long,
    val icon: Int,
    val title: String,
    val value: String,
    @ColorInt val color: Int? = null,
    val onClick: () -> Unit = {}
)