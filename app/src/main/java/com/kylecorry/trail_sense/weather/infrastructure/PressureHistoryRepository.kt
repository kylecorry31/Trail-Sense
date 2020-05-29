package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import android.os.Build
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
                            it[2].toFloat()
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
                "${reading.time.toEpochMilli()},${reading.pressure},${reading.altitude}"
            }.toByteArray()
            it.write(output)
        }
    }

    private fun getMockData(): List<PressureAltitudeReading> {
        return ("48,1021\n" +
                "47,1020\n" +
                "46,1019.3\n" +
                "45,1019.6\n" +
                "44,1020\n" +
                "43,1019.6\n" +
                "42,1019.1\n" +
                "41,1018.8\n" +
                "40,1018.4\n" +
                "39,1018\n" +
                "38,1017.2\n" +
                "37,1017\n" +
                "36,1016.5\n" +
                "35,1015.9\n" +
                "34,1015.4\n" +
                "33,1013.2\n" +
                "32,1013.5\n" +
                "31,1012.9\n" +
                "30,1012.2\n" +
                "29,1011.8\n" +
                "28,1011\n" +
                "27,1010.6\n" +
                "26,1010\n" +
                "25,1010\n" +
                "24,1009.5\n" +
                "23,1009.2\n" +
                "22,1008.3\n" +
                "21,1008.6\n" +
                "20,1009.1\n" +
                "19,1009.4\n" +
                "18,1008.8\n" +
                "17,1008.9\n" +
                "16,1009.2\n" +
                "15,1009.7\n" +
                "14,1009.5\n" +
                "13,1009.2\n" +
                "12,1009\n" +
                "11,1008.4\n" +
                "10,1008\n" +
                "9,1008.1\n" +
                "8,1007.9\n" +
                "7,1008.5\n" +
                "6,1009\n" +
                "5,1009.3\n" +
                "4,1009\n" +
                "3,1008.3\n" +
                "2,1008.2\n" +
                "1,1008\n" +
                "0,1007.6").split("\n").map { it.split(",") }
            .map {
                PressureAltitudeReading(Instant.now().minusSeconds(it[0].toLong() * 60 * 60), it[1].toFloat(), 0f) }
    }

}