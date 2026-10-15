package com.kylecorry.trail_sense.tools.paths.domain.hiking

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Speed
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import java.time.Duration

interface IHikingService {

    fun getDistances(points: List<Coordinate>, minDistance: Float = 0f): List<Float>

    /**
     * Smooths the elevations of the path to remove noise
     * @param points The path points in order
     * @param smoothing How much to smooth the elevations, from 0 (none) to 1 (the most)
     */
    fun correctElevations(
        points: List<PathPoint>,
        smoothing: Float = HikingService.DEFAULT_ELEVATION_SMOOTHING
    ): List<PathPoint>

    fun getHikingDifficulty(points: List<PathPoint>): HikingDifficulty

    fun getAveragePace(difficulty: HikingDifficulty, factor: Float = 2f): Speed

    fun getHikingDuration(
        path: List<PathPoint>,
        pace: Speed
    ): Duration

    fun getHikingDuration(
        path: List<PathPoint>,
        paceFactor: Float = 2f,
        difficulty: HikingDifficulty? = null
    ): Duration

    fun getElevationLossGain(path: List<PathPoint>): Pair<Distance, Distance>

    fun getElevationGain(path: List<PathPoint>): Distance

    fun getSlopes(path: List<PathPoint>): List<Triple<PathPoint, PathPoint, Float>>
}
