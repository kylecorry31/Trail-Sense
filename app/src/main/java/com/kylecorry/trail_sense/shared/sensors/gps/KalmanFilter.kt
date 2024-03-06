package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.sol.math.algebra.Matrix
import com.kylecorry.sol.math.algebra.add
import com.kylecorry.sol.math.algebra.createMatrix
import com.kylecorry.sol.math.algebra.dot
import com.kylecorry.sol.math.algebra.identityMatrix
import com.kylecorry.sol.math.algebra.inverse
import com.kylecorry.sol.math.algebra.rows
import com.kylecorry.sol.math.algebra.subtract
import com.kylecorry.sol.math.algebra.transpose

/**
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
internal class KalmanFilter(
    stateDimension: Int,
    measureDimension: Int,
    controlDimension: Int
) {
    // State transition model
    var F = createMatrix(stateDimension, stateDimension) { _, _ -> 0f }

    // Observation model
    var H = createMatrix(measureDimension, stateDimension) { _, _ -> 0f }

    // Control matrix
    var B = createMatrix(stateDimension, controlDimension) { _, _ -> 0f }

    // Process noise covariance
    var Q = createMatrix(stateDimension, stateDimension) { _, _ -> 0f }

    // Observation noise covariance
    var R = createMatrix(measureDimension, measureDimension) { _, _ -> 0f }

    // Control vector
    var Uk = createMatrix(controlDimension, 1) { _, _ -> 0f }

    // Actual values (measured)
    var Zk = createMatrix(measureDimension, 1) { _, _ -> 0f }

    // Predicted state estimate
    var Xk_km1 = createMatrix(stateDimension, 1) { _, _ -> 0f }

    // Predicted estimate covariance
    var Pk_km1 = createMatrix(stateDimension, stateDimension) { _, _ -> 0f }

    // Measurement innovation
    var Yk = createMatrix(measureDimension, 1) { _, _ -> 0f }

    // Innovation covariance
    var Sk = createMatrix(measureDimension, measureDimension) { _, _ -> 0f }

    // Innovation covariance inverse
    var SkInv = createMatrix(measureDimension, measureDimension) { _, _ -> 0f }

    // Kalman gain (optimal)
    var K = createMatrix(stateDimension, measureDimension) { _, _ -> 0f }

    // Updated (current) state
    var Xk_k = createMatrix(stateDimension, 1) { _, _ -> 0f }

    // Updated estimate covariance
    var Pk_k = createMatrix(stateDimension, stateDimension) { _, _ -> 0f }

    // Post fit residual - not used yet
//    val Yk_k = Matrix(measureDimension, 1)

    /* Auxiliary matrices */
    private val auxBxU = createMatrix(stateDimension, 1) { _, _ -> 0f }
    private var auxSDxSD = createMatrix(stateDimension, stateDimension) { _, _ -> 0f }
    private var auxSDxMD = createMatrix(stateDimension, measureDimension) { _, _ -> 0f }

    fun predict() {
        //Xk|k-1 = Fk*Xk-1|k-1 + Bk*Uk
        Xk_km1 = F.dot(Xk_k).add(B.dot(Uk))
//        Matrix.matrixMultiply(F, Xk_k, Xk_km1)
//        Matrix.matrixMultiply(B, Uk, auxBxU)
//        Matrix.matrixAdd(Xk_km1, auxBxU, Xk_km1)

        //Pk|k-1 = Fk*Pk-1|k-1*Fk(t) + Qk
        auxSDxSD = F.dot(Pk_k)
        Pk_km1 = auxSDxSD.dot(F.transpose())
        Pk_km1 = Pk_km1.add(Q)
//        Matrix.matrixMultiply(F, Pk_k, auxSDxSD)
//        Matrix.matrixMultiplyByTranspose(auxSDxSD, F, Pk_km1)
//        Matrix.matrixAdd(Pk_km1, Q, Pk_km1)
    }

    fun update() {
        //Yk = Zk - Hk*Xk|k-1
        Yk = Zk.subtract(H.dot(Xk_km1))
//        Matrix.matrixMultiply(H, Xk_km1, Yk)
//        Matrix.matrixSubtract(Zk, Yk, Yk)

        //Sk = Rk + Hk*Pk|k-1*Hk(t)
        auxSDxMD = Pk_km1.dot(H.transpose())
        Sk = R.add(H.dot(auxSDxMD))
//        Matrix.matrixMultiplyByTranspose(Pk_km1, H, auxSDxMD)
//        Matrix.matrixMultiply(H, auxSDxMD, Sk)
//        Matrix.matrixAdd(R, Sk, Sk)

        //Kk = Pk|k-1*Hk(t)*Sk(inv)
        SkInv = Sk.inverse()
        K = auxSDxMD.dot(SkInv)
//        if (!Matrix.matrixDestructiveInvert(Sk, SkInv)) return  // No inverse, can't continue
//        Matrix.matrixMultiply(auxSDxMD, SkInv, K)

        //xk|k = xk|k-1 + Kk*Yk
        Xk_k = Xk_km1.add(K.dot(Yk))
//        Matrix.matrixMultiply(K, Yk, Xk_k)
//        Matrix.matrixAdd(Xk_km1, Xk_k, Xk_k)

        //Pk|k = (I - Kk*Hk) * Pk|k-1 - SEE WIKI!!!
        auxSDxSD = K.dot(H)
        auxSDxSD = identityMatrix(auxSDxSD.rows()).subtract(auxSDxSD)
        Pk_k = auxSDxSD.dot(Pk_km1)
//        Matrix.matrixMultiply(K, H, auxSDxSD)
//        Matrix.matrixSubtractFromIdentity(auxSDxSD)
//        Matrix.matrixMultiply(auxSDxSD, Pk_km1, Pk_k)

        //we don't use this :
        //Yk|k = Zk - Hk*Xk|k
//        Matrix.matrixMultiply(H, Xk_k, Yk_k);
//        Matrix.matrixSubtract(Zk, Yk_k, Yk_k);
    }
}