package com.kylecorry.trail_sense.weather.infrastructure.database

import android.content.Context
import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading

interface IPressureHistoryRepository {
    fun getAll(context: Context): List<PressureAltitudeReading>
    fun add(context: Context, reading: PressureAltitudeReading)
}