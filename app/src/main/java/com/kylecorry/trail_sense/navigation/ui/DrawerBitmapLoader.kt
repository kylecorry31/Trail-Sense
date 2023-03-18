package com.kylecorry.trail_sense.navigation.ui

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.tryOrNothing

class DrawerBitmapLoader(private val drawer: ICanvasDrawer) {

    private val icons = mutableMapOf<Int, Bitmap>()
    private val lock = Any()

    fun load(@DrawableRes id: Int, size: Int): Bitmap {
        return synchronized(lock) {
            val bitmap = if (icons.containsKey(id)) {
                icons[id]
            } else {
                val bm = drawer.loadImage(id, size, size)
                icons[id] = bm
                icons[id]
            }
            bitmap!!
        }
    }

    fun clear() {
        tryOrNothing {
            synchronized(lock) {
                for (icon in icons) {
                    icon.value.recycle()
                }
                icons.clear()
            }
        }
    }

}