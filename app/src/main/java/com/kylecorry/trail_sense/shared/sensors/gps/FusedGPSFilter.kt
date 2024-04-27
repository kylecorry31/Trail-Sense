package com.kylecorry.trail_sense.shared.sensors.gps

import android.os.SystemClock
import com.kylecorry.sol.math.algebra.columnMatrix
import com.kylecorry.sol.math.algebra.createMatrix
import com.kylecorry.sol.math.algebra.identityMatrix
import com.kylecorry.sol.math.algebra.multiply

/*
 * MIT License
 *
 * Copyright (c) 2020 Mad Devs
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

internal class FusedGPSFilter(
    private val useGpsSpeed: Boolean,
    initialX: Float,
    initialY: Float,
    initialXVelocity: Float,
    initialYVelocity: Float,
    private val accelerationDeviation: Float,
    initialPositionDeviation: Float,
    updateStateWithPrediction: Boolean = false
) {
    private var lastPredictTime = SystemClock.elapsedRealtimeNanos()
    private var lastUpdateTime = lastPredictTime
    private val kalmanFilter = KalmanFilter(
        4,
        if (useGpsSpeed) 4 else 2,
        2,
        updateStateWithPrediction = updateStateWithPrediction
    )

    val currentX: Float
        get() = kalmanFilter.Xk_k[0][0]
    val currentY: Float
        get() = kalmanFilter.Xk_k[1][0]
    val currentXVelocity: Float
        get() = kalmanFilter.Xk_k[2][0]
    val currentYVelocity: Float
        get() = kalmanFilter.Xk_k[3][0]
    val currentPositionError: Float
        get() = kalmanFilter.Pk_k[0][0]
    val currentVelocityError: Float
        get() = kalmanFilter.Pk_k[2][2]

    val predictedX: Float
        get() = kalmanFilter.Xk_km1[0][0]
    val predictedY: Float
        get() = kalmanFilter.Xk_km1[1][0]
    val predictedXVelocity: Float
        get() = kalmanFilter.Xk_km1[2][0]
    val predictedYVelocity: Float
        get() = kalmanFilter.Xk_km1[3][0]
    val predictedPositionError: Float
        get() = kalmanFilter.Pk_km1[0][0]
    val predictedVelocityError: Float
        get() = kalmanFilter.Pk_km1[2][2]

    private val lock = Any()

    init {
        kalmanFilter.Xk_k = columnMatrix(initialX, initialY, initialXVelocity, initialYVelocity)
        // Both state and measurement have 4 dimensions, so use an identity matrix
        kalmanFilter.H =
            createMatrix(if (useGpsSpeed) 4 else 2, 4) { i, j -> if (i == j) 1f else 0f }
        kalmanFilter.Pk_k = identityMatrix(4).multiply(initialPositionDeviation)
    }

    fun predict(
        xAcceleration: Float,
        yAcceleration: Float
    ) = synchronized(lock) {
        val time = SystemClock.elapsedRealtimeNanos()
        val dtPredict = (time - lastPredictTime) / 1_000_000_000f
        val dtUpdate = (time - lastUpdateTime) / 1_000_000_000f
        rebuildFMatrix(dtPredict)
        rebuildBMatrix(dtPredict)
        rebuildUMatrix(xAcceleration, yAcceleration)
        rebuildQMatrix(dtUpdate, accelerationDeviation)
        lastPredictTime = time
        kalmanFilter.predict()
    }

    fun update(
        x: Float,
        y: Float,
        xVel: Float,
        yVel: Float,
        posDev: Float,
        velErr: Float
    ) = synchronized(lock) {
        lastUpdateTime = SystemClock.elapsedRealtimeNanos()
        rebuildR(posDev, velErr)
        if (useGpsSpeed) {
            kalmanFilter.Zk = columnMatrix(x, y, xVel, yVel)
        } else {
            kalmanFilter.Zk = columnMatrix(x, y)
        }
        kalmanFilter.update()
    }

    private fun rebuildFMatrix(dtPredict: Float) {
        kalmanFilter.F = identityMatrix(4)
        kalmanFilter.F[0][2] = dtPredict
        kalmanFilter.F[1][3] = dtPredict
    }

    private fun rebuildUMatrix(
        xAcc: Float,
        yAcc: Float
    ) {
        kalmanFilter.Uk = columnMatrix(xAcc, yAcc)
    }

    private fun rebuildBMatrix(dtPredict: Float) {
        val dt2 = 0.5f * dtPredict * dtPredict
        kalmanFilter.B = createMatrix(4, 2, 0f)
        kalmanFilter.B[0][0] = dt2
        kalmanFilter.B[1][1] = dt2
        kalmanFilter.B[2][0] = dtPredict
        kalmanFilter.B[3][1] = dtPredict
    }

    private fun rebuildR(posSigma: Float, velSigma: Float) {
        if (useGpsSpeed) {
            kalmanFilter.R = identityMatrix(4).multiply(posSigma)
            kalmanFilter.R[2][2] = velSigma
            kalmanFilter.R[3][3] = velSigma
        } else {
            kalmanFilter.R = identityMatrix(2).multiply(posSigma)
        }
    }

    private fun rebuildQMatrix(
        dtUpdate: Float,
        accelerationDeviation: Float
    ) {
        kalmanFilter.Q = identityMatrix(4).multiply(accelerationDeviation * dtUpdate)
    }
}