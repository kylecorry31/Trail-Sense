package com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.temp

import com.kylecorry.sol.math.Vector2

interface ITimeSeriesPredictor {
    fun predictNext(samples: List<Vector2>, n: Int, step: Float? = null): List<Vector2>
}