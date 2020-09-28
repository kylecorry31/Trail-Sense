package com.kylecorry.trail_sense.weather.domain.sealevel

import com.kylecorry.trail_sense.weather.domain.AltitudeReading
import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import kotlin.math.abs

internal class GPSAltitudeCalculator3 :
    IAltitudeCalculator {
    override fun convert(readings: List<PressureAltitudeReading>): List<AltitudeReading> {

        if (readings.size <= 1) {
            return readings.map { AltitudeReading(it.time, it.altitude) }
        }

        val changeThreshold = 60
        val dwellThreshold = 3
        val groups = mutableListOf<MutableList<AltitudeReading>>()
        for (reading in readings) {
            if (groups.isEmpty()) {
                groups.add(mutableListOf(AltitudeReading(reading.time, reading.altitude)))
                continue
            }

            val lastGroup = groups.last()
            if (abs(lastGroup.last().value - reading.altitude) < changeThreshold) {
                lastGroup.add(AltitudeReading(reading.time, reading.altitude))
            } else {
                groups.add(mutableListOf(AltitudeReading(reading.time, reading.altitude)))
            }
        }

        for (group in groups) {
            if (group.size < dwellThreshold) {
                for (i in 0 until group.size) {
                    val old = group.removeAt(i)
                    group.add(i, old.copy(value = Float.NaN))
                }
            }
        }

        val newAltitudes = mutableListOf<AltitudeReading>()
        for (group in groups) {
            if (group.first().value.isNaN()) {
                newAltitudes.addAll(group)
                continue
            }
            val average = group.sumByDouble { it.value.toDouble() }.toFloat() / group.size
            for (altitude in group) {
                newAltitudes.add(AltitudeReading(altitude.time, average))
            }
        }

        for (i in 0 until newAltitudes.size) {
            if (newAltitudes[i].value.isNaN()) {
                val prev = prevValid(newAltitudes, i, readings.first().altitude)
                val next = nextValid(newAltitudes, i, readings.last().altitude)
                // TODO: Weighted average
                val avg = (next + prev) / 2
                val old = newAltitudes.removeAt(i)
                newAltitudes.add(i, old.copy(value = avg))
            }
        }



        return newAltitudes
    }

    private fun nextValid(readings: List<AltitudeReading>, start: Int, fallback: Float): Float {
        for (i in start until readings.size) {
            if (!readings[i].value.isNaN()) {
                return readings[i].value
            }
        }
        return fallback
    }

    private fun prevValid(readings: List<AltitudeReading>, start: Int, fallback: Float): Float {
        for (i in start downTo 0) {
            if (!readings[i].value.isNaN()) {
                return readings[i].value
            }
        }
        return fallback
    }
}