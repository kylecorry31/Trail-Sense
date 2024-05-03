package com.kylecorry.trail_sense.tools.diagnostics.infrastructure

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.permissions.SpecialPermission
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.alerts.MissingSensorAlert
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.navigation.IAppNavigation
import com.kylecorry.trail_sense.shared.permissions.RemoveBatteryRestrictionsCommand
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.commands.SunsetAlarmCommand
import com.kylecorry.trail_sense.tools.diagnostics.domain.DiagnosticCode
import com.kylecorry.trail_sense.tools.diagnostics.domain.Severity
import com.kylecorry.trail_sense.tools.diagnostics.ui.DiagnosticCodeDescriptionLookup
import com.kylecorry.trail_sense.tools.diagnostics.ui.DiagnosticCodeTitleLookup
import com.kylecorry.trail_sense.tools.diagnostics.ui.IDiagnosticAlertService
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightService
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService
import com.kylecorry.trail_sense.tools.tools.ui.items.DiagnosticItem
import com.kylecorry.trail_sense.tools.weather.infrastructure.alerts.CurrentWeatherAlerter
import com.kylecorry.trail_sense.tools.weather.infrastructure.alerts.DailyWeatherAlerter
import com.kylecorry.trail_sense.tools.weather.infrastructure.alerts.StormAlerter

class DiagnosticAlertService(private val context: Context, private val navigation: IAppNavigation) :
    IDiagnosticAlertService {

    private val titleLookup = DiagnosticCodeTitleLookup(context)
    private val descriptionLookup = DiagnosticCodeDescriptionLookup(context)

    override fun alert(item: DiagnosticItem) {
        val action = getAction(item.code)
        val affectedTools = item.tools.sortedBy { it.name }.joinToString("\n") { "- ${it.name}" }

        val message = context.getString(
            R.string.diagnostic_message_template,
            getSeverityName(item.code.severity),
            descriptionLookup.getDescription(item.code),
            affectedTools,
            getResolution(item.code)
        )

        if (action != null) {
            Alerts.dialog(
                context,
                titleLookup.getTitle(item.code),
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
                titleLookup.getTitle(item.code),
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

    private fun getResolution(code: DiagnosticCode): CharSequence {
        return when (code) {
            DiagnosticCode.AltitudeOverridden -> getString(R.string.elevation_override_resolution)
            DiagnosticCode.LocationOverridden -> getString(R.string.location_override_resolution)
            DiagnosticCode.LocationUnset -> getString(R.string.location_override_not_set_resolution)
            DiagnosticCode.PowerSavingMode -> getString(R.string.power_saving_mode_resolution)
            DiagnosticCode.BatteryHealthPoor -> getString(R.string.no_resolution)
            DiagnosticCode.BatteryUsageRestricted -> getString(R.string.battery_restricted_resolution)
            DiagnosticCode.CameraUnavailable -> getString(R.string.no_resolution)
            DiagnosticCode.BarometerUnavailable -> getString(R.string.no_resolution)
            DiagnosticCode.MagnetometerUnavailable -> MissingSensorAlert.getMissingSensorMessage(
                context,
                getString(R.string.pref_compass_sensor_title)
            )

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

            DiagnosticCode.GyroscopeUnavailable -> getString(R.string.no_resolution)
            DiagnosticCode.GyroscopePoor -> getString(R.string.no_resolution)
        }
    }

    private fun getAction(code: DiagnosticCode): Action? {
        return when (code) {
            DiagnosticCode.AltitudeOverridden -> navigateAction(R.id.calibrateAltimeterFragment)
            DiagnosticCode.LocationOverridden -> navigateAction(R.id.calibrateGPSFragment)
            DiagnosticCode.LocationUnset -> navigateAction(R.id.calibrateGPSFragment)
            DiagnosticCode.PowerSavingMode -> navigateAction(R.id.powerSettingsFragment)
            DiagnosticCode.BatteryHealthPoor -> null
            DiagnosticCode.BatteryUsageRestricted -> commandAction(
                RemoveBatteryRestrictionsCommand(
                    context
                )
            )

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
            DiagnosticCode.SunsetAlertsBlocked -> notificationAction(SunsetAlarmCommand.NOTIFICATION_CHANNEL_ID)
            DiagnosticCode.StormAlertsBlocked -> notificationAction(StormAlerter.STORM_CHANNEL_ID)
            DiagnosticCode.DailyForecastNotificationsBlocked -> notificationAction(
                DailyWeatherAlerter.DAILY_CHANNEL_ID
            )

            DiagnosticCode.FlashlightNotificationsBlocked -> notificationAction(FlashlightService.CHANNEL_ID)
            DiagnosticCode.PedometerNotificationsBlocked -> notificationAction(StepCounterService.CHANNEL_ID)
            DiagnosticCode.WeatherNotificationsBlocked -> notificationAction(CurrentWeatherAlerter.WEATHER_CHANNEL_ID)
            DiagnosticCode.LightSensorUnavailable -> null
            DiagnosticCode.WeatherMonitorDisabled -> navigateAction(R.id.weatherSettingsFragment)
            DiagnosticCode.ExactAlarmNoPermission -> alarmAndReminderAction()
            DiagnosticCode.GyroscopeUnavailable -> null
            DiagnosticCode.GyroscopePoor -> null
        }
    }

    private fun alarmAndReminderAction(): Action? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Action(getString(R.string.settings)) {
                Permissions.requestPermission(context, SpecialPermission.SCHEDULE_EXACT_ALARMS)
            }
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

    private fun notificationAction(channel: String? = null): Action {
        return intentAction(Intents.notificationSettings(context, channel))
    }

    private fun permissionAction(): Action {
        return intentAction(Intents.appSettings(context))
    }

    private fun intentAction(to: Intent, title: String = getString(R.string.settings)): Action {
        return Action(title) {
            context.startActivity(to)
        }
    }

    private fun commandAction(
        command: Command,
        title: String = getString(R.string.settings)
    ): Action {
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