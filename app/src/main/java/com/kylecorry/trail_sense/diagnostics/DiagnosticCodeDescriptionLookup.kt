package com.kylecorry.trail_sense.diagnostics

import android.content.Context
import com.kylecorry.trail_sense.R

class DiagnosticCodeDescriptionLookup(private val context: Context) {

    fun getDescription(code: DiagnosticCode): String {
        return when (code) {
            DiagnosticCode.AltitudeOverridden -> context.getString(R.string.overridden)
            DiagnosticCode.LocationOverridden -> context.getString(R.string.overridden)
            DiagnosticCode.LocationUnset -> context.getString(R.string.location_not_set)
            DiagnosticCode.PowerSavingMode -> context.getString(R.string.on)
            DiagnosticCode.BatteryHealthPoor -> context.getString(R.string.quality_poor)
            DiagnosticCode.BatteryUsageRestricted -> context.getString(R.string.battery_usage_restricted)
            DiagnosticCode.CameraUnavailable -> context.getString(R.string.unavailable)
            DiagnosticCode.BarometerUnavailable -> context.getString(R.string.unavailable)
            DiagnosticCode.MagnetometerUnavailable -> context.getString(R.string.unavailable)
            DiagnosticCode.AccelerometerUnavailable -> context.getString(R.string.unavailable)
            DiagnosticCode.LightSensorUnavailable -> context.getString(R.string.unavailable)
            DiagnosticCode.GPSUnavailable -> context.getString(R.string.unavailable)
            DiagnosticCode.FlashlightUnavailable -> context.getString(R.string.unavailable)
            DiagnosticCode.PedometerUnavailable -> context.getString(R.string.unavailable)
            DiagnosticCode.CameraNoPermission -> context.getString(R.string.no_permission)
            DiagnosticCode.LocationNoPermission -> context.getString(R.string.no_permission)
            DiagnosticCode.BackgroundLocationNoPermission -> context.getString(R.string.no_permission)
            DiagnosticCode.PedometerNoPermission -> context.getString(R.string.no_permission)
            DiagnosticCode.BarometerPoor -> context.getString(R.string.quality_poor)
            DiagnosticCode.MagnetometerPoor -> context.getString(R.string.quality_poor)
            DiagnosticCode.AccelerometerPoor -> context.getString(R.string.quality_poor)
            DiagnosticCode.GPSPoor -> context.getString(R.string.quality_poor)
            DiagnosticCode.GPSTimedOut -> context.getString(R.string.gps_signal_lost)
            DiagnosticCode.SunsetAlertsBlocked -> context.getString(R.string.notifications_blocked)
            DiagnosticCode.StormAlertsBlocked -> context.getString(R.string.notifications_blocked)
            DiagnosticCode.DailyForecastNotificationsBlocked -> context.getString(R.string.notifications_blocked)
            DiagnosticCode.FlashlightNotificationsBlocked -> context.getString(R.string.notifications_blocked)
            DiagnosticCode.PedometerNotificationsBlocked -> context.getString(R.string.notifications_blocked)
            DiagnosticCode.WeatherNotificationsBlocked -> context.getString(R.string.notifications_blocked)
            DiagnosticCode.WeatherMonitorDisabled -> context.getString(R.string.weather_monitoring_disabled)
            DiagnosticCode.ExactAlarmNoPermission -> context.getString(R.string.no_permission)
        }
    }

}