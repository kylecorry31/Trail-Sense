package com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.temp

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.Vector
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.calculus.Calculus
import com.kylecorry.sol.math.calculus.RungeKutta4thOrderSolver

class DerivativePredictor(
    private val order: Int = 2,
    private val configs: Map<Int, DerivativePredictorConfig> = emptyMap()
) : ITimeSeriesPredictor {

    private val solver = RungeKutta4thOrderSolver()

    private fun withMapping(order: Int, values: List<Vector2>): List<Vector2> {
        val mapFn = configs[order]?.map ?: return values
        return mapFn(values)
    }

    private fun calculateLastStep(samples: List<Vector2>): Float? {
        if (samples.size < 2) {
            return null
        }
        return samples[samples.size - 1].x - samples[samples.size - 2].x
    }

    override fun predictNext(samples: List<Vector2>, n: Int, step: Float?): List<Vector2> {
        val mappedSamples = withMapping(0, samples.sortedBy { it.x }).toMutableList()
        val actualStep = step ?: calculateLastStep(mappedSamples) ?: 1f
        val values = mutableListOf(mappedSamples)
        for (i in 1..order) {
            val lastValues = values.last()
            values.add(withMapping(i, Calculus.derivative(lastValues)).toMutableList())
        }

        val lastX = mappedSamples.last().x
        val y0 = Vector(values.map { it.last().y }.toFloatArray())

        val odeSystem: (Float, Vector) -> Vector = { _, y ->
            Vector(FloatArray(order + 1) { i ->
                if (i < order) {
                    y[i + 1]
                } else {
                    // Highest order derivative is constant
                    0f
                }
            })
        }

        val solution = solver.solve(
            Range(lastX, lastX + n * actualStep),
            actualStep,
            y0,
            odeSystem
        )

        return solution.mapIndexed { index, state ->
            Vector2(state.first, state.second[0])
        }
    }

    class DerivativePredictorConfig(
        val map: ((List<Vector2>) -> List<Vector2>)? = null,
    )
}