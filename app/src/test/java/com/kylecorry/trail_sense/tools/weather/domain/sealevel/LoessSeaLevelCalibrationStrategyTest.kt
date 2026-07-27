package com.kylecorry.trail_sense.tools.weather.domain.sealevel

import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.tools.weather.domain.RawWeatherObservation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

internal class LoessSeaLevelCalibrationStrategyTest {

    @Test
    fun calibrateWithConsecutiveNoisyPressureReadings() {
        val start = Instant.parse("2026-01-01T00:00:00Z")
        val readings = List(30) { index ->
            val pressure = if (index in 10..19) {
                if (index % 2 == 0) 900f else 1100f
            } else {
                1000f + (index % 3 - 1) * 0.1f
            }
            Reading(
                RawWeatherObservation(
                    id = index.toLong(),
                    pressure = pressure,
                    altitude = 0f,
                    temperature = 20f
                ),
                start.plusSeconds(index * 60L)
            )
        }
        val strategy = LoessSeaLevelCalibrationStrategy(
            NullSeaLevelCalibrationStrategy(),
            smoothing = 0.15f
        )

        val calibrated = strategy.calibrate(readings)

        assertEquals(readings.size, calibrated.size)
        assertEquals(readings.map { it.time }, calibrated.map { it.time })
        assertTrue(calibrated.all { it.value.value.isFinite() })
    }
}
