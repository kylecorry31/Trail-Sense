package com.kylecorry.trail_sense.navigation.domain.hiking

import com.kylecorry.trail_sense.tools.paths.domain.PathPoint

interface HikingDifficultyCalculator {

    fun calculate(points: List<PathPoint>): HikingDifficulty

}