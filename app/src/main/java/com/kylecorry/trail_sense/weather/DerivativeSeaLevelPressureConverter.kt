package com.kylecorry.trail_sense.weather

import com.kylecorry.trail_sense.models.PressureAltitudeReading
import com.kylecorry.trail_sense.models.PressureReading
import java.time.Duration
import kotlin.math.abs

class DerivativeSeaLevelPressureConverter(private val maximumNaturalChange: Float)  : ISeaLevelPressureConverter {
    override fun convert(readings: List<PressureAltitudeReading>): List<PressureReading> {

        val output = mutableListOf<PressureReading>()

        var lastPressure = readings.first()

        var pressure = SeaLevelPressureCalibrator.calibrate(
                lastPressure.pressure,
                lastPressure.altitude
            )

        readings.forEach {
            var dp = 0.0f
            val dt = Duration.between(lastPressure.time, it.time).toMillis() * MILLIS_TO_HOURS

            if (lastPressure != it) {
                dp = (it.pressure - lastPressure.pressure) / dt
            }

            if (abs(dp) > maximumNaturalChange) {
                // Caused by change in location, don't accumulate pressure
                dp = 0f
            }

            pressure += dp * dt
            lastPressure = it

            output.add(PressureReading(it.time, pressure))
        }

        return output
    }

    companion object {
        private const val MILLIS_TO_HOURS = 1f / (1000f * 60f * 60f)
    }

}