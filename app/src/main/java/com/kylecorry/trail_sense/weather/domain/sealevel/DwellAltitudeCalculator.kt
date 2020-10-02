package com.kylecorry.trail_sense.weather.domain.sealevel

import com.kylecorry.trail_sense.weather.domain.AltitudeReading
import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import java.time.Duration
import kotlin.math.abs

internal class DwellAltitudeCalculator(private val dwellThreshold: Duration, private val changeThreshold: Float) : IAltitudeCalculator {
    override fun convert(readings: List<PressureAltitudeReading>, interpolateAltitudeChanges: Boolean): List<AltitudeReading> {

        if (readings.size <= 1) {
            return readings.map { AltitudeReading(it.time, it.altitude) }
        }

        val groups = mutableListOf<MutableList<AltitudeReading>>()
        for (reading in readings) {
            if (groups.isEmpty()) {
                groups.add(mutableListOf(AltitudeReading(reading.time, reading.altitude)))
                continue
            }

            val lastGroup = groups.last()
            if (abs(lastGroup.first().value - reading.altitude) < changeThreshold) {
                lastGroup.add(AltitudeReading(reading.time, reading.altitude))
            } else {
                groups.add(mutableListOf(AltitudeReading(reading.time, reading.altitude)))
            }
        }

        for (group in groups) {
            val duration = Duration.between(group.first().time, group.last().time)
            if (duration < dwellThreshold) {
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

        if (interpolateAltitudeChanges) {
            val first = AltitudeReading(readings.first().time, readings.first().altitude)
            val last = AltitudeReading(readings.last().time, readings.last().altitude)
            for (i in 0 until newAltitudes.size) {
                if (newAltitudes[i].value.isNaN()) {
                    val prev = prevValid(newAltitudes, i, first)
                    val next = nextValid(newAltitudes, i, last)

                    val range = next.time.epochSecond - prev.time.epochSecond
                    val percentOfTime = if (range == 0L) {
                        0f
                    } else {
                        (newAltitudes[i].time.epochSecond - prev.time.epochSecond) / range.toFloat()
                    }

                    val altitudeChange = next.value - prev.value
                    val avg = prev.value + altitudeChange * percentOfTime
                    val old = newAltitudes.removeAt(i)
                    newAltitudes.add(i, old.copy(value = avg))
                }
            }
        }
        return newAltitudes
    }

    private fun nextValid(
        readings: List<AltitudeReading>,
        start: Int,
        fallback: AltitudeReading
    ): AltitudeReading {
        for (i in start until readings.size) {
            if (!readings[i].value.isNaN()) {
                return AltitudeReading(readings[i].time, readings[i].value)
            }
        }
        return fallback
    }

    private fun prevValid(
        readings: List<AltitudeReading>,
        start: Int,
        fallback: AltitudeReading
    ): AltitudeReading {
        for (i in start downTo 0) {
            if (!readings[i].value.isNaN()) {
                return AltitudeReading(readings[i].time, readings[i].value)
            }
        }
        return fallback
    }
}