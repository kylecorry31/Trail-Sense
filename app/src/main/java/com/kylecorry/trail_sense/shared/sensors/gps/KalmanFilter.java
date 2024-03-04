package com.kylecorry.trail_sense.shared.sensors.gps;

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

class KalmanFilter {
    /*these matrices should be provided by user*/
    public Matrix F; //state transition model
    public Matrix H; //observation model
    public Matrix B; //control matrix
    public Matrix Q; //process noise covariance
    public Matrix R; //observation noise covariance

    /*these matrices will be updated by user*/
    public Matrix Uk; //control vector
    public Matrix Zk; //actual values (measured)
    public Matrix Xk_km1; //predicted state estimate
    public Matrix Pk_km1; //predicted estimate covariance
    public Matrix Yk; //measurement innovation

    public Matrix Sk; //innovation covariance
    public Matrix SkInv; //innovation covariance inverse

    public Matrix K; //Kalman gain (optimal)
    public Matrix Xk_k; //updated (current) state
    public Matrix Pk_k; //updated estimate covariance
    public Matrix Yk_k; //post fit residual

    /*auxiliary matrices*/
    public Matrix auxBxU;
    public Matrix auxSDxSD;
    public Matrix auxSDxMD;

    public KalmanFilter(int stateDimension,
                        int measureDimension,
                        int controlDimension) {
        this.F = new Matrix(stateDimension, stateDimension);
        this.H = new Matrix(measureDimension, stateDimension);
        this.Q = new Matrix(stateDimension, stateDimension);
        this.R = new Matrix(measureDimension, measureDimension);

        this.B = new Matrix(stateDimension, controlDimension);
        this.Uk = new Matrix(controlDimension, 1);

        this.Zk = new Matrix(measureDimension, 1);

        this.Xk_km1 = new Matrix(stateDimension, 1);
        this.Pk_km1 = new Matrix(stateDimension, stateDimension);

        this.Yk = new Matrix(measureDimension, 1);
        this.Sk = new Matrix(measureDimension, measureDimension);
        this.SkInv = new Matrix(measureDimension, measureDimension);

        this.K = new Matrix(stateDimension, measureDimension);

        this.Xk_k = new Matrix(stateDimension, 1);
        this.Pk_k = new Matrix(stateDimension, stateDimension);
        this.Yk_k = new Matrix(measureDimension, 1);

        this.auxBxU = new Matrix(stateDimension, 1);
        this.auxSDxSD = new Matrix(stateDimension, stateDimension);
        this.auxSDxMD = new Matrix(stateDimension, measureDimension);
    }

    public void predict() {
        //Xk|k-1 = Fk*Xk-1|k-1 + Bk*Uk
        Matrix.matrixMultiply(F, Xk_k, Xk_km1);
        Matrix.matrixMultiply(B, Uk, auxBxU);
        Matrix.matrixAdd(Xk_km1, auxBxU, Xk_km1);

        //Pk|k-1 = Fk*Pk-1|k-1*Fk(t) + Qk
        Matrix.matrixMultiply(F, Pk_k, auxSDxSD);
        Matrix.matrixMultiplyByTranspose(auxSDxSD, F, Pk_km1);
        Matrix.matrixAdd(Pk_km1, Q, Pk_km1);
    }

    public void update() {
        //Yk = Zk - Hk*Xk|k-1
        Matrix.matrixMultiply(H, Xk_km1, Yk);
        Matrix.matrixSubtract(Zk, Yk, Yk);

        //Sk = Rk + Hk*Pk|k-1*Hk(t)
        Matrix.matrixMultiplyByTranspose(Pk_km1, H, auxSDxMD);
        Matrix.matrixMultiply(H, auxSDxMD, Sk);
        Matrix.matrixAdd(R, Sk, Sk);

        //Kk = Pk|k-1*Hk(t)*Sk(inv)
        if (!(Matrix.matrixDestructiveInvert(Sk, SkInv)))
            return; //matrix hasn't inversion
        Matrix.matrixMultiply(auxSDxMD, SkInv, K);

        //xk|k = xk|k-1 + Kk*Yk
        Matrix.matrixMultiply(K, Yk, Xk_k);
        Matrix.matrixAdd(Xk_km1, Xk_k, Xk_k);

        //Pk|k = (I - Kk*Hk) * Pk|k-1 - SEE WIKI!!!
        Matrix.matrixMultiply(K, H, auxSDxSD);
        Matrix.matrixSubtractFromIdentity(auxSDxSD);
        Matrix.matrixMultiply(auxSDxSD, Pk_km1, Pk_k);

        //we don't use this :
        //Yk|k = Zk - Hk*Xk|k
//        Matrix.matrixMultiply(H, Xk_k, Yk_k);
//        Matrix.matrixSubtract(Zk, Yk_k, Yk_k);
    }

}