package com.kylecorry.trail_sense.shared.io

import android.graphics.Bitmap
import android.widget.ImageView
import com.kylecorry.andromeda.core.coroutines.ControlledRunner
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ImageThumbnailManager {

    private val bitmaps = mutableMapOf<Int, Bitmap>()
    private val views = mutableMapOf<Int, ImageView>()
    private val runners = mutableMapOf<Int, ControlledRunner<Unit>>()

    fun setImage(
        scope: CoroutineScope,
        view: ImageView,
        load: suspend CoroutineScope.() -> Bitmap?
    ) {
        synchronized(this) {
            val runner = runners.getOrPut(view.hashCode()) { ControlledRunner() }
            views[view.hashCode()] = view
            scope.launch {
                runner.cancelPreviousThenRun {
                    onMain {
                        view.setImageDrawable(null)
                    }
                    onIO {
                        removeBitmap(view.hashCode())
                        val loaded = load() ?: return@onIO
                        bitmaps[view.hashCode()] = loaded
                        onMain {
                            view.setImageBitmap(bitmaps[view.hashCode()])
                        }
                    }
                }
            }
        }
    }

    private fun removeBitmap(id: Int) {
        synchronized(this) {
            val previous = bitmaps[id]
            bitmaps.remove(id)
            previous?.recycle()
        }
    }

    fun clear() {
        synchronized(this) {
            runners.forEach { it.value.cancel() }
            views.forEach { it.value.setImageDrawable(null) }
            bitmaps.forEach { it.value.recycle() }
            views.clear()
            runners.clear()
            bitmaps.clear()
        }
    }

}