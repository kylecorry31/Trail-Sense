package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import android.os.Build
import com.kylecorry.luna.specifications.Specification
import com.kylecorry.trail_sense.tools.astronomy.AstronomyToolRegistration
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.pedometer.PedometerToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.turn_back.TurnBackToolRegistration
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration

class IsNotificationPermissionRequired : Specification<Context>() {

    override fun isSatisfiedBy(value: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false
        }
        return servicesWithNotifications.any { isServiceEnabled(value, it) }
    }

    private fun isServiceEnabled(context: Context, serviceId: String): Boolean {
        val service = Tools.getService(context, serviceId) ?: return false
        return service.isEnabled() && !service.isBlocked()
    }

    companion object {
        private val servicesWithNotifications = listOf(
            PathsToolRegistration.SERVICE_BACKTRACK,
            WeatherToolRegistration.SERVICE_WEATHER_MONITOR,
            PedometerToolRegistration.SERVICE_PEDOMETER,
            AstronomyToolRegistration.SERVICE_SUNSET_ALERTS,
            AstronomyToolRegistration.SERVICE_SUNRISE_ALERTS,
            AstronomyToolRegistration.SERVICE_ASTRONOMY_ALERTS,
            TurnBackToolRegistration.SERVICE_TURN_BACK
        )
    }
}
