package com.kylecorry.trail_sense.shared.sensors.gps

import kotlin.math.abs

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
internal class Matrix(private val rows: Int, private val cols: Int) {
    val data: Array<DoubleArray> = Array(rows) { DoubleArray(cols) }

    fun setData(vararg args: Double) {
        assert(args.size == rows * cols)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                data[r][c] = args[r * cols + c]
            }
        }
    }

    fun setData(vararg args: Float) {
        assert(args.size == rows * cols)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                data[r][c] = args[r * cols + c].toDouble()
            }
        }
    }

    fun setIdentityDiag() {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                data[r][c] = 0.0
            }
            data[r][r] = 1.0
        }
    }

    fun setIdentity() {
        assert(rows == cols)
        setIdentityDiag()
    }

    fun subtractFromIdentity() {
        var r: Int
        var c: Int
        r = 0
        while (r < rows) {
            c = 0
            while (c < r) {
                data[r][c] = -data[r][c]
                ++c
            }
            data[r][r] = 1.0 - data[r][r]
            c = r + 1
            while (c < cols) {
                data[r][c] = -data[r][c]
                ++c
            }
            ++r
        }
    }

    fun scale(scalar: Double) {
        var r: Int
        var c: Int
        r = 0
        while (r < rows) {
            c = 0
            while (c < cols) {
                data[r][c] *= scalar
                ++c
            }
            ++r
        }
    }

    private fun swapRows(r1: Int, r2: Int) {
        assert(r1 != r2)
        val tmp = data[r1]
        data[r1] = data[r2]
        data[r2] = tmp
    }

    private fun scaleRow(r: Int, scalar: Double) {
        assert(r < rows)
        var c: Int
        c = 0
        while (c < cols) {
            data[r][c] *= scalar
            ++c
        }
    }

    fun shearRow(
        r1: Int, r2: Int, scalar: Double
    ) {
        assert(r1 != r2)
        assert(r1 < rows && r2 < rows)
        var c: Int
        c = 0
        while (c < cols) {
            data[r1][c] += data[r2][c] * scalar
            ++c
        }
    }

    companion object {
        fun matrixAdd(
            ma: Matrix?, mb: Matrix?, mc: Matrix?
        ) {
            assert(ma != null)
            assert(mb != null)
            assert(mc != null)
            assert(ma!!.cols == mb!!.cols && mb.cols == mc!!.cols)
            assert(ma.rows == mb.rows && mb.rows == mc!!.rows)
            for (r in 0 until ma.rows) {
                for (c in 0 until ma.cols) {
                    mc!!.data[r][c] = ma.data[r][c] + mb.data[r][c]
                }
            }
        }

        fun matrixSubtract(
            ma: Matrix?, mb: Matrix?, mc: Matrix?
        ) {
            assert(ma != null)
            assert(mb != null)
            assert(mc != null)
            assert(ma!!.cols == mb!!.cols && mb.cols == mc!!.cols)
            assert(ma.rows == mb.rows && mb.rows == mc!!.rows)
            for (r in 0 until ma.rows) {
                for (c in 0 until ma.cols) {
                    mc!!.data[r][c] = ma.data[r][c] - mb.data[r][c]
                }
            }
        }

        fun matrixMultiply(
            ma: Matrix?, mb: Matrix?, mc: Matrix?
        ) {
            assert(ma != null)
            assert(mb != null)
            assert(mc != null)
            assert(ma!!.cols == mb!!.rows)
            assert(ma.rows == mc!!.rows)
            assert(mb.cols == mc.cols)
            var r: Int
            var c: Int
            var rc: Int
            val mcrows = mc.rows
            val mccols = mc.cols
            val macols = ma.cols
            r = 0
            while (r < mcrows) {
                c = 0
                while (c < mccols) {
                    mc.data[r][c] = 0.0
                    rc = 0
                    while (rc < macols) {
                        mc.data[r][c] += ma.data[r][rc] * mb.data[rc][c]
                        ++rc
                    }
                    ++c
                }
                ++r
            }
        }

        fun matrixMultiplyByTranspose(
            ma: Matrix?, mb: Matrix?, mc: Matrix?
        ) {
            assert(ma != null)
            assert(mb != null)
            assert(mc != null)
            assert(ma!!.cols == mb!!.cols)
            assert(ma.rows == mc!!.rows)
            assert(mb.rows == mc.cols)
            var r: Int
            var c: Int
            var rc: Int
            r = 0
            while (r < mc.rows) {
                c = 0
                while (c < mc.cols) {
                    mc.data[r][c] = 0.0
                    rc = 0
                    while (rc < ma.cols) {
                        mc.data[r][c] += ma.data[r][rc] * mb.data[c][rc]
                        ++rc
                    }
                    ++c
                }
                ++r
            }
        }

        fun matrixTranspose(
            mtxin: Matrix?, mtxout: Matrix?
        ) {
            assert(mtxin != null)
            assert(mtxout != null)
            assert(mtxin!!.rows == mtxout!!.cols)
            assert(mtxin.cols == mtxout.rows)
            var r: Int
            var c: Int
            r = 0
            while (r < mtxin.rows) {
                c = 0
                while (c < mtxin.cols) {
                    mtxout.data[c][r] = mtxin.data[r][c]
                    ++c
                }
                ++r
            }
        }

        fun matrixEq(
            ma: Matrix?, mb: Matrix?, eps: Double
        ): Boolean {
            assert(ma != null)
            assert(mb != null)
            var r: Int
            var c: Int
            if (ma!!.rows != mb!!.rows || ma.cols != mb.cols) return false
            r = 0
            while (r < ma.rows) {
                c = 0
                while (c < ma.cols) {
                    if (abs(ma.data[r][c] - mb.data[r][c]) <= eps) {
                        ++c
                        continue
                    }
                    return false
                    ++c
                }
                ++r
            }
            return true
        }

        fun matrixCopy(
            mSrc: Matrix?, mDst: Matrix?
        ) {
            assert(mSrc != null)
            assert(mDst != null)
            assert(mSrc!!.rows == mDst!!.rows && mSrc.cols == mDst.cols)
            for (r in 0 until mSrc.rows) {
                for (c in 0 until mSrc.cols) {
                    mDst.data[r][c] = mSrc.data[r][c]
                }
            }
        }

        fun matrixDestructiveInvert(
            mtxin: Matrix?, mtxout: Matrix?
        ): Boolean {
            assert(mtxin != null)
            assert(mtxout != null)
            assert(mtxin!!.cols == mtxin.rows)
            assert(mtxout!!.cols == mtxin.cols)
            assert(mtxout.rows == mtxin.rows)
            var r: Int
            var ri: Int
            var scalar: Double
            mtxout.setIdentity()
            r = 0
            while (r < mtxin.rows) {
                if (mtxin.data[r][r] == 0.0) { //we have to swap rows here to make nonzero diagonal
                    ri = r
                    while (ri < mtxin.rows) {
                        if (mtxin.data[ri][ri] != 0.0) break
                        ++ri
                    }
                    if (ri == mtxin.rows) return false //can't get inverse matrix
                    mtxin.swapRows(r, ri)
                    mtxout.swapRows(r, ri)
                } //if mtxin.data[r][r] == 0.0
                scalar = 1.0 / mtxin.data[r][r]
                mtxin.scaleRow(r, scalar)
                mtxout.scaleRow(r, scalar)
                ri = 0
                while (ri < r) {
                    scalar = -mtxin.data[ri][r]
                    mtxin.shearRow(ri, r, scalar)
                    mtxout.shearRow(ri, r, scalar)
                    ++ri
                }
                ri = r + 1
                while (ri < mtxin.rows) {
                    scalar = -mtxin.data[ri][r]
                    mtxin.shearRow(ri, r, scalar)
                    mtxout.shearRow(ri, r, scalar)
                    ++ri
                }
                ++r
            }
            return true
        }

        fun matrixSubtractFromIdentity(m: Matrix) {
            var r: Int
            var c: Int
            r = 0
            while (r < m.rows) {
                c = 0
                while (c < r) {
                    m.data[r][c] = -m.data[r][c]
                    ++c
                }
                m.data[r][r] = 1.0 - m.data[r][r]
                c = r + 1
                while (c < m.cols) {
                    m.data[r][c] = -m.data[r][c]
                    ++c
                }
                ++r
            }
        }
    }
}