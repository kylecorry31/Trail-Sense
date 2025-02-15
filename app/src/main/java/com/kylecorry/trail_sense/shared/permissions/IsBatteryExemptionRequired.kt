package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration

class IsBatteryExemptionRequired(
    private val isBatteryUsageRestricted: Specification<Context> = IsBatteryUsageRestricted(),
) : Specification<Context>() {

    override fun isSatisfiedBy(value: Context): Boolean {
        if (!isBatteryUsageRestricted.isSatisfiedBy(value)) {
            return false
        }
        return isServiceEnabled(value, WeatherToolRegistration.SERVICE_WEATHER_MONITOR) ||
                isServiceEnabled(value, PathsToolRegistration.SERVICE_BACKTRACK)
    }

    private fun isServiceEnabled(context: Context, serviceId: String): Boolean {
        val service = Tools.getService(context, serviceId) ?: return false
        return service.isEnabled() && !service.isBlocked()
    }
}