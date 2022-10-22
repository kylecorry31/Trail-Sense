package com.kylecorry.trail_sense.weather.infrastructure.commands

import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.weather.domain.RawWeatherObservation

internal interface IWeatherObserver {
    suspend fun getWeatherObservation(): Reading<RawWeatherObservation>?
}