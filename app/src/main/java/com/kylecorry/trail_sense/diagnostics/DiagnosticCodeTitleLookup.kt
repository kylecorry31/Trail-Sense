package com.kylecorry.trail_sense.diagnostics

import android.content.Context
import com.kylecorry.trail_sense.R

class DiagnosticCodeTitleLookup(private val context: Context) {
    fun getTitle(code: DiagnosticCode): String {
        return when (code) {
            DiagnosticCode.AltitudeOverridden -> context.getString(R.string.altitude)
            DiagnosticCode.LocationOverridden -> context.getString(R.string.gps)
            DiagnosticCode.LocationUnset -> context.getString(R.string.gps)
            DiagnosticCode.PowerSavingMode -> context.getString(R.string.pref_low_power_mode_title)
            DiagnosticCode.BatteryHealthPoor -> context.getString(R.string.tool_battery_title)
            DiagnosticCode.BatteryUsageRestricted -> context.getString(R.string.tool_battery_title)
            DiagnosticCode.CameraUnavailable -> context.getString(R.string.camera)
            DiagnosticCode.BarometerUnavailable -> context.getString(R.string.barometer)
            DiagnosticCode.MagnetometerUnavailable -> context.getString(R.string.pref_compass_sensor_title)
            DiagnosticCode.AccelerometerUnavailable -> context.getString(R.string.gravity)
            DiagnosticCode.GPSUnavailable -> context.getString(R.string.gps)
            DiagnosticCode.FlashlightUnavailable -> context.getString(R.string.flashlight_title)
            DiagnosticCode.PedometerUnavailable -> context.getString(R.string.pedometer)
            DiagnosticCode.CameraNoPermission -> context.getString(R.string.camera)
            DiagnosticCode.LocationNoPermission -> context.getString(R.string.gps)
            DiagnosticCode.BackgroundLocationNoPermission -> context.getString(R.string.gps)
            DiagnosticCode.PedometerNoPermission -> context.getString(R.string.pedometer)
            DiagnosticCode.BarometerPoor -> context.getString(R.string.barometer)
            DiagnosticCode.MagnetometerPoor -> context.getString(R.string.pref_compass_sensor_title)
            DiagnosticCode.AccelerometerPoor -> context.getString(R.string.gravity)
            DiagnosticCode.GPSPoor -> context.getString(R.string.gps)
            DiagnosticCode.GPSTimedOut -> context.getString(R.string.gps)
            DiagnosticCode.SunsetAlertsBlocked -> context.getString(R.string.sunset_alerts)
            DiagnosticCode.StormAlertsBlocked -> context.getString(R.string.storm_alerts)
            DiagnosticCode.DailyForecastNotificationsBlocked -> context.getString(R.string.todays_forecast)
            DiagnosticCode.FlashlightNotificationsBlocked -> context.getString(R.string.flashlight_title)
            DiagnosticCode.PedometerNotificationsBlocked -> context.getString(R.string.pedometer)
            DiagnosticCode.WeatherNotificationsBlocked -> context.getString(R.string.weather)
            DiagnosticCode.LightSensorUnavailable -> context.getString(R.string.tool_light_meter_title)
            DiagnosticCode.WeatherMonitorDisabled -> context.getString(R.string.weather)
        }
    }
}