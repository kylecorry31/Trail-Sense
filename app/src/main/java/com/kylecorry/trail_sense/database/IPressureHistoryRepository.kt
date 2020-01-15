package com.kylecorry.trail_sense.database

import android.content.Context
import com.kylecorry.trail_sense.models.PressureAltitudeReading

interface IPressureHistoryRepository {
    fun getAll(context: Context): List<PressureAltitudeReading>
    fun add(context: Context, reading: PressureAltitudeReading): PressureAltitudeReading
}