package com.kylecorry.trail_sense.shared.andromeda_temp

import com.kylecorry.andromeda.canvas.ICanvasDrawer

fun ICanvasDrawer.layerOpacity(opacity: Int) {
    canvas.saveLayerAlpha(null, opacity)
}

inline fun ICanvasDrawer.withLayerOpacity(
    opacity: Int,
    ignoreFullOpacity: Boolean = true,
    crossinline block: () -> Unit
) {
    if (ignoreFullOpacity && opacity == 255) {
        opacity(opacity)
        block()
        return
    }
    layerOpacity(opacity)
    try {
        block()
    } finally {
        pop()
    }
}