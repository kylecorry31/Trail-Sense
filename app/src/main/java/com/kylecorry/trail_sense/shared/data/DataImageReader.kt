package com.kylecorry.trail_sense.shared.data

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Size

interface DataImageReader {
    fun getSize(): Size

    /**
     * Gets a region of the image as a 3D array of floats.
     * @param rect The rectangle region to get.
     * @param config The bitmap configuration to use when decoding the image.
     * @return A pair containing the 3D array of floats and a boolean indicating if any data was found in the region
     */
    suspend fun getRegion(
        rect: Rect,
        config: Bitmap.Config = Bitmap.Config.ARGB_8888
    ): Pair<FloatBitmap, Boolean>?
}