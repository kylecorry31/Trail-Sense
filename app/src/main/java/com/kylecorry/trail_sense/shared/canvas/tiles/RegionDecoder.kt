package com.kylecorry.trail_sense.shared.canvas.tiles

import android.graphics.Bitmap
import android.graphics.Rect

interface RegionDecoder {
    suspend fun decodeRegionSuspend(sRect: Rect, sampleSize: Int): Bitmap?
    suspend fun recycleSuspend()
}