package com.kylecorry.trail_sense.shared.sensors.gps

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
    val F = Matrix(stateDimension, stateDimension)

    // Observation model
    val H = Matrix(measureDimension, stateDimension)

    // Control matrix
    val B = Matrix(stateDimension, controlDimension)

    // Process noise covariance
    val Q = Matrix(stateDimension, stateDimension)

    // Observation noise covariance
    val R = Matrix(measureDimension, measureDimension)

    // Control vector
    val Uk = Matrix(controlDimension, 1)

    // Actual values (measured)
    val Zk = Matrix(measureDimension, 1)

    // Predicted state estimate
    val Xk_km1 = Matrix(stateDimension, 1)

    // Predicted estimate covariance
    val Pk_km1 = Matrix(stateDimension, stateDimension)

    // Measurement innovation
    val Yk = Matrix(measureDimension, 1)

    // Innovation covariance
    val Sk = Matrix(measureDimension, measureDimension)

    // Innovation covariance inverse
    val SkInv = Matrix(measureDimension, measureDimension)

    // Kalman gain (optimal)
    val K = Matrix(stateDimension, measureDimension)

    // Updated (current) state
    val Xk_k = Matrix(stateDimension, 1)

    // Updated estimate covariance
    val Pk_k = Matrix(stateDimension, stateDimension)

    // Post fit residual - not used yet
//    val Yk_k = Matrix(measureDimension, 1)

    /* Auxiliary matrices */
    private val auxBxU = Matrix(stateDimension, 1)
    private val auxSDxSD = Matrix(stateDimension, stateDimension)
    private val auxSDxMD = Matrix(stateDimension, measureDimension)

    fun predict() {
        //Xk|k-1 = Fk*Xk-1|k-1 + Bk*Uk
        Matrix.matrixMultiply(F, Xk_k, Xk_km1)
        Matrix.matrixMultiply(B, Uk, auxBxU)
        Matrix.matrixAdd(Xk_km1, auxBxU, Xk_km1)

        //Pk|k-1 = Fk*Pk-1|k-1*Fk(t) + Qk
        Matrix.matrixMultiply(F, Pk_k, auxSDxSD)
        Matrix.matrixMultiplyByTranspose(auxSDxSD, F, Pk_km1)
        Matrix.matrixAdd(Pk_km1, Q, Pk_km1)
    }

    fun update() {
        //Yk = Zk - Hk*Xk|k-1
        Matrix.matrixMultiply(H, Xk_km1, Yk)
        Matrix.matrixSubtract(Zk, Yk, Yk)

        //Sk = Rk + Hk*Pk|k-1*Hk(t)
        Matrix.matrixMultiplyByTranspose(Pk_km1, H, auxSDxMD)
        Matrix.matrixMultiply(H, auxSDxMD, Sk)
        Matrix.matrixAdd(R, Sk, Sk)

        //Kk = Pk|k-1*Hk(t)*Sk(inv)
        if (!Matrix.matrixDestructiveInvert(Sk, SkInv)) return  // No inverse, can't continue
        Matrix.matrixMultiply(auxSDxMD, SkInv, K)

        //xk|k = xk|k-1 + Kk*Yk
        Matrix.matrixMultiply(K, Yk, Xk_k)
        Matrix.matrixAdd(Xk_km1, Xk_k, Xk_k)

        //Pk|k = (I - Kk*Hk) * Pk|k-1 - SEE WIKI!!!
        Matrix.matrixMultiply(K, H, auxSDxSD)
        Matrix.matrixSubtractFromIdentity(auxSDxSD)
        Matrix.matrixMultiply(auxSDxSD, Pk_km1, Pk_k)

        //we don't use this :
        //Yk|k = Zk - Hk*Xk|k
//        Matrix.matrixMultiply(H, Xk_k, Yk_k);
//        Matrix.matrixSubtract(Zk, Yk_k, Yk_k);
    }
}