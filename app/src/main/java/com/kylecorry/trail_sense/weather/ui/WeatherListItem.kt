package com.kylecorry.trail_sense.weather.ui

data class WeatherListItem(
    val id: Long,
    val icon: Int,
    val title: String,
    val value: String,
    val onClick: () -> Unit = {}
)