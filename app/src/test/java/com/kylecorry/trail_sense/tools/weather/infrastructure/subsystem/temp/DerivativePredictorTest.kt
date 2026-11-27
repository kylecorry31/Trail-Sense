package com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.temp

import com.kylecorry.sol.math.Vector2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DerivativePredictorTest {

    private val accelerating = (0..10).map { Vector2(it.toFloat(), it * it.toFloat()) }

    @Test
    fun decayingTheHighestDerivativeBendsTheForecastLess() {
        val constant = DerivativePredictor(2).predictNext(accelerating, 6, 1f)
        val decaying = DerivativePredictor(2, highestDerivativeDecayTime = 1f)
            .predictNext(accelerating, 6, 1f)

        constant.zip(decaying).drop(1).forEach { (c, d) ->
            assertTrue(d.y < c.y, "Expected decay to bend less: constant=${c.y}, decaying=${d.y}")
        }
    }

    @Test
    fun aDecayedHighestDerivativeLeavesALinearTrend() {
        val prediction = DerivativePredictor(2, highestDerivativeDecayTime = 0.5f)
            .predictNext(accelerating, 12, 1f)

        val lateSteps = prediction.takeLast(4).zipWithNext { a, b -> b.y - a.y }
        lateSteps.zipWithNext { a, b -> assertEquals(a, b, 0.05f) }
    }
}
