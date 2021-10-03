package com.kylecorry.trail_sense.weather.infrastructure.clouds

import com.kylecorry.sol.science.meteorology.clouds.CloudType

data class CloudObservation(
    val cover: Float,
    val luminance: Float,
    val possibleClouds: List<CloudType>
)