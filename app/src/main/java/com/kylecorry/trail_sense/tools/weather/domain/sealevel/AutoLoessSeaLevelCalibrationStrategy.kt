package com.kylecorry.trail_sense.tools.weather.domain.sealevel

import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.data.DataUtils
import com.kylecorry.trail_sense.tools.weather.domain.RawWeatherObservation
import java.time.Duration

/**
 * Smooths the pressure with the narrowest LOESS span which reduces the estimated noise to a target.
 * A clean history is barely smoothed, a noisy one gets a wide span.
 * @param smoothing from 0 (none) to 1, where [DEFAULT_SMOOTHING] is the automatic amount and each end of
 * the range is 4 times more or less smoothing
 */
class AutoLoessSeaLevelCalibrationStrategy(
    private val baseStrategy: ISeaLevelCalibrationStrategy,
    private val smoothing: Float = DEFAULT_SMOOTHING
) : ISeaLevelCalibrationStrategy {
    override fun calibrate(readings: List<Reading<RawWeatherObservation>>): List<Reading<Pressure>> {
        val seaLevel = baseStrategy.calibrate(readings)
        return DataUtils.smoothAutomatically(
            seaLevel,
            OPTIONS,
            smoothing,
            { _, reading -> Duration.between(seaLevel.first().time, reading.time).toMillis() / MILLIS_PER_HOUR },
            { it.value.hpa().value }
        ) { reading, smoothed ->
            reading.copy(value = Pressure.hpa(smoothed).convertTo(reading.value.units))
        }
    }

    companion object {
        const val DEFAULT_SMOOTHING = 0.5f
        private const val MILLIS_PER_HOUR = 3_600_000f
        private val OPTIONS = DataUtils.AutomaticSmoothingOptions(
            targetNoise = 0.1f,
            minimumSpan = 1f,
            maximumSpan = 4f,
            spanStep = 0.5f
        )
    }
}
