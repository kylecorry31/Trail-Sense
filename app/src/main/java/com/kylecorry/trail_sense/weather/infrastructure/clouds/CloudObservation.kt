package com.kylecorry.trail_sense.weather.infrastructure.clouds

import com.kylecorry.sol.science.meteorology.clouds.CloudGenus

data class CloudObservation(
    val cover: Float,
    val luminance: Float,
    val contrast: Float,
    val energy: Float,
    val entropy: Float,
    val homogeneity: Float,
    val possibleClouds: List<Pair<CloudGenus, Float>>
)