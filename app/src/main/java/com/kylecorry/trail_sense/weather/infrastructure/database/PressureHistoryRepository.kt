package com.kylecorry.trail_sense.weather.infrastructure.database

import android.content.Context
import com.kylecorry.trail_sense.weather.domain.PressureAltitudeReading
import java.time.Duration
import java.time.Instant
import java.util.*

object PressureHistoryRepository : Observable(), IPressureHistoryRepository {

    private const val FILE_NAME = "pressure.csv"
    private val keepDuration: Duration = Duration.ofHours(48).plusMinutes(5)

    private var readings: MutableList<PressureAltitudeReading> = mutableListOf()

    private var loaded = false

    override fun getAll(context: Context): List<PressureAltitudeReading> {
        if (!loaded) {
            loadFromFile(context)
        }
        return readings
    }

    override fun add(context: Context, reading: PressureAltitudeReading) {
        if (!loaded) {
            loadFromFile(context)
        }
        readings.add(reading)
        removeOldReadings()
        saveToFile(context)
        setChanged()
        notifyObservers()
    }

    private fun removeOldReadings() {
        readings.removeIf { Duration.between(it.time, Instant.now()) > keepDuration }
    }

    private fun loadFromFile(context: Context) {
        if (!context.getFileStreamPath(FILE_NAME).exists()) return
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
            loaded = true
            PressureHistoryRepository.readings = readings
            removeOldReadings()
            setChanged()
            notifyObservers()
        }
    }

    private fun saveToFile(context: Context) {
        context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use {
            val output = readings.joinToString("\n") { reading ->
                "${reading.time.toEpochMilli()},${reading.pressure},${reading.altitude},${reading.temperature}"
            }.toByteArray()
            it.write(output)
        }
    }

}