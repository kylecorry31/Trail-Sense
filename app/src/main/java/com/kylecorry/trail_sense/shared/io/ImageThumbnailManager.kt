package com.kylecorry.trail_sense.shared.io

import android.graphics.Bitmap
import android.widget.ImageView
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ImageThumbnailManager {

    private val bitmaps = mutableMapOf<Int, Bitmap>()
    private val jobs = mutableMapOf<Int, Job>()
    private val views = mutableMapOf<Int, ImageView>()

    fun setImage(
        scope: CoroutineScope,
        view: ImageView,
        load: suspend CoroutineScope.() -> Bitmap?
    ) {
        synchronized(this) {
            views[view.hashCode()] = view
            jobs[view.hashCode()]?.cancel()
            jobs[view.hashCode()] = scope.launch {
                onIO {
                    val previous = bitmaps[view.hashCode()]
                    val loaded = load() ?: return@onIO
                    bitmaps[view.hashCode()] = loaded
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
            views.forEach { it.value.setImageDrawable(null) }
            bitmaps.forEach { it.value.recycle() }
            views.clear()
            jobs.clear()
            bitmaps.clear()
        }
    }

}