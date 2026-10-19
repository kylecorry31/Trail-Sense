package com.kylecorry.trail_sense.tools.navigation.domain.paths

import com.kylecorry.trail_sense.tools.paths.domain.PathDirection

sealed interface PathFollow {
    data class Direction(val direction: PathDirection) : PathFollow
    data class ToPoint(val pointId: Long) : PathFollow
}
