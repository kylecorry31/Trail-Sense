package com.kylecorry.trail_sense.shared.andromeda_temp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Size
import androidx.annotation.ColorInt
import androidx.core.graphics.createBitmap
import com.kylecorry.andromeda.bitmaps.operations.BitmapOperation
import com.kylecorry.andromeda.core.math.MathUtils
import com.kylecorry.andromeda.core.units.PercentBounds
import com.kylecorry.andromeda.core.units.PixelBounds
import com.kylecorry.andromeda.core.units.PixelCoordinate

class CorrectPerspective2 : BitmapOperation {

    private var bounds: PixelBounds? = null
    private var percentBounds: PercentBounds? = null
    private val backgroundColor: Int?
    private var maxSize: Size? = null
    private var outputSize: Size? = null

    private val paint = Paint()

    constructor(
        bounds: PixelBounds,
        @ColorInt backgroundColor: Int? = null,
        maxSize: Size? = null,
        outputSize: Size? = null,
        useBilinearInterpolation: Boolean = true
    ) {
        this.bounds = bounds
        this.percentBounds = null
        this.backgroundColor = backgroundColor
        this.maxSize = maxSize
        this.outputSize = outputSize
        paint.isFilterBitmap = useBilinearInterpolation
    }

    constructor(
        bounds: PercentBounds,
        @ColorInt backgroundColor: Int? = null,
        maxSize: Size? = null,
        outputSize: Size? = null,
        useBilinearScaling: Boolean = true
    ) {
        this.bounds = null
        this.percentBounds = bounds
        this.backgroundColor = backgroundColor
        this.maxSize = maxSize
        this.outputSize = outputSize
        paint.isFilterBitmap = useBilinearScaling
    }

    override fun execute(bitmap: Bitmap): Bitmap {
        val actualBounds = percentBounds?.toPixelBounds(
            bitmap.width.toFloat(),
            bitmap.height.toFloat()
        ) ?: bounds ?: return bitmap

        return bitmap.fixPerspective(
            actualBounds,
            backgroundColor = backgroundColor,
            maxOutputSize = maxSize,
            outputSize = outputSize,
            paint = paint
        )
    }

    private fun Bitmap.fixPerspective(
        bounds: PixelBounds,
        shouldRecycleOriginal: Boolean = false,
        @ColorInt backgroundColor: Int? = null,
        maxOutputSize: Size? = null,
        outputSize: Size? = null,
        paint: Paint? = null
    ): Bitmap {
        return fixPerspective(
            bounds.topLeft,
            bounds.topRight,
            bounds.bottomLeft,
            bounds.bottomRight,
            shouldRecycleOriginal,
            backgroundColor,
            maxOutputSize,
            outputSize,
            paint
        )
    }

    fun Bitmap.fixPerspective(
        topLeft: PixelCoordinate,
        topRight: PixelCoordinate,
        bottomLeft: PixelCoordinate,
        bottomRight: PixelCoordinate,
        shouldRecycleOriginal: Boolean = false,
        @ColorInt backgroundColor: Int? = null,
        maxOutputSize: Size? = null,
        outputSize: Size? = null,
        paint: Paint? = null
    ): Bitmap {
        val top = topLeft.distanceTo(topRight)
        val bottom = bottomLeft.distanceTo(bottomRight)
        var newWidth = ((top + bottom) / 2f).coerceAtLeast(1f)

        val left = topLeft.distanceTo(bottomLeft)
        val right = topRight.distanceTo(bottomRight)
        var newHeight = ((left + right) / 2f).coerceAtLeast(1f)

        if (outputSize != null) {
            newWidth = outputSize.width.toFloat()
            newHeight = outputSize.height.toFloat()
        }

        if (maxOutputSize != null && (newWidth > maxOutputSize.width || newHeight > maxOutputSize.height)) {
            val scale = MathUtils.scaleToBounds(
                Size(newWidth.toInt(), newHeight.toInt()),
                maxOutputSize
            )
            newWidth = scale.width.toFloat()
            newHeight = scale.height.toFloat()
        }

        val matrix = Matrix()
        matrix.setPolyToPoly(
            floatArrayOf(
                topLeft.x, topLeft.y,
                topRight.x, topRight.y,
                bottomRight.x, bottomRight.y,
                bottomLeft.x, bottomLeft.y,
            ),
            0,
            floatArrayOf(
                0f, 0f,
                newWidth, 0f,
                newWidth, newHeight,
                0f, newHeight
            ),
            0,
            4
        )

        // Create an empty mutable bitmap
        val blank =
            createBitmap(newWidth.toInt(), newHeight.toInt(), config ?: Bitmap.Config.ARGB_8888)
        // Create a canvas to draw on
        val canvas = Canvas(blank)

        if (backgroundColor != null) {
            canvas.drawColor(backgroundColor)
        }

        // Apply matrix to canvas
        canvas.concat(matrix)

        canvas.drawBitmap(this, 0f, 0f, paint)

        if (shouldRecycleOriginal) {
            this.recycle()
        }

        return blank
    }
}