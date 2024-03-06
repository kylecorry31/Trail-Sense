package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.sol.math.algebra.add
import com.kylecorry.sol.math.algebra.createMatrix
import com.kylecorry.sol.math.algebra.dot
import com.kylecorry.sol.math.algebra.identityMatrix
import com.kylecorry.sol.math.algebra.inverse
import com.kylecorry.sol.math.algebra.subtract
import com.kylecorry.sol.math.algebra.transpose

/**
 * MIT License
 *
 * Copyright (c) 2020 Mad Devs
 * Updated by Kyle Corry to convert to Kotlin and sol matrices in 2024
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
internal class KalmanFilter(
    stateDimension: Int,
    measureDimension: Int,
    controlDimension: Int,
    private val updateStateWithPrediction: Boolean = false
) {
    // State transition model
    var F = createMatrix(stateDimension, stateDimension, 0f)

    // Observation model
    var H = createMatrix(measureDimension, stateDimension, 0f)

    // Control matrix
    var B = createMatrix(stateDimension, controlDimension, 0f)

    // Process noise covariance
    var Q = createMatrix(stateDimension, stateDimension, 0f)

    // Observation noise covariance
    var R = createMatrix(measureDimension, measureDimension, 0f)

    // Control vector
    var Uk = createMatrix(controlDimension, 1, 0f)

    // Actual values (measured)
    var Zk = createMatrix(measureDimension, 1, 0f)

    // Predicted state estimate
    var Xk_km1 = createMatrix(stateDimension, 1, 0f)

    // Predicted estimate covariance
    var Pk_km1 = createMatrix(stateDimension, stateDimension, 0f)

    // Measurement innovation
    var Yk = createMatrix(measureDimension, 1, 0f)

    // Innovation covariance
    var Sk = createMatrix(measureDimension, measureDimension, 0f)

    // Kalman gain (optimal)
    var K = createMatrix(stateDimension, measureDimension, 0f)

    // Updated (current) state
    var Xk_k = createMatrix(stateDimension, 1, 0f)

    // Updated estimate covariance
    var Pk_k = createMatrix(stateDimension, stateDimension, 0f)

    // Post fit residual - not used yet
//    val Yk_k = createMatrix(measureDimension, 1, 0f)

    fun predict() {
        //Xk|k-1 = Fk*Xk-1|k-1 + Bk*Uk
        Xk_km1 = F.dot(Xk_k).add(B.dot(Uk))

        //Pk|k-1 = Fk*Pk-1|k-1*Fk(t) + Qk
        Pk_km1 = F.dot(Pk_k).dot(F.transpose()).add(Q)

        // Update the current state (the original library had this as an option)
        if (updateStateWithPrediction) {
            println("State")
            Xk_k = Xk_km1.clone()
            Pk_k = Pk_km1.clone()
        }
    }

    fun update() {
        //Yk = Zk - Hk*Xk|k-1
        Yk = Zk.subtract(H.dot(Xk_km1))

        val pk_km1DotHkt = Pk_km1.dot(H.transpose())

        //Sk = Rk + Hk*Pk|k-1*Hk(t)
        Sk = R.add(H.dot(pk_km1DotHkt))

        //Kk = Pk|k-1*Hk(t)*Sk(inv)
        K = pk_km1DotHkt.dot(Sk.inverse())

        //xk|k = xk|k-1 + Kk*Yk
        Xk_k = Xk_km1.add(K.dot(Yk))

        //Pk|k = Pk|k-1 - Kk*Hk*Pk|k-1
        Pk_k = Pk_km1.subtract(K.dot(H.dot(Pk_km1)))

        // This is not used yet
        //Yk|k = Zk - Hk*Xk|k
//        Yk_k = Zk.subtract(H.dot(Xk_k))
    }
}