package com.kylecorry.trail_sense.shared.bitmaps

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import com.kylecorry.trail_sense.tools.photo_maps.domain.PercentBounds
import com.kylecorry.trail_sense.tools.photo_maps.domain.PixelBounds
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.fixPerspective

class CorrectPerspective : BitmapOperation {

    private var bounds: PixelBounds? = null
    private var percentBounds: PercentBounds? = null
    private val backgroundColor: Int?

    constructor(
        bounds: PixelBounds, @ColorInt backgroundColor: Int? = null
    ) {
        this.bounds = bounds
        this.percentBounds = null
        this.backgroundColor = backgroundColor
    }

    constructor(
        bounds: PercentBounds, @ColorInt backgroundColor: Int? = null
    ) {
        this.bounds = null
        this.percentBounds = bounds
        this.backgroundColor = backgroundColor
    }

    override fun execute(bitmap: Bitmap): Bitmap {
        val actualBounds = percentBounds?.toPixelBounds(
            bitmap.width.toFloat(),
            bitmap.height.toFloat()
        ) ?: bounds ?: return bitmap

        return bitmap.fixPerspective(actualBounds, backgroundColor = backgroundColor)
    }
}