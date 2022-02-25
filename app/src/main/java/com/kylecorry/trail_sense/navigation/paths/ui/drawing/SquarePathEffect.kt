package com.kylecorry.trail_sense.navigation.paths.ui.drawing

import android.graphics.Path
import android.graphics.PathDashPathEffect

class SquarePathEffect(size: Float = 6f, advance: Float = 3 * size, phase: Float = 0f) :
    PathDashPathEffect(
        getSquarePath(size), advance, phase, Style.ROTATE
    ) {

    companion object {
        private fun getSquarePath(size: Float): Path {
            val path = Path()
            path.addRect(-size / 2f, -size / 2f, size / 2f, size / 2f, Path.Direction.CW)
            path.close()
            return path
        }
    }
}