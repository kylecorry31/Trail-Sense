package com.kylecorry.trail_sense.shared.io

import android.graphics.Bitmap
import android.widget.ImageView
import com.kylecorry.andromeda.core.bitmap.BitmapUtils
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ImageThumbnailManager {

    private val bitmaps = mutableMapOf<Int, Bitmap>()
    private val jobs = mutableMapOf<Int, Job>()

    fun setImage(
        scope: CoroutineScope,
        view: ImageView,
        load: suspend CoroutineScope.() -> Bitmap
    ) {
        synchronized(this) {
            jobs[view.hashCode()]?.cancel()
            jobs[view.hashCode()] = scope.launch {
                onIO {
                    val previous = bitmaps[view.hashCode()]
                    bitmaps[view.hashCode()] = load()
                    onMain {
                        view.setImageBitmap(bitmaps[view.hashCode()])
                    }
                    previous?.recycle()
                }
            }
        }
    }

    fun clear() {
        synchronized(this) {
            jobs.forEach { it.value.cancel() }
            bitmaps.forEach { it.value.recycle() }
            jobs.clear()
            bitmaps.clear()
        }
    }

    private fun loadImage(path: String, width: Int, height: Int): Bitmap {
        return BitmapUtils.decodeBitmapScaled(
            path,
            width,
            height
        )
    }

}