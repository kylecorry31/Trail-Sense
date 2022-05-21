package com.kylecorry.trail_sense.navigation.paths.ui.drawing

import android.graphics.Path
import com.kylecorry.sol.units.Coordinate

interface IRenderedPathFactory {
    fun render(points: List<Coordinate>, path: Path = Path()): RenderedPath
}