package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.trail_sense.navigation.paths.infrastructure.BacktrackIsEnabled
import com.kylecorry.trail_sense.weather.infrastructure.WeatherMonitorIsEnabled

class IsBatteryExemptionRequired(
    private val isBatteryUsageRestricted: Specification<Context> = IsBatteryUsageRestricted(),
    private val areBackgroundServicesRequired: Specification<Context> = backgroundRequired
) : Specification<Context>() {

    override fun isSatisfiedBy(value: Context): Boolean {
        if (!isBatteryUsageRestricted.isSatisfiedBy(value)) {
            return false
        }
        return areBackgroundServicesRequired.isSatisfiedBy(value)
    }

    companion object {

        private val backtrack = BacktrackIsEnabled()
        private val weather = WeatherMonitorIsEnabled()
        private val backgroundRequired = backtrack.or(weather)

    }
}