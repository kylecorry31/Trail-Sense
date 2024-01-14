package com.kylecorry.trail_sense.navigation.paths.ui.drawing

import com.kylecorry.sol.units.Coordinate

interface IRenderedPathFactory {
    fun render(points: List<Coordinate>, line: MutableList<Float> = mutableListOf()): RenderedPath
}