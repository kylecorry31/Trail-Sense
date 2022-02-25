package com.kylecorry.trail_sense.navigation.paths.ui.drawing

import android.graphics.Path
import android.graphics.PathDashPathEffect

class DiamondPathEffect(size: Float = 6f, advance: Float = 3 * size, phase: Float = 0f) :
    PathDashPathEffect(
        getDiamondPath(size), advance, phase, Style.ROTATE
    ) {

    companion object {
        private fun getDiamondPath(size: Float): Path {
            val path = Path()
            path.moveTo(0f, -size / 2)
            path.lineTo(size / 2, 0f)
            path.lineTo(0f, size / 2)
            path.lineTo(-size / 2, 0f)
            path.close()
            return path
        }
    }
}