package com.kylecorry.trail_sense.shared.camera

import android.graphics.Bitmap
import android.graphics.Rect
import com.kylecorry.andromeda.bitmaps.BitmapUtils.moment
import com.kylecorry.andromeda.bitmaps.BitmapUtils.threshold
import com.kylecorry.andromeda.bitmaps.BitmapUtils.use
import com.kylecorry.andromeda.core.units.PixelCoordinate

class GrayscaleMomentFinder(private val threshold: Float, private val minPixels: Int) {

    fun getMoment(bitmap: Bitmap, rect: Rect? = null): PixelCoordinate? {
        if (threshold == 0f) {
            return bitmap.moment(rect = rect)
        }

        var moment: PixelCoordinate? = null
        bitmap.threshold(threshold).use {
            // TODO: Count the number of non-zero pixels (or use moment values to determine the shape/size)
            moment = moment(rect = rect)
        }
        return moment
    }

}