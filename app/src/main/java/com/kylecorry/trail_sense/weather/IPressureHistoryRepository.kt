package com.kylecorry.trail_sense.weather

import android.content.Context
import com.kylecorry.trail_sense.shared.PressureAltitudeReading

interface IPressureHistoryRepository {
    fun getAll(context: Context): List<PressureAltitudeReading>
    fun add(context: Context, reading: PressureAltitudeReading): PressureAltitudeReading
}