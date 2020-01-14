package com.kylecorry.trail_sense.weather

import android.content.Context

interface IPressureHistoryRepository {
    fun getAll(context: Context): List<PressureReading>
    fun add(context: Context, reading: PressureReading): PressureReading
}