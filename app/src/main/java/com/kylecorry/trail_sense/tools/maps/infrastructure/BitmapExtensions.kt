package com.kylecorry.trail_sense.tools.maps.infrastructure

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.fixPerspective
import com.kylecorry.trail_sense.tools.maps.domain.PixelBounds


// Don't allow concave polygons
fun Bitmap.fixPerspective(
    bounds: PixelBounds,
    shouldRecycleOriginal: Boolean = false,
    @ColorInt backgroundColor: Int? = null
): Bitmap {
    return fixPerspective(
        bounds.topLeft,
        bounds.topRight,
        bounds.bottomLeft,
        bounds.bottomRight,
        shouldRecycleOriginal,
        backgroundColor
    )
}