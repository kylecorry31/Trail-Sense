package com.kylecorry.trail_sense.weather.domain.sealevel

import com.kylecorry.trail_sense.weather.domain.AltitudeReading
import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import kotlin.math.abs

internal class GPSAltitudeCalculator2 :
    IAltitudeCalculator {
    override fun convert(readings: List<PressureAltitudeReading>): List<AltitudeReading> {
        val changeThreshold = 60
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

        val newAltitudes = mutableListOf<AltitudeReading>()
        for (group in groups) {
            val average = group.sumByDouble { it.value.toDouble() }.toFloat() / group.size
            for (altitude in group) {
                newAltitudes.add(AltitudeReading(altitude.time, average))
            }
        }

        return newAltitudes
    }
}