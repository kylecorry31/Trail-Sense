package com.kylecorry.trail_sense.diagnostics

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.navigation.IAppNavigation
import com.kylecorry.trail_sense.shared.permissions.RemoveBatteryRestrictionsCommand

class DiagnosticAlertService(private val context: Context, private val navigation: IAppNavigation) :
    IDiagnosticAlertService {

    private val titleLookup = DiagnosticCodeTitleLookup(context)
    private val descriptionLookup = DiagnosticCodeDescriptionLookup(context)

    override fun alert(code: DiagnosticCode) {
        val action = getAction(code)
        val affectedTools = getAffectedTools(code).joinToString("\n") { "- $it" }

        val message = context.getString(
            R.string.diagnostic_message_template,
            getSeverityName(code.severity),
            descriptionLookup.getDescription(code),
            affectedTools,
            getResolution(code)
        )

        if (action != null) {
            Alerts.dialog(
                context,
                titleLookup.getTitle(code),
                MarkdownService(context).toMarkdown(message),
                okText = action.title
            ) { cancelled ->
                if (!cancelled) {
                    action.action.invoke()
                }
            }
        } else {
            Alerts.dialog(
                context,
                titleLookup.getTitle(code),
                MarkdownService(context).toMarkdown(message),
                cancelText = null
            )
        }
    }


    private fun getSeverityName(status: Severity): String {
        return when (status) {
            Severity.Error -> getString(R.string.error)
            Severity.Warning -> getString(R.string.warning)
        }
    }

    private fun getAffectedTools(code: DiagnosticCode): List<String> {
        val weather = getString(R.string.weather)
        val navigation = getString(R.string.navigation)
        val backtrack = getString(R.string.backtrack)
        val astronomy = getString(R.string.astronomy)
        val speedometer = getString(R.string.speedometer)
        val pedometer = getString(R.string.pedometer)
        val waterBoil = getString(R.string.water_boil_timer_title)
        val clinometer = getString(R.string.clinometer_title)
        val level = getString(R.string.tool_bubble_level_title)
        val solar = getString(R.string.tool_solar_panel_title)
        val lightMeter = getString(R.string.tool_light_meter_title)
        val metalDetector = getString(R.string.tool_metal_detector_title)
        val sightingCompass = getString(R.string.sighting_compass)
        val flashlight = getString(R.string.flashlight_title)
        val clock = context.getString(R.string.tool_clock_title)

        val locationAffectedTools = listOf(
            navigation,
            backtrack,
            astronomy,
            speedometer,
            pedometer,
            solar
        )

        val accelAffectedTools = listOf(
            navigation,
            clinometer,
            level,
            solar
        )

        return when (code) {
            DiagnosticCode.AltitudeOverridden -> listOf(weather, waterBoil)
            DiagnosticCode.LocationOverridden -> locationAffectedTools
            DiagnosticCode.LocationUnset -> locationAffectedTools
            DiagnosticCode.PowerSavingMode -> listOf(backtrack, pedometer)
            DiagnosticCode.BatteryHealthPoor -> listOf()
            DiagnosticCode.BatteryUsageRestricted -> listOf(
                backtrack,
                astronomy,
                pedometer,
                weather
            )
            DiagnosticCode.CameraUnavailable -> listOf(navigation, sightingCompass)
            DiagnosticCode.BarometerUnavailable -> listOf(weather)
            DiagnosticCode.MagnetometerUnavailable -> listOf(navigation, metalDetector, solar)
            DiagnosticCode.AccelerometerUnavailable -> accelAffectedTools
            DiagnosticCode.GPSUnavailable -> locationAffectedTools
            DiagnosticCode.FlashlightUnavailable -> listOf(flashlight)
            DiagnosticCode.PedometerUnavailable -> listOf(pedometer)
            DiagnosticCode.CameraNoPermission -> listOf(navigation, sightingCompass)
            DiagnosticCode.LocationNoPermission -> locationAffectedTools
            DiagnosticCode.BackgroundLocationNoPermission -> listOf(
                backtrack,
                astronomy,
                speedometer,
                pedometer
            )
            DiagnosticCode.PedometerNoPermission -> listOf(pedometer)
            DiagnosticCode.BarometerPoor -> listOf(weather)
            DiagnosticCode.MagnetometerPoor -> listOf(navigation, metalDetector, solar)
            DiagnosticCode.AccelerometerPoor -> accelAffectedTools
            DiagnosticCode.GPSPoor -> locationAffectedTools
            DiagnosticCode.GPSTimedOut -> locationAffectedTools
            DiagnosticCode.SunsetAlertsBlocked -> listOf(astronomy)
            DiagnosticCode.StormAlertsBlocked -> listOf(weather)
            DiagnosticCode.DailyForecastNotificationsBlocked -> listOf(weather)
            DiagnosticCode.FlashlightNotificationsBlocked -> listOf(flashlight)
            DiagnosticCode.PedometerNotificationsBlocked -> listOf(pedometer)
            DiagnosticCode.WeatherNotificationsBlocked -> listOf(weather)
            DiagnosticCode.LightSensorUnavailable -> listOf(lightMeter)
            DiagnosticCode.WeatherMonitorDisabled -> listOf(weather)
            DiagnosticCode.ExactAlarmNoPermission -> listOf(astronomy, clock)
        }
    }

    private fun getResolution(code: DiagnosticCode): String {
        return when (code) {
            DiagnosticCode.AltitudeOverridden -> getString(R.string.altitude_override_resolution)
            DiagnosticCode.LocationOverridden -> getString(R.string.location_override_resolution)
            DiagnosticCode.LocationUnset -> getString(R.string.location_override_not_set_resolution)
            DiagnosticCode.PowerSavingMode -> getString(R.string.power_saving_mode_resolution)
            DiagnosticCode.BatteryHealthPoor -> getString(R.string.no_resolution)
            DiagnosticCode.BatteryUsageRestricted -> getString(R.string.battery_restricted_resolution)
            DiagnosticCode.CameraUnavailable -> getString(R.string.no_resolution)
            DiagnosticCode.BarometerUnavailable -> getString(R.string.no_resolution)
            DiagnosticCode.MagnetometerUnavailable -> getString(R.string.no_resolution)
            DiagnosticCode.AccelerometerUnavailable -> getString(R.string.no_resolution)
            DiagnosticCode.GPSUnavailable -> getString(R.string.gps_unavailable_resolution)
            DiagnosticCode.FlashlightUnavailable -> getString(R.string.no_resolution)
            DiagnosticCode.PedometerUnavailable -> getString(R.string.no_resolution)
            DiagnosticCode.CameraNoPermission -> context.getString(
                R.string.grant_permission,
                getString(R.string.camera)
            )
            DiagnosticCode.LocationNoPermission -> context.getString(
                R.string.grant_permission,
                getString(R.string.location)
            )
            DiagnosticCode.BackgroundLocationNoPermission -> context.getString(
                R.string.grant_permission, getString(
                    R.string.background_location_permission
                )
            )
            DiagnosticCode.PedometerNoPermission -> context.getString(
                R.string.grant_permission,
                getString(R.string.activity_recognition)
            )
            DiagnosticCode.BarometerPoor -> getString(R.string.no_resolution)
            DiagnosticCode.MagnetometerPoor -> context.getString(
                R.string.calibrate_compass_dialog_content,
                getString(android.R.string.ok)
            )
            DiagnosticCode.AccelerometerPoor -> getString(R.string.no_resolution)
            DiagnosticCode.GPSPoor -> getString(R.string.get_gps_signal)
            DiagnosticCode.GPSTimedOut -> getString(R.string.get_gps_signal)
            DiagnosticCode.SunsetAlertsBlocked -> context.getString(
                R.string.unblock_notification_channel,
                getString(R.string.sunset_alert_channel_title)
            )
            DiagnosticCode.StormAlertsBlocked -> context.getString(
                R.string.unblock_notification_channel,
                getString(R.string.alerts)
            )
            DiagnosticCode.DailyForecastNotificationsBlocked -> context.getString(
                R.string.unblock_notification_channel,
                getString(R.string.todays_forecast)
            )
            DiagnosticCode.FlashlightNotificationsBlocked -> context.getString(
                R.string.unblock_notification_channel,
                getString(R.string.flashlight_title)
            )
            DiagnosticCode.PedometerNotificationsBlocked -> context.getString(
                R.string.unblock_notification_channel,
                getString(R.string.odometer)
            )
            DiagnosticCode.WeatherNotificationsBlocked -> context.getString(
                R.string.unblock_notification_channel,
                getString(R.string.weather)
            )
            DiagnosticCode.LightSensorUnavailable -> getString(R.string.no_resolution)
            DiagnosticCode.WeatherMonitorDisabled -> getString(R.string.weather_monitor_disabled_resolution)
            DiagnosticCode.ExactAlarmNoPermission -> context.getString(
                R.string.grant_permission,
                getString(R.string.permission_alarms_and_reminders)
            )
        }
    }

    private fun getAction(code: DiagnosticCode): Action? {
        return when (code) {
            DiagnosticCode.AltitudeOverridden -> navigateAction(R.id.calibrateAltimeterFragment)
            DiagnosticCode.LocationOverridden -> navigateAction(R.id.calibrateGPSFragment)
            DiagnosticCode.LocationUnset -> navigateAction(R.id.calibrateGPSFragment)
            DiagnosticCode.PowerSavingMode -> navigateAction(R.id.powerSettingsFragment)
            DiagnosticCode.BatteryHealthPoor -> null
            DiagnosticCode.BatteryUsageRestricted -> commandAction(RemoveBatteryRestrictionsCommand(context))
            DiagnosticCode.CameraUnavailable -> null
            DiagnosticCode.BarometerUnavailable -> null
            DiagnosticCode.MagnetometerUnavailable -> null
            DiagnosticCode.AccelerometerUnavailable -> null
            DiagnosticCode.GPSUnavailable -> locationSourcesAction()
            DiagnosticCode.FlashlightUnavailable -> null
            DiagnosticCode.PedometerUnavailable -> null
            DiagnosticCode.CameraNoPermission -> permissionAction()
            DiagnosticCode.LocationNoPermission -> permissionAction()
            DiagnosticCode.BackgroundLocationNoPermission -> permissionAction()
            DiagnosticCode.PedometerNoPermission -> permissionAction()
            DiagnosticCode.BarometerPoor -> null
            DiagnosticCode.MagnetometerPoor -> null
            DiagnosticCode.AccelerometerPoor -> null
            DiagnosticCode.GPSPoor -> null
            DiagnosticCode.GPSTimedOut -> null
            DiagnosticCode.SunsetAlertsBlocked -> notificationAction()
            DiagnosticCode.StormAlertsBlocked -> notificationAction()
            DiagnosticCode.DailyForecastNotificationsBlocked -> notificationAction()
            DiagnosticCode.FlashlightNotificationsBlocked -> notificationAction()
            DiagnosticCode.PedometerNotificationsBlocked -> notificationAction()
            DiagnosticCode.WeatherNotificationsBlocked -> notificationAction()
            DiagnosticCode.LightSensorUnavailable -> null
            DiagnosticCode.WeatherMonitorDisabled -> navigateAction(R.id.weatherSettingsFragment)
            DiagnosticCode.ExactAlarmNoPermission -> alarmAndReminderAction()
        }
    }

    private fun alarmAndReminderAction(): Action? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            intentAction(Intents.alarmAndReminderSettings(context))
        } else {
            null
        }
    }

    private fun locationSourcesAction(): Action {
        return Action(getString(R.string.settings)) {
            try {
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            } catch (e: Exception) {
                context.startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    private fun notificationAction(): Action {
        return intentAction(Intents.appSettings(context))
    }

    private fun permissionAction(): Action {
        return intentAction(Intents.appSettings(context))
    }

    private fun intentAction(to: Intent, title: String = getString(R.string.settings)): Action {
        return Action(title) {
            context.startActivity(to)
        }
    }

    private fun commandAction(command: Command, title: String = getString(R.string.settings)): Action {
        return Action(title) {
            command.execute()
        }
    }

    private fun navigateAction(
        @IdRes to: Int,
        title: String = getString(R.string.settings)
    ): Action {
        return Action(title) {
            navigation.navigate(to)
        }
    }

    data class Action(val title: String, val action: () -> Unit)

    private fun getString(@StringRes id: Int): String {
        return context.getString(id)
    }


}