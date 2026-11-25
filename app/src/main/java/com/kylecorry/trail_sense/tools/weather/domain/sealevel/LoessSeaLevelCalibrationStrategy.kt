package com.kylecorry.trail_sense.tools.weather.domain.sealevel

import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.data.DataUtils
import com.kylecorry.trail_sense.tools.weather.domain.RawWeatherObservation
import java.time.Duration

class LoessSeaLevelCalibrationStrategy(
    private val baseStrategy: ISeaLevelCalibrationStrategy,
    private val smoothing: Float = DEFAULT_SMOOTHING
) : ISeaLevelCalibrationStrategy {
    override fun calibrate(readings: List<Reading<RawWeatherObservation>>): List<Reading<Pressure>> {
        val seaLevel = baseStrategy.calibrate(readings)
        val start = seaLevel.firstOrNull()?.time ?: return seaLevel
        return DataUtils.smoothAutomatically(
            seaLevel,
            HOURLY_SMOOTHING_OPTIONS,
            smoothing,
            { _, reading -> Duration.between(start, reading.time).toMillis() / MILLIS_PER_HOUR },
            { it.value.hpa().value }
        ) { reading, smoothed ->
            reading.copy(value = Pressure.hpa(smoothed).convertTo(reading.value.units))
        }
    }

    companion object {
        const val DEFAULT_SMOOTHING = 0.5f
        private const val MILLIS_PER_HOUR = 3_600_000f

        private val HOURLY_SMOOTHING_OPTIONS = DataUtils.AutomaticSmoothingOptions(
            targetNoise = 0.1f,
            minimumSpan = 1f,
            maximumSpan = 4f,
            spanStep = 0.5f
        )
    }
}
