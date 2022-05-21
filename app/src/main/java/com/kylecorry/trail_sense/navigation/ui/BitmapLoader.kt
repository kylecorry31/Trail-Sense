package com.kylecorry.trail_sense.navigation.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.toBitmap
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrNothing

class BitmapLoader(private val context: Context) {

    private val icons = mutableMapOf<Int, Bitmap>()

    fun load(@DrawableRes id: Int, size: Int): Bitmap {
        val bitmap = if (icons.containsKey(id)) {
            icons[id]
        } else {
            val drawable = Resources.drawable(context, id)
            val bm = drawable?.toBitmap(size, size)
            icons[id] = bm!!
            icons[id]
        }
        return bitmap!!
    }

    fun clear(){
        tryOrNothing {
            for (icon in icons) {
                icon.value.recycle()
            }
            icons.clear()
        }
    }

}