package com.kylecorry.trail_sense.tools.paths.ui.drawing

import com.kylecorry.trail_sense.tools.paths.domain.LineStyle

class PathLineDrawerFactory {

    private val cache = mutableMapOf<LineStyle, IPathLineDrawerStrategy>()


    fun create(style: LineStyle): IPathLineDrawerStrategy {
        return cache.getOrPut(style) {
            when (style) {
                LineStyle.Solid -> SolidPathLineDrawerStrategy()
                LineStyle.Dotted -> DottedPathLineDrawerStrategy()
                LineStyle.Arrow -> ArrowPathLineDrawerStrategy()
                LineStyle.Dashed -> DashedPathLineDrawerStrategy()
                LineStyle.Square -> SquarePathLineDrawerStrategy()
                LineStyle.Diamond -> DiamondPathLineDrawerStrategy()
                LineStyle.Cross -> CrossPathLineDrawerStrategy()
            }
        }
    }

}