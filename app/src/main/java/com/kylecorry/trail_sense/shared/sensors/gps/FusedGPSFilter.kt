package com.kylecorry.trail_sense.shared.sensors.gps

import java.time.Duration
import java.time.Instant

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
    initialX: Double,
    initialY: Double,
    initialXVelocity: Double,
    initialYVelocity: Double,
    private val accelerationDeviation: Double,
    initialPositionDeviation: Double,
    initialTime: Instant,
    private val velocityFactor: Double = 1.0,
    private val positionFactor: Double = 1.0
) {
    private var lastPredictTime = initialTime
    private var lastUpdateTime = initialTime
    private var predictCount = 0
    private val kalmanFilter = KalmanFilter(4, if (useGpsSpeed) 4 else 2, 2)

    val currentX: Double
        get() = kalmanFilter.Xk_k.data[0][0]
    val currentY: Double
        get() = kalmanFilter.Xk_k.data[1][0]
    val currentXVelocity: Double
        get() = kalmanFilter.Xk_k.data[2][0]
    val currentYVelocity: Double
        get() = kalmanFilter.Xk_k.data[3][0]
    val positionError: Double
        get() = kalmanFilter.Pk_k.data[0][0]
    val velocityError: Double
        get() = kalmanFilter.Pk_k.data[2][2]

    init {
        kalmanFilter.Xk_k.setData(initialX, initialY, initialXVelocity, initialYVelocity)
        // Both state and measurement have 4 dimensions, so use an identity matrix
        kalmanFilter.H.setIdentityDiag()
        kalmanFilter.Pk_k.setIdentity()
        kalmanFilter.Pk_k.scale(initialPositionDeviation)
    }

    fun predict(
        time: Instant,
        xAcceleration: Double,
        yAcceleration: Double
    ) {
        val dtPredict = Duration.between(lastPredictTime, time).toMillis() / 1000.0
        val dtUpdate = Duration.between(lastUpdateTime, time).toMillis() / 1000.0
        rebuildFMatrix(dtPredict)
        rebuildBMatrix(dtPredict)
        rebuildUMatrix(xAcceleration, yAcceleration)
        predictCount++
        rebuildQMatrix(dtUpdate, accelerationDeviation)
        lastPredictTime = time
        kalmanFilter.predict()
        Matrix.matrixCopy(kalmanFilter.Xk_km1, kalmanFilter.Xk_k)
    }

    fun update(
        time: Instant,
        x: Double,
        y: Double,
        xVel: Double,
        yVel: Double,
        posDev: Double,
        velErr: Double
    ) {
        predictCount = 0
        lastUpdateTime = time
        rebuildR(posDev, velErr)
        if (useGpsSpeed) {
            kalmanFilter.Zk.setData(x, y, xVel, yVel)
        } else {
            kalmanFilter.Zk.setData(x, y)
        }
        kalmanFilter.update()
    }

    private fun rebuildFMatrix(dtPredict: Double) {
        val newFMatrix = doubleArrayOf(
            1.0, 0.0, dtPredict, 0.0,
            0.0, 1.0, 0.0, dtPredict,
            0.0, 0.0, 1.0, 0.0,
            0.0, 0.0, 0.0, 1.0
        )
        kalmanFilter.F.setData(*newFMatrix)
    }

    private fun rebuildUMatrix(
        xAcc: Double,
        yAcc: Double
    ) {
        kalmanFilter.Uk.setData(xAcc, yAcc)
    }

    private fun rebuildBMatrix(dtPredict: Double) {
        val dt2 = 0.5 * dtPredict * dtPredict
        val newBMatrix = doubleArrayOf(
            dt2, 0.0,
            0.0, dt2,
            dtPredict, 0.0,
            0.0, dtPredict
        )
        kalmanFilter.B.setData(*newBMatrix)
    }

    private fun rebuildR(posSigma: Double, velSigma: Double) {
        val scaledPosSigma = posSigma * positionFactor
        if (useGpsSpeed) {
            val scaledVelSigma = velSigma * velocityFactor
            val newRMatrix = doubleArrayOf(
                scaledPosSigma, 0.0, 0.0, 0.0,
                0.0, scaledPosSigma, 0.0, 0.0,
                0.0, 0.0, scaledVelSigma, 0.0,
                0.0, 0.0, 0.0, scaledVelSigma
            )
            kalmanFilter.R.setData(*newRMatrix)
        } else {
            kalmanFilter.R.setIdentity()
            kalmanFilter.R.scale(posSigma)
        }
    }

    private fun rebuildQMatrix(
        dtUpdate: Double,
        accelerationDeviation: Double
    ) {
        // The original code uses predict count, but mentioned there might be a way to use dtUpdate instead
//        kalmanFilter.Q.setIdentity();
//        kalmanFilter.Q.scale(accelerationDeviation * dtUpdate);
        val velDev = accelerationDeviation * predictCount
        val posDev = velDev * predictCount / 2
        val covDev = velDev * posDev
        val posSig = posDev * posDev
        val velSig = velDev * velDev
        val newQMatrix = doubleArrayOf(
            posSig, 0.0, covDev, 0.0,
            0.0, posSig, 0.0, covDev,
            covDev, 0.0, velSig, 0.0,
            0.0, covDev, 0.0, velSig
        )
        kalmanFilter.Q.setData(*newQMatrix)
    }
}