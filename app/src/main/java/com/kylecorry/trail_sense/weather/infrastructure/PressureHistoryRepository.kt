package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import android.os.Build
import com.kylecorry.trail_sense.weather.domain.PressureAltitudeReading
import org.threeten.bp.Duration
import org.threeten.bp.Instant
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            readings.removeIf { Duration.between(it.time, Instant.now()) > keepDuration }
        } else {
            readings.removeAll(readings.filter { Duration.between(it.time, Instant.now()) > keepDuration })
        }
    }

    private fun loadFromFile(context: Context) {
        if (!context.getFileStreamPath(FILE_NAME).exists()) return
        val readings = context.openFileInput(FILE_NAME).bufferedReader().useLines { lines ->
            lines.map { it.split(",") }
                .map {
                    PressureAltitudeReading(
                        Instant.ofEpochMilli(it[0].toLong()),
                        it[1].toFloat(),
                        it[2].toFloat()
                    )
                }
                .sortedBy { it.time }
                .toMutableList()
        }
        loaded = true
        PressureHistoryRepository.readings = readings
        removeOldReadings()
        setChanged()
        notifyObservers()
    }

    private fun saveToFile(context: Context) {
        context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use {
            val output = readings.joinToString("\n") { reading ->
                "${reading.time.toEpochMilli()},${reading.pressure},${reading.altitude}"
            }.toByteArray()
            it.write(output)
        }
    }

}