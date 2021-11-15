package com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence

import com.kylecorry.trail_sense.shared.paths.PathStyle
import java.time.Duration

interface IPathPreferences {
    val defaultPathStyle: PathStyle
    val backtrackHistory: Duration
}