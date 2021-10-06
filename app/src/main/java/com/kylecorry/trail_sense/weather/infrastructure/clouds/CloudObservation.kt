package com.kylecorry.trail_sense.weather.infrastructure.clouds

import com.kylecorry.sol.science.meteorology.clouds.CloudGenus

data class CloudObservation(
    val cover: Float,
    val contrast: Float,
    val energy: Float,
    val entropy: Float,
    val homogeneity: Float,
    val redMean: Float,
    val blueMean: Float,
    val redGreenDiff: Float,
    val redBlueDiff: Float,
    val greenBlueDiff: Float,
    val blueStdev: Float,
    val possibleClouds: List<Pair<CloudGenus, Float>>
)