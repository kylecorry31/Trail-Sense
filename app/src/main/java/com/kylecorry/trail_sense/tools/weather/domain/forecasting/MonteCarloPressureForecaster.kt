package com.kylecorry.trail_sense.tools.weather.domain.forecasting

import com.kylecorry.sol.math.SolMath
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
    private fun randomValue(
        random: Random,
        center: Float,
        deviation: Float,
        minimum: Float,
        maximum: Float
    ): Float {
        return (random.nextGaussian().toFloat() * deviation + center).coerceIn(minimum, maximum)
    }

    fun getPressureForecast(
        history: List<Reading<Pressure>>,
        forecastLengthHours: Float = 6f,
        maxErrorHpa: Float = 5f,
        forecastStepSizeHours: Float = 1f,
        velocityError: Float = 0.2f,
        velocitySmoothing: Float = 0.15f,
        accelerationError: Float = 0.2f,
        accelerationSmoothing: Float = 0.15f
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
        var cachedFirstDerivative: List<Vector2>? = null
        var cachedSecondDerivative: List<Vector2>? = null
        val predictors = (0 until 50).map {
            val velocityOffset = randomValue(random, 0f, velocityError, -1f, 1f)
            val accelerationOffset = randomValue(random, 0f, accelerationError, -0.5f, 0.5f)
            DerivativePredictor(
                2, mapOf(
                    1 to DerivativePredictor.DerivativePredictorConfig {
                        if (cachedFirstDerivative == null) {
                            cachedFirstDerivative = if (SolMath.isZero(velocitySmoothing)) {
                                it
                            } else {
                                DataUtils.smooth(it, velocitySmoothing)
                            }
                        }
                        cachedFirstDerivative.map { it.copy(y = it.y + velocityOffset) }
                    },
                    2 to DerivativePredictor.DerivativePredictorConfig {
                        if (cachedSecondDerivative == null) {
                            cachedSecondDerivative = if (SolMath.isZero(accelerationSmoothing)) {
                                it
                            } else {
                                DataUtils.smooth(it, accelerationSmoothing)
                            }
                        }
                        cachedSecondDerivative.map { it.copy(y = it.y + accelerationOffset) }
                    }
                )
            )
        }

        val ensemble = TimeSeriesEnsemble(predictors)

        val n = (forecastLengthHours / forecastStepSizeHours).toInt()
        // TODO: If the pressure turns around, don't forecast further out than that (can't tell what the next pressure system will bring)
        // TODO: Return the CI or only include samples that are confident
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
}