package com.kylecorry.trail_sense.weather.infrastructure.clouds

import com.kylecorry.sol.science.meteorology.clouds.CloudGenus

data class CloudObservation(
    val cover: Float,
    val luminance: Float,
    val contrast: Float,
    val possibleClouds: List<CloudGenus>
)