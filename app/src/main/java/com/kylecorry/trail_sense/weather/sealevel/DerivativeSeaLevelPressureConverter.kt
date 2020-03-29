package com.kylecorry.trail_sense.weather.sealevel

import com.kylecorry.trail_sense.shared.PressureAltitudeReading
import com.kylecorry.trail_sense.shared.PressureReading
import java.time.Duration
import kotlin.math.abs

class DerivativeSeaLevelPressureConverter(private val maximumNaturalChange: Float)  :
    ISeaLevelPressureConverter {
    override fun convert(readings: List<PressureAltitudeReading>): List<PressureReading> {

        if (readings.isEmpty()) return listOf()

        val output = mutableListOf<PressureReading>()

        var lastPressure = readings.first()
        var lastDp = 0f

        var pressure =
            SeaLevelPressureCalibrator.calibrate(
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
                dp = lastDp
            }

            pressure += dp * dt

            lastPressure = it
            lastDp = dp

            output.add(
                PressureReading(
                    it.time,
                    pressure
                )
            )
        }

        return output
    }

    companion object {
        private const val MILLIS_TO_HOURS = 1f / (1000f * 60f * 60f)
    }

}