package com.kylecorry.trail_sense.navigation.paths.domain

typealias PathLine = Pair<PathPoint, PathPoint>

fun List<PathPoint>.lines(): List<PathLine> {
    return zipWithNext()
}