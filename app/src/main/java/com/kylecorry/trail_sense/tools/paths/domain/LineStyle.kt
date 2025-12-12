package com.kylecorry.trail_sense.tools.paths.domain

enum class LineStyle(val id: Int, val stringId: String) {
    Solid(1, "solid"),
    Dotted(2, "dotted"),
    Arrow(3, "arrow"),
    Dashed(4, "dashed"),
    Square(5, "square"),
    Diamond(6, "diamond"),
    Cross(7, "cross")
}