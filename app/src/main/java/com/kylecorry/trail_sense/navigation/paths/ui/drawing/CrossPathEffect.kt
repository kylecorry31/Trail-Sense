package com.kylecorry.trail_sense.navigation.paths.ui.drawing

import android.graphics.Path
import android.graphics.PathDashPathEffect

class CrossPathEffect(size: Float = 6f, advance: Float = 3 * size, phase: Float = 0f) :
    PathDashPathEffect(
        getCrossPath(size), advance, phase, Style.ROTATE
    ) {

    companion object {
        private fun getCrossPath(size: Float): Path {
            val path = Path()

            val thickness = size / 8
            val rotation = 0.70710678119f
            val offset = thickness * rotation

            // Bottom to top
            path.moveTo(-size / 2 + offset, -size / 2 - offset)
            path.lineTo(-size / 2 - offset, -size / 2 + offset)
            path.lineTo(size / 2 - offset, size / 2 + offset)
            path.lineTo(size / 2 + offset, size / 2 - offset)
            path.close()

            // Top to bottom
            path.moveTo(-size / 2 - offset, size / 2 - offset)
            path.lineTo(-size / 2 + offset, size / 2 + offset)
            path.lineTo(size / 2 + offset, -size / 2 + offset)
            path.lineTo(size / 2 - offset, -size / 2 - offset)
            path.close()

            return path
        }
    }
}