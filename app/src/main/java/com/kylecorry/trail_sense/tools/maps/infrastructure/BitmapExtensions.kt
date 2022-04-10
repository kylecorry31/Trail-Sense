package com.kylecorry.trail_sense.tools.maps.infrastructure

import android.graphics.Bitmap
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.fixPerspective
import com.kylecorry.trail_sense.tools.maps.domain.PixelBounds


fun Bitmap.fixPerspective(bounds: PixelBounds): Bitmap {
   return fixPerspective(bounds.topLeft, bounds.topRight, bounds.bottomLeft, bounds.bottomRight)
}