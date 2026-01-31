package com.kylecorry.trail_sense.shared.andromeda_temp

import android.graphics.Bitmap
import android.util.Size
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.kylecorry.andromeda.bitmaps.operations.BitmapOperation
import kotlin.math.abs

/**
 * An upscaler that preserves the colors of the pixels but attempts to smooth edges
 */
class PixelPreservationUpscale(
    private val size: Size
) : BitmapOperation {
    override fun execute(bitmap: Bitmap): Bitmap {
        var array = bitmap.getPixels()
        var width = bitmap.width
        var height = bitmap.height
        while (width < size.width && height < size.height) {
            array = upscale2x(array, width, height)
            width *= 2
            height *= 2
        }
        return Bitmap.createBitmap(array, width, height, bitmap.config)
    }

    // xBR tutorial: https://forums.libretro.com/t/xbr-algorithm-tutorial/123
    private fun upscale2x(src: IntArray, width: Int, height: Int): IntArray {
        val destinationWidth = width * 2
        val destinationHeight = height * 2
        val destination = IntArray(destinationWidth * destinationHeight)

        for (y in 0 until height) {
            for (x in 0 until width) {
                // 5x5 neighborhood (clamped at edges):
                //     A1 B1 C1
                // A0  A  B  C  C4
                // D0  D  E  F  F4
                // G0  G  H  I  I4
                //     G5 H5 I5
                val a = get(src, x - 1, y - 1, width, height)
                val b = get(src, x, y - 1, width, height)
                val c = get(src, x + 1, y - 1, width, height)
                val d = get(src, x - 1, y, width, height)
                val e = get(src, x, y, width, height)
                val f = get(src, x + 1, y, width, height)
                val g = get(src, x - 1, y + 1, width, height)
                val h = get(src, x, y + 1, width, height)
                val i = get(src, x + 1, y + 1, width, height)
                val a1 = get(src, x - 1, y - 2, width, height)
                val b1 = get(src, x, y - 2, width, height)
                val c1 = get(src, x + 1, y - 2, width, height)
                val a0 = get(src, x - 2, y - 1, width, height)
                val c4 = get(src, x + 2, y - 1, width, height)
                val d0 = get(src, x - 2, y, width, height)
                val f4 = get(src, x + 2, y, width, height)
                val g0 = get(src, x - 2, y + 1, width, height)
                val i4 = get(src, x + 2, y + 1, width, height)
                val g5 = get(src, x - 1, y + 2, width, height)
                val h5 = get(src, x, y + 2, width, height)
                val i5 = get(src, x + 1, y + 2, width, height)

                // 4x4 output for original E pixel
                // E0 E1
                // E2 E3
                var e0 = e
                var e1 = e
                var e2 = e
                var e3 = e

                // Each corner is processed by applying the same rules rotated by symmetry.
                // For each edge: compute EDR, then apply Level 2 (if conditions met) or Level 1.

                // E3 (bottom-right) - edge between H and F
                val e3wd1 = d(e, c) + d(e, g) + d(i, f4) + d(i, h5) + 4 * d(h, f)
                val e3wd2 = d(h, d) + d(h, i5) + d(f, i4) + d(f, b) + 4 * d(e, i)
                if (e3wd1 < e3wd2) {
                    val newColor = if (d(e, f) <= d(e, h)) f else h
                    if (f == g && h == c) {
                        e3 = newColor
                        e2 = newColor
                        e1 = newColor
                    } else if (f == g) {
                        e3 = newColor
                        e2 = newColor
                    } else if (h == c) {
                        e3 = newColor
                        e1 = newColor
                    } else {
                        e3 = newColor
                    }
                }

                // E1 (top-right) - edge between B and F
                val e1wd1 = d(e, i) + d(e, a) + d(c, f4) + d(c, b1) + 4 * d(b, f)
                val e1wd2 = d(b, d) + d(b, c1) + d(f, h) + d(f, c4) + 4 * d(e, c)
                if (e1wd1 < e1wd2) {
                    val newColor = if (d(e, f) <= d(e, b)) f else b
                    if (f == a && b == i) {
                        e1 = newColor
                        e0 = newColor
                        e3 = newColor
                    } else if (f == a) {
                        e1 = newColor
                        e0 = newColor
                    } else if (b == i) {
                        e1 = newColor
                        e3 = newColor
                    } else {
                        e1 = newColor
                    }
                }

                // E0 (top-left) - edge between B and D
                val e0wd1 = d(e, g) + d(e, c) + d(a, d0) + d(a, b1) + 4 * d(b, d)
                val e0wd2 = d(b, f) + d(b, a1) + d(d, h) + d(d, a0) + 4 * d(e, a)
                if (e0wd1 < e0wd2) {
                    val newColor = if (d(e, d) <= d(e, b)) d else b
                    if (d == c && b == g) {
                        e0 = newColor
                        e1 = newColor
                        e2 = newColor
                    } else if (d == c) {
                        e0 = newColor
                        e1 = newColor
                    } else if (b == g) {
                        e0 = newColor
                        e2 = newColor
                    } else {
                        e0 = newColor
                    }
                }

                // E2 (bottom-left) - edge between D and H
                val e2wd1 = d(e, i) + d(e, a) + d(g, d0) + d(g, h5) + 4 * d(h, d)
                val e2wd2 = d(h, f) + d(h, g5) + d(d, b) + d(d, g0) + 4 * d(e, g)
                if (e2wd1 < e2wd2) {
                    val newColor = if (d(e, d) <= d(e, h)) d else h
                    if (d == i && h == a) {
                        e2 = newColor
                        e3 = newColor
                        e0 = newColor
                    } else if (d == i) {
                        e2 = newColor
                        e3 = newColor
                    } else if (h == a) {
                        e2 = newColor
                        e0 = newColor
                    } else {
                        e2 = newColor
                    }
                }

                destination[(y * 2) * destinationWidth + (x * 2)] = e0
                destination[(y * 2) * destinationWidth + (x * 2) + 1] = e1
                destination[(y * 2 + 1) * destinationWidth + (x * 2)] = e2
                destination[(y * 2 + 1) * destinationWidth + (x * 2) + 1] = e3
            }
        }

        return destination
    }

    private fun get(src: IntArray, x: Int, y: Int, width: Int, height: Int): Int {
        val cx = x.coerceIn(0, width - 1)
        val cy = y.coerceIn(0, height - 1)
        return src[cy * width + cx]
    }

    private fun d(c1: Int, c2: Int): Float {
        val r = abs(c1.red - c2.red)
        val g = abs(c1.green - c2.green)
        val b = abs(c1.blue - c2.blue)
        val a = abs(c1.alpha - c2.alpha)

        val y = abs(0.299 * r + 0.587 * g + 0.114 * b)
        val u = abs(-0.169 * r - 0.331 * g + 0.500 * b)
        val v = abs(0.500 * r - 0.419 * g - 0.081 * b)

        return (48.0 * y + 7.0 * u + 6.0 * v + 48.0 * a).toFloat()
    }
}