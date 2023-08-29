package com.kylecorry.trail_sense.navigation.domain.hiking

import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint

interface HikingDifficultyCalculator {

    fun calculate(points: List<PathPoint>): HikingDifficulty

}