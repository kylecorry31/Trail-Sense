package com.kylecorry.trail_sense.tools.paths.ui.drawing

import com.kylecorry.sol.units.Coordinate

interface IRenderedPathFactory {
    fun render(points: List<Coordinate>, line: MutableList<Float> = mutableListOf()): RenderedPath
}