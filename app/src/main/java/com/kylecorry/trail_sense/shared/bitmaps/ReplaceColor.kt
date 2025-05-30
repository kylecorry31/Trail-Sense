package com.kylecorry.trail_sense.shared.bitmaps

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.bitmaps.BitmapUtils.replaceColor
import com.kylecorry.sol.math.SolMath

class ReplaceColor(
    @ColorInt private val sourceColor: Int,
    @ColorInt private val destinationColor: Int,
    private val threshold: Float = SolMath.EPSILON_FLOAT,
    private val interpolate: Boolean = false,
    private val inPlace: Boolean = true
) : BitmapOperation {
    override fun execute(bitmap: Bitmap): Bitmap {
        return bitmap.replaceColor(
            sourceColor,
            destinationColor,
            threshold,
            interpolate,
            inPlace
        )
    }
}