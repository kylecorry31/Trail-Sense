package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.trail_sense.navigation.paths.infrastructure.BacktrackIsEnabled
import com.kylecorry.trail_sense.navigation.paths.infrastructure.BacktrackRequiresForeground
import com.kylecorry.trail_sense.weather.infrastructure.WeatherMonitorIsEnabled
import com.kylecorry.trail_sense.weather.infrastructure.WeatherMonitorRequiresForeground

class IsPersistentForegroundRequired(
    private val areForegroundServicesRestricted: Specification<Context> = AreForegroundServicesRestricted(),
    private val areForegroundServicesRequired: Specification<Context> = foregroundRequired
) : Specification<Context>() {

    override fun isSatisfiedBy(value: Context): Boolean {
        if (!areForegroundServicesRestricted.isSatisfiedBy(value)) {
            return false
        }
        return areForegroundServicesRequired.isSatisfiedBy(value)
    }

    companion object {

        private val backtrack = BacktrackIsEnabled().and(BacktrackRequiresForeground())
        private val weather = WeatherMonitorIsEnabled().and(WeatherMonitorRequiresForeground())
        private val foregroundRequired = backtrack.or(weather)

    }
}