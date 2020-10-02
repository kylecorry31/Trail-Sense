package com.kylecorry.trail_sense.weather.infrastructure.database

import android.content.Context
import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import java.time.Duration
import java.time.Instant
import java.util.*

internal object PressureHistoryRepository {

    private const val FILE_NAME = "pressure.csv"
    private val keepDuration: Duration = Duration.ofHours(48).plusMinutes(5)

    fun clear(context: Context) {
        if (!context.getFileStreamPath(FILE_NAME).exists()) return
        context.deleteFile(FILE_NAME)
    }

    fun get(context: Context): List<PressureAltitudeReading> {
        if (!context.getFileStreamPath(FILE_NAME).exists()) return listOf()
        context.openFileInput(FILE_NAME).use { file ->
            val readings = file.bufferedReader().useLines { lines ->
                lines.map { it.split(",") }
                    .map {
                        PressureAltitudeReading(
                            Instant.ofEpochMilli(it[0].toLong()),
                            it[1].toFloat(),
                            it[2].toFloat(),
                            if (it.size > 3) it[3].toFloat() else 16f
                        )
                    }
                    .sortedBy { it.time }
                    .toMutableList()
            }
            file.close()
            return readings.filter { Duration.between(it.time, Instant.now()) <= keepDuration }
        }
    }

}