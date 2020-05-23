package com.kylecorry.trail_sense.weather.domain.sealevel

import android.hardware.SensorManager
import com.kylecorry.trail_sense.weather.domain.AltitudeReading
import com.kylecorry.trail_sense.weather.domain.PressureAltitudeReading
import org.threeten.bp.Duration
import kotlin.math.abs

internal class BarometerGPSAltitudeCalculator(private val maxNaturalPressureChange: Float = 5f) :
    IAltitudeCalculator {
    override fun convert(readings: List<PressureAltitudeReading>): List<AltitudeReading> {

        if (readings.isEmpty()) return listOf()

        var lastReading = readings.first()

        var altitude = lastReading.altitude

        return readings.map {
            if (it != lastReading) {
                val dt = Duration.between(lastReading.time, it.time).toMillis() * MILLIS_TO_HOURS
                val dp = (it.pressure - lastReading.pressure) / dt

                if (abs(dp) > maxNaturalPressureChange) {
                    // Barometer will show change
                    val barometerAltitude = altitude + getAltitudeChange(lastReading.pressure, it.pressure)
                    altitude = barometerAltitude

                    altitude = if (abs(it.altitude - barometerAltitude) < MAX_ALTITUDE_DIFF) {
                        it.altitude
                    } else {
                        barometerAltitude
                    }

                }
            }
            lastReading = it
            AltitudeReading(
                it.time,
                altitude
            )
        }
    }

    private fun getAltitudeChange(lastPressure: Float, currentPressure: Float): Float {
        val lastBarometricAltitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, lastPressure)
        val currentBarometricAltitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, currentPressure)
        return currentBarometricAltitude - lastBarometricAltitude
    }

    companion object {
        private const val MILLIS_TO_HOURS = 1f / (1000f * 60f * 60f)
        private const val MAX_ALTITUDE_DIFF = 10f
    }
}