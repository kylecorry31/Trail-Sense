package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.sol.math.algebra.Matrix
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class KalmanFilterTest {
    @ParameterizedTest
    @ValueSource(floats = [1e-20f, 1f, 1e20f])
    fun gainIsIndependentOfCovarianceScale(scale: Float) {
        val filter = KalmanFilter(4, 4, 0).apply {
            F = Matrix.identity(4)
            H = Matrix.identity(4)
            Pk_k = Matrix.diagonal(scale, scale, scale, scale)
            R = Matrix.diagonal(scale, scale, scale, scale)
            Zk = Matrix.column(2f, 4f, 6f, 8f)
        }
        filter.predict()
        assertTrue(filter.update())
        for (i in 0 until 4) {
            assertEquals(i + 1f, filter.Xk_k[i, 0], 1e-5f)
            assertEquals(0.5f, filter.Pk_k[i, i] / scale, 1e-5f)
        }
    }

    @Test
    fun solvesCorrelatedMeasurements() {
        val filter = KalmanFilter(2, 2, 0).apply {
            F = Matrix.identity(2)
            H = Matrix.identity(2)
            Pk_k = Matrix.create(arrayOf(floatArrayOf(4f, 2f), floatArrayOf(2f, 3f)))
            R = Matrix.identity(2)
            Zk = Matrix.column(4f, 8f)
        }
        filter.predict()
        assertTrue(filter.update())
        assertMatrixEquals(Matrix.column(4f, 6f), filter.Xk_k)
        val expected = Matrix.create(arrayOf(floatArrayOf(0.75f, 0.125f), floatArrayOf(0.125f, 0.6875f)))
        assertMatrixEquals(expected, filter.K)
        assertMatrixEquals(expected, filter.Pk_k)
    }

    @Test
    fun supportsChangingObservationSize() {
        val filter = KalmanFilter(3, 1, 0).apply { F = Matrix.identity(3) }
        for (size in listOf(1, 2, 3, 2, 1, 3)) {
            filter.Xk_k = Matrix.column(0f, 0f, 0f)
            filter.Pk_k = Matrix.diagonal(2f, 4f, 6f)
            filter.H = Matrix.create(size, 3) { row, column -> if (row == column) 1f else 0f }
            filter.R = Matrix.create(size, size) { row, column -> if (row == column) 2f * (row + 1) else 0f }
            filter.Zk = Matrix.create(size, 1) { row, _ -> 2f * (row + 1) }
            filter.predict()
            assertTrue(filter.update(), "measurement size: $size")
            for (state in 0 until 3) {
                assertEquals(if (state < size) state + 1f else 0f, filter.Xk_k[state, 0], 1e-5f)
                assertEquals((state + 1f) * if (state < size) 1f else 2f, filter.Pk_k[state, state], 1e-5f)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(floats = [0f, -1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY])
    fun failedFactorizationPreservesPosteriorAndAllowsRecovery(invalidVariance: Float) {
        val filter = KalmanFilter(2, 2, 0).apply {
            F = Matrix.identity(2)
            H = Matrix.identity(2)
            Xk_k = Matrix.column(3f, 4f)
            Pk_k = Matrix.identity(2)
            R = Matrix.identity(2)
            Zk = Matrix.column(5f, 8f)
        }
        // Complete a successful update first, then fail on the second pivot.
        filter.predict()
        assertTrue(filter.update())
        val state = filter.Xk_k.clone()
        val covariance = filter.Pk_k.clone()
        filter.predict()
        filter.R = Matrix.diagonal(1f, invalidVariance - filter.Pk_km1[1, 1])
        assertFalse(filter.update())
        assertMatrixEquals(state, filter.Xk_k)
        assertMatrixEquals(covariance, filter.Pk_k)

        filter.R = Matrix.identity(2)
        assertTrue(filter.update())
        assertMatrixEquals(Matrix.column(13f / 3f, 20f / 3f), filter.Xk_k)
        assertMatrixEquals(Matrix.diagonal(1f / 3f, 1f / 3f), filter.Pk_k)
    }

    @Test
    fun predictionIncludesControlAndPropagatesCovariance() {
        val filter = KalmanFilter(2, 1, 1).apply {
            F = Matrix.create(arrayOf(floatArrayOf(1f, 2f), floatArrayOf(0f, 1f)))
            B = Matrix.column(2f, 2f)
            Uk = Matrix.column(3f)
            Xk_k = Matrix.column(10f, 4f)
            Pk_k = Matrix.diagonal(1f, 2f)
            Q = Matrix.diagonal(3f, 4f)
        }
        filter.predict()
        assertMatrixEquals(Matrix.column(24f, 10f), filter.Xk_km1)
        assertMatrixEquals(Matrix.create(arrayOf(floatArrayOf(12f, 4f), floatArrayOf(4f, 6f))), filter.Pk_km1)
        assertMatrixEquals(Matrix.column(10f, 4f), filter.Xk_k)
        assertMatrixEquals(Matrix.diagonal(1f, 2f), filter.Pk_k)
    }

    @Test
    fun correlatedPositionObservationUpdatesVelocity() {
        val filter = KalmanFilter(2, 1, 0).apply {
            F = Matrix.identity(2)
            H = Matrix.row(1f, 0f)
            Pk_k = Matrix.create(arrayOf(floatArrayOf(4f, 2f), floatArrayOf(2f, 3f)))
            R = Matrix.diagonal(1f)
            Zk = Matrix.column(5f)
        }
        filter.predict()
        assertTrue(filter.update())
        assertEquals(4f, filter.Xk_k[0, 0], 1e-5f)
        assertEquals(2f, filter.Xk_k[1, 0], 1e-5f)
        assertEquals(0.8f, filter.Pk_k[0, 0], 1e-5f)
        assertEquals(0.4f, filter.Pk_k[0, 1], 1e-5f)
        assertEquals(filter.Pk_k[0, 1], filter.Pk_k[1, 0])
        assertEquals(2.2f, filter.Pk_k[1, 1], 1e-5f)
    }

    @Test
    fun indefiniteInnovationWithPositiveDiagonalIsRejected() {
        val filter = KalmanFilter(2, 2, 0).apply {
            F = Matrix.identity(2)
            H = Matrix.identity(2)
            Xk_k = Matrix.column(3f, 4f)
            R = Matrix.create(arrayOf(floatArrayOf(1f, 2f), floatArrayOf(2f, 1f)))
        }
        filter.predict()
        assertFalse(filter.update())
        assertEquals(3f, filter.Xk_k[0, 0])
        assertEquals(4f, filter.Xk_k[1, 0])
        assertMatrixEquals(Matrix.zeros(2, 2), filter.Pk_k)
    }

    @Test
    fun repeatedUpdatesConvergeToKnownMeanAndVariance() {
        val filter = KalmanFilter(1, 1, 0).apply {
            F = Matrix.identity(1)
            H = Matrix.identity(1)
            Pk_k = Matrix.diagonal(2f)
            R = Matrix.diagonal(2f)
            Zk = Matrix.column(4f)
        }
        for (count in 1..100) {
            filter.predict()
            assertTrue(filter.update())
            // Equal-variance observations and the zero prior have equal weight.
            assertEquals(4f * count / (count + 1), filter.Xk_k[0, 0], 1e-5f)
            assertEquals(2f / (count + 1), filter.Pk_k[0, 0], 1e-5f)
        }
    }

    @Test
    fun committedPredictionsAccumulateWithoutMeasurements() {
        val filter = KalmanFilter(1, 1, 1, updateStateWithPrediction = true).apply {
            F = Matrix.identity(1)
            B = Matrix.identity(1)
            Uk = Matrix.column(2f)
            Xk_k = Matrix.column(3f)
            Pk_k = Matrix.diagonal(4f)
            Q = Matrix.diagonal(5f)
        }
        repeat(2) { filter.predict() }
        assertMatrixEquals(Matrix.column(7f), filter.Xk_k)
        assertMatrixEquals(Matrix.diagonal(14f), filter.Pk_k)
        // Predictions and committed estimates must not alias mutable storage.
        filter.Xk_km1[0, 0] = 0f
        filter.Pk_km1[0, 0] = 0f
        assertEquals(7f, filter.Xk_k[0, 0])
        assertEquals(14f, filter.Pk_k[0, 0])
    }

    private fun assertMatrixEquals(expected: Matrix, actual: Matrix) {
        assertEquals(expected.rows(), actual.rows())
        assertEquals(expected.columns(), actual.columns())
        for (row in 0 until expected.rows()) {
            for (column in 0 until expected.columns()) {
                assertEquals(expected[row, column], actual[row, column], 1e-5f, "[$row, $column]")
            }
        }
    }
}
