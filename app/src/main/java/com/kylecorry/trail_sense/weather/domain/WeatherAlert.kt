package com.kylecorry.trail_sense.weather.domain

import com.kylecorry.trail_sense.shared.database.Identifiable

enum class WeatherAlert(override val id: Long) : Identifiable {
    Storm(1),
    Hot(2),
    Cold(3)
}