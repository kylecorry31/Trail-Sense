package com.kylecorry.trail_sense.tools.weather.domain.sealevel

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.filters.LoessFilter2D
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.tools.weather.domain.RawWeatherObservation
import java.time.Duration
import kotlin.math.pow

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
        if (seaLevel.size < 3 || smoothing <= 0f) {
            return seaLevel
        }

        val factor = MAX_SMOOTHING_FACTOR.pow(2 * smoothing.coerceAtMost(1f) - 1)

        val start = seaLevel.first().time
        val hours = seaLevel.map { Duration.between(start, it.time).toMillis() / MILLIS_PER_HOUR }
        val hpa = seaLevel.map { it.value.hpa().value }

        val noise = Statistics.estimateNoise(hours, hpa)
        val span = LoessFilter2D.getSpanForNoise(
            hours,
            noise,
            TARGET_NOISE_HPA / factor,
            MIN_SPAN_HOURS * factor,
            MAX_SPAN_HOURS * factor,
            SPAN_STEP_HOURS * factor
        )

        val smoothed = LoessFilter2D(
            1f,
            1,
            minimumSpanSize = 3,
            maximumSpanDistance = span
        ).filter(hours.zip(hpa) { x, y -> Vector2(x, y) })

        return seaLevel.zip(smoothed) { reading, point ->
            reading.copy(value = Pressure.hpa(point.y).convertTo(reading.value.units))
        }
    }

    companion object {
        const val DEFAULT_SMOOTHING = 0.5f
        private const val MAX_SMOOTHING_FACTOR = 4f
        private const val TARGET_NOISE_HPA = 0.1f
        private const val MILLIS_PER_HOUR = 3_600_000f
        private const val MIN_SPAN_HOURS = 1f
        private const val MAX_SPAN_HOURS = 4f
        private const val SPAN_STEP_HOURS = 0.5f
    }
}
