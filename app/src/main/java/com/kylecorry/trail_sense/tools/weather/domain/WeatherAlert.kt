package com.kylecorry.trail_sense.tools.weather.domain

import com.kylecorry.trail_sense.shared.data.Identifiable

enum class WeatherAlert(override val id: Long) : Identifiable {
    Storm(1),
    Hot(2),
    Cold(3)
}