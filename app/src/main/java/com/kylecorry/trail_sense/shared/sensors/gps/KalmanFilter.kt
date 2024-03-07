package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.sol.math.algebra.add
import com.kylecorry.sol.math.algebra.createMatrix
import com.kylecorry.sol.math.algebra.dot
import com.kylecorry.sol.math.algebra.inverse
import com.kylecorry.sol.math.algebra.subtract
import com.kylecorry.sol.math.algebra.transpose

internal class KalmanFilter(
    stateSize: Int,
    measurementSize: Int,
    controlSize: Int,
    private val updateStateWithPrediction: Boolean = false
) {
    // State transition model
    var F = createMatrix(stateSize, stateSize, 0f)

    // Observation model
    var H = createMatrix(measurementSize, stateSize, 0f)

    // Control matrix
    var B = createMatrix(stateSize, controlSize, 0f)

    // Process noise covariance
    var Q = createMatrix(stateSize, stateSize, 0f)

    // Observation noise covariance
    var R = createMatrix(measurementSize, measurementSize, 0f)

    // Control vector
    var Uk = createMatrix(controlSize, 1, 0f)

    // Actual values (measured)
    var Zk = createMatrix(measurementSize, 1, 0f)

    // Predicted state estimate
    var Xk_km1 = createMatrix(stateSize, 1, 0f)

    // Predicted estimate covariance
    var Pk_km1 = createMatrix(stateSize, stateSize, 0f)

    // Measurement innovation
    var Yk = createMatrix(measurementSize, 1, 0f)

    // Innovation covariance
    var Sk = createMatrix(measurementSize, measurementSize, 0f)

    // Kalman gain (optimal)
    var K = createMatrix(stateSize, measurementSize, 0f)

    // Updated (current) state
    var Xk_k = createMatrix(stateSize, 1, 0f)

    // Updated estimate covariance
    var Pk_k = createMatrix(stateSize, stateSize, 0f)

    // Post fit residual - not used yet
//    val Yk_k = createMatrix(measureDimension, 1, 0f)

    fun predict() {
        //Xk|k-1 = Fk*Xk-1|k-1 + Bk*Uk
        Xk_km1 = F.dot(Xk_k).add(B.dot(Uk))

        //Pk|k-1 = Fk*Pk-1|k-1*Fk(t) + Qk
        Pk_km1 = F.dot(Pk_k).dot(F.transpose()).add(Q)

        // Update the current state (the original library had this as an option)
        if (updateStateWithPrediction) {
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