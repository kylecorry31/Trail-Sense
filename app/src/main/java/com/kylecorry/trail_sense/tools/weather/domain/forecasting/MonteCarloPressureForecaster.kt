package com.kylecorry.trail_sense.tools.weather.domain.forecasting

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.random.nextGaussian
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.data.DataUtils
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.temp.DerivativePredictor
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.temp.TimeSeriesEnsemble
import java.time.Instant
import kotlin.random.Random

class MonteCarloPressureForecaster {

    fun getPressureForecast(
        history: List<Reading<Pressure>>,
        forecastLengthHours: Float = 6f,
        maxErrorHpa: Float = 5f,
        forecastStepSizeHours: Float = 1f,
        velocityError: Float = 0.2f,
        accelerationError: Float = 0.2f
    ): List<Reading<Pressure>> {
        val start = history.firstOrNull()?.time?.toEpochMilli() ?: return emptyList()
        val pressures = history.map {
            val hours = (it.time.toEpochMilli() - start) / 3600000f
            Vector2(hours, it.value.hpa().value)
        }
        if (pressures.size <= 2) {
            return emptyList()
        }

        val random = Random(1)
        var smoothedVelocity: List<Vector2>? = null
        var smoothedAcceleration: List<Vector2>? = null
        val predictors = (0 until 50).map {
            val velocityOffset =
                (random.nextGaussian().toFloat() * velocityError).coerceIn(-1f, 1f)
            val accelerationOffset =
                (random.nextGaussian().toFloat() * accelerationError).coerceIn(-0.5f, 0.5f)
            DerivativePredictor(
                2,
                mapOf(
                    1 to DerivativePredictor.DerivativePredictorConfig { velocity ->
                        val smoothed = smoothedVelocity ?: smooth(velocity, VELOCITY_SMOOTHING_OPTIONS).also {
                            smoothedVelocity = it
                        }
                        smoothed.map { it.copy(y = it.y + velocityOffset) }
                    },
                    2 to DerivativePredictor.DerivativePredictorConfig { acceleration ->
                        val smoothed = smoothedAcceleration
                            ?: smooth(acceleration, ACCELERATION_SMOOTHING_OPTIONS).also {
                                smoothedAcceleration = it
                            }
                        smoothed.map { it.copy(y = it.y + accelerationOffset) }
                    }
                ),
                ACCELERATION_DECAY_HOURS
            )
        }

        val ensemble = TimeSeriesEnsemble(predictors)

        val n = (forecastLengthHours / forecastStepSizeHours).toInt()
        return ensemble.predictNext(pressures, n, forecastStepSizeHours).filter {
            val confidence = it.upper.y - it.lower.y
            confidence < maxErrorHpa
        }.map {
            Reading(
                Pressure.hpa(it.value.y),
                Instant.ofEpochMilli(start + (it.value.x * 3600000f).toLong())
            )
        }
    }

    private fun smooth(
        values: List<Vector2>,
        options: DataUtils.AutomaticSmoothingOptions
    ): List<Vector2> {
        return DataUtils.smoothAutomatically(
            values,
            options,
            x = { _, value -> value.x },
            y = { it.y }
        ) { value, smoothed -> value.copy(y = smoothed) }
    }

    companion object {
        private val VELOCITY_SMOOTHING_OPTIONS = DataUtils.AutomaticSmoothingOptions(
            targetNoise = 0.05f,
            minimumSpan = 1f,
            maximumSpan = 6f,
            spanStep = 0.5f
        )

        private val ACCELERATION_SMOOTHING_OPTIONS = DataUtils.AutomaticSmoothingOptions(
            targetNoise = 0.02f,
            minimumSpan = 2f,
            maximumSpan = 8f,
            spanStep = 0.5f
        )

        private const val ACCELERATION_DECAY_HOURS = 1f
    }
}
