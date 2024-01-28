package com.kylecorry.trail_sense.tools.paths.infrastructure.persistence

import com.kylecorry.trail_sense.tools.paths.domain.PathStyle
import java.time.Duration

interface IPathPreferences {
    val defaultPathStyle: PathStyle
    val backtrackHistory: Duration
    val simplifyPathOnImport: Boolean
    val onlyNavigateToPoints: Boolean
    val useFastPathRendering: Boolean
}