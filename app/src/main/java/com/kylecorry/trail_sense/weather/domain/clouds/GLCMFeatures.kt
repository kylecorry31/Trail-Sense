package com.kylecorry.trail_sense.weather.domain.clouds

data class GLCMFeatures(
    val energy: Float,
    val entropy: Float,
    val contrast: Float,
    val homogeneity: Float
)
