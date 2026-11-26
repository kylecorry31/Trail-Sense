package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.sol.math.algebra.Matrix
import com.kylecorry.sol.math.algebra.add
import com.kylecorry.sol.math.algebra.dot
import com.kylecorry.sol.math.algebra.subtract
import com.kylecorry.sol.math.algebra.transpose
import kotlin.math.sqrt

internal class KalmanFilter(
    private val stateSize: Int,
    measurementSize: Int,
    controlSize: Int,
    private val updateStateWithPrediction: Boolean = false
) {
    private val identity = Matrix.identity(stateSize)
    var F = Matrix.zeros(stateSize, stateSize)

    var H = Matrix.zeros(measurementSize, stateSize)

    var B = Matrix.zeros(stateSize, controlSize)

    var Q = Matrix.zeros(stateSize, stateSize)

    var R = Matrix.zeros(measurementSize, measurementSize)

    var Uk = Matrix.zeros(controlSize, 1)

    var Zk = Matrix.zeros(measurementSize, 1)

    var Xk_km1 = Matrix.zeros(stateSize, 1)

    var Pk_km1 = Matrix.zeros(stateSize, stateSize)

    var Yk = Matrix.zeros(measurementSize, 1)

    var Sk = Matrix.zeros(measurementSize, measurementSize)

    var K = Matrix.zeros(stateSize, measurementSize)

    var Xk_k = Matrix.zeros(stateSize, 1)

    var Pk_k = Matrix.zeros(stateSize, stateSize)

    fun predict() {
        Xk_km1 = F.dot(Xk_k).add(B.dot(Uk))

        Pk_km1 = F.dot(Pk_k).dot(F.transpose()).add(Q)

        if (updateStateWithPrediction) {
            Xk_k = Xk_km1.clone()
            Pk_k = Pk_km1.clone()
        }
    }

    fun update(): Boolean {
        Yk = Zk.subtract(H.dot(Xk_km1))

        val pk_km1DotHkt = Pk_km1.dot(H.transpose())

        Sk = R.add(H.dot(pk_km1DotHkt))

        K = solveGain(pk_km1DotHkt) ?: return false

        Xk_k = Xk_km1.add(K.dot(Yk))

        // WHY: Joseph form keeps the covariance positive semi-definite despite rounding error
        val correction = identity.subtract(K.dot(H))
        Pk_k = correction.dot(Pk_km1).dot(correction.transpose())
            .add(K.dot(R).dot(K.transpose()))
        // WHY: Floating point error can make the covariance slightly asymmetric
        for (row in 0 until stateSize) {
            for (column in 0 until row) {
                val average = (Pk_k[row, column].toDouble() + Pk_k[column, row]) / 2
                Pk_k[row, column] = average.toFloat()
                Pk_k[column, row] = average.toFloat()
            }
        }

        return true
    }

    // WHY: A Cholesky solve in Double precision is more stable than inverting Sk
    private fun solveGain(crossCovariance: Matrix): Matrix? {
        val size = Sk.rows()
        val lower = Array(size) { DoubleArray(size) }
        for (row in 0 until size) {
            for (column in 0..row) {
                var value = (Sk[row, column].toDouble() + Sk[column, row]) / 2
                for (k in 0 until column) {
                    value -= lower[row][k] * lower[column][k]
                }
                if (!value.isFinite() || (row == column && value <= 0)) return null
                lower[row][column] = if (row == column) sqrt(value) else value / lower[column][column]
            }
        }
        val gain = Matrix.zeros(stateSize, size)
        for (state in 0 until stateSize) {
            val solution = DoubleArray(size)
            for (row in 0 until size) {
                var value = crossCovariance[state, row].toDouble()
                for (column in 0 until row) value -= lower[row][column] * solution[column]
                solution[row] = value / lower[row][row]
            }
            for (row in size - 1 downTo 0) {
                var value = solution[row]
                for (column in row + 1 until size) value -= lower[column][row] * solution[column]
                solution[row] = value / lower[row][row]
                gain[state, row] = solution[row].toFloat()
                if (!gain[state, row].isFinite()) return null
            }
        }
        return gain
    }
}
