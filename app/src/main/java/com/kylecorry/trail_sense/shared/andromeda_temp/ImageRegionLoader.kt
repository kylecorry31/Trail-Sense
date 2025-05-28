package com.kylecorry.trail_sense.shared.andromeda_temp

import android.graphics.Bitmap
import android.graphics.BitmapFactory.Options
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.util.Size
import androidx.core.graphics.createBitmap
import com.kylecorry.andromeda.bitmaps.BitmapUtils
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

object ImageRegionLoader {

    fun decodeBitmapRegionWrapped(
        stream: InputStream,
        rect: Rect,
        imageSize: Size,
        wrap: Boolean = false,
        options: Options? = null,
        enforceBounds: Boolean = true
    ): Bitmap {
        val left = rect.left
        val top = rect.top
        val right = rect.right
        val width = rect.width()
        val height = rect.height()
        val fullImageWidth = imageSize.width

        val resultBitmap = createBitmap(width, height)
        val canvas = Canvas(resultBitmap)

        val rectsToLoad = mutableListOf<Pair<Point, Rect>>()

        // Center
        val centerIntersection = getIntersection(rect, imageSize)
        val centerOffsetX = centerIntersection.left - left
        val centerOffsetY = centerIntersection.top - top
        if (centerIntersection.width() > 0 && centerIntersection.height() > 0) {
            rectsToLoad.add(
                Pair(
                    Point(centerOffsetX, centerOffsetY),
                    centerIntersection
                )
            )
        }

        // Left (display the right side of the image)
        if (wrap && centerOffsetX > 0) {
            val leftRect = Rect(
                fullImageWidth - centerOffsetX,
                centerIntersection.top,
                fullImageWidth,
                centerIntersection.bottom
            )
            rectsToLoad.add(
                Pair(
                    Point(0, centerOffsetY),
                    leftRect
                )
            )
        }

        // Right (display the left side of the image)
        if (wrap && right > fullImageWidth) {
            val rightRect = Rect(
                0,
                centerIntersection.top,
                right - fullImageWidth,
                centerIntersection.bottom
            )
            rectsToLoad.add(
                Pair(
                    Point(centerIntersection.width() + centerOffsetX, centerOffsetY),
                    rightRect
                )
            )
        }

        for ((offset, rectToLoad) in rectsToLoad) {
            val bitmap = BitmapUtils.decodeRegion(
                stream,
                rectToLoad,
                options,
                autoClose = false,
                enforceBounds = enforceBounds
            ) ?: continue
            val sourceRect = Rect(0, 0, bitmap.width, bitmap.height)
            val destRect = Rect(
                offset.x,
                offset.y,
                offset.x + rectToLoad.width(),
                offset.y + rectToLoad.height()
            )
            canvas.drawBitmap(bitmap, sourceRect, destRect, null)
            bitmap.recycle()
        }

        return resultBitmap
    }

    private fun getIntersection(rect: Rect, imageSize: Size): Rect {
        val left = max(0, rect.left)
        val top = max(0, rect.top)
        val right = min(imageSize.width, rect.right)
        val bottom = min(imageSize.height, rect.bottom)
        return Rect(left, top, right, bottom)
    }

}