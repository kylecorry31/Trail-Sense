package com.kylecorry.trail_sense.shared.camera

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Range
import com.kylecorry.andromeda.bitmaps.BitmapUtils.blobs
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.canvas.PixelCircle
import kotlin.math.max

class GrayscalePointFinder(
    private val threshold: Float,
    private val minRadius: Float,
    private val aspectRatioRange: Range<Float>
) {

    fun getPoints(bitmap: Bitmap): List<PixelCircle> {
        val clusters = bitmap.blobs(threshold)

        return clusters
            .filter {
                val aspectRatio = it.width().toFloat() / it.height().toFloat()
                aspectRatioRange.contains(aspectRatio)
            }
            .map {
                PixelCircle(
                    getCenter(it),
                    max(it.width().toFloat() / 2f, it.height().toFloat() / 2f)
                )
            }.filter {
                it.radius >= minRadius
            }.sortedBy { it.radius }
    }

    private fun getCenter(rect: Rect): PixelCoordinate {
        return PixelCoordinate(rect.centerX().toFloat(), rect.centerY().toFloat())
    }


}