package com.kylecorry.trail_sense.tools.paths.infrastructure.persistence

import com.kylecorry.trail_sense.tools.paths.domain.PathStyle
import java.time.Duration

interface IPathPreferences {
    val defaultPathStyle: PathStyle
    val backtrackHistory: Duration
    val keepBacktrackPathOnStop: Boolean
    val simplifyPathOnImport: Boolean
    val useFastPathRendering: Boolean
    val elevationSmoothing: Float
}
