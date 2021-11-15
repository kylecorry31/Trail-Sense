package com.kylecorry.trail_sense.navigation.domain.hiking

import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Speed
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import java.time.Duration

interface IHikingService {
    fun getHikingDifficulty(
        points: List<PathPoint>,
        gainThreshold: Distance = Distance.meters(0f)
    ): HikingDifficulty

    fun getAveragePace(difficulty: HikingDifficulty, factor: Float = 2f): Speed

    fun getHikingDuration(
        path: List<PathPoint>,
        gainThreshold: Distance = Distance.meters(0f),
        pace: Speed
    ): Duration

    fun getHikingDuration(
        path: List<PathPoint>,
        gainThreshold: Distance = Distance.meters(0f),
        paceFactor: Float = 2f
    ): Duration
}