package com.kylecorry.trail_sense.diagnostics

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentDiagnosticsBinding
import com.kylecorry.trail_sense.databinding.ListItemPlainIconBinding
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.CustomUiUtils

class DiagnosticsFragment : BoundFragment<FragmentDiagnosticsBinding>() {

    private lateinit var diagnostics: List<IDiagnostic>
    private lateinit var diagnosticListView: ListView<DiagnosticCode>

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentDiagnosticsBinding {
        return FragmentDiagnosticsBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.sensorDetailsBtn.setOnClickListener {
            findNavController().navigate(R.id.sensorDetailsFragment)
        }
        diagnosticListView =
            ListView(binding.diagnosticsList, R.layout.list_item_plain_icon) { itemView, code ->
                val itemBinding = ListItemPlainIconBinding.bind(itemView)
                itemBinding.title.text = getCodeTitle(code)
                itemBinding.description.text = getCodeDescription(code)
                itemBinding.icon.setImageResource(R.drawable.ic_alert)
                CustomUiUtils.setImageColor(itemBinding.icon, getStatusTint(code.severity))
                itemBinding.root.setOnClickListener {
                    val action = getAction(code)
                    val affectedTools = getAffectedTools(code).joinToString("\n") { "- $it" }

                    val message = getString(
                        R.string.diagnostic_message_template,
                        getCodeDescription(code),
                        affectedTools,
                        getResolution(code)
                    )

                    if (action != null) {
                        Alerts.dialog(
                            requireContext(),
                            getCodeTitle(code),
                            MarkdownService(requireContext()).toMarkdown(message),
                            okText = action.title
                        ) { cancelled ->
                            if (!cancelled) {
                                action.action.invoke()
                            }
                        }
                    } else {
                        Alerts.dialog(
                            requireContext(),
                            getCodeTitle(code),
                            MarkdownService(requireContext()).toMarkdown(message),
                            cancelText = null
                        )
                    }
                }
            }
        diagnosticListView.addLineSeparator()
        diagnostics = listOfNotNull(
            AccelerometerDiagnostic(requireContext(), this),
            MagnetometerDiagnostic(requireContext(), this),
            GPSDiagnostic(requireContext(), this),
            BarometerDiagnostic(requireContext(), this),
            AltimeterDiagnostic(requireContext()),
            BatteryDiagnostic(requireContext(), this),
            LightSensorDiagnostic(requireContext(), this),
            CameraDiagnostic(requireContext()),
            FlashlightDiagnostic(requireContext()),
            PedometerDiagnostic(requireContext()),
            NotificationDiagnostic(requireContext())
        )
        scheduleUpdates(INTERVAL_1_FPS)
    }

    override fun onUpdate() {
        super.onUpdate()
        val results = diagnostics.flatMap { it.scan() }.toSet().sortedBy { it.severity.ordinal }
        binding.emptyText.isVisible = results.isEmpty()
        diagnosticListView.setData(results)
    }

    @ColorInt
    private fun getStatusTint(status: Severity): Int {
        return when (status) {
            Severity.Error -> AppColor.Red.color
            Severity.Warning -> AppColor.Yellow.color
        }
    }

    private fun getCodeDescription(code: DiagnosticCode): String {
        return when (code) {
            DiagnosticCode.AltitudeOverridden -> getString(R.string.overridden)
            DiagnosticCode.LocationOverridden -> getString(R.string.overridden)
            DiagnosticCode.LocationUnset -> getString(R.string.location_not_set)
            DiagnosticCode.PowerSavingMode -> getString(R.string.on)
            DiagnosticCode.BatteryHealthPoor -> getString(R.string.quality_poor)
            DiagnosticCode.BatteryUsageRestricted -> getString(R.string.battery_usage_restricted)
            DiagnosticCode.CameraUnavailable -> getString(R.string.unavailable)
            DiagnosticCode.BarometerUnavailable -> getString(R.string.unavailable)
            DiagnosticCode.MagnetometerUnavailable -> getString(R.string.unavailable)
            DiagnosticCode.AccelerometerUnavailable -> getString(R.string.unavailable)
            DiagnosticCode.LightSensorUnavailable -> getString(R.string.unavailable)
            DiagnosticCode.GPSUnavailable -> getString(R.string.unavailable)
            DiagnosticCode.FlashlightUnavailable -> getString(R.string.unavailable)
            DiagnosticCode.PedometerUnavailable -> getString(R.string.unavailable)
            DiagnosticCode.CameraNoPermission -> getString(R.string.no_permission)
            DiagnosticCode.LocationNoPermission -> getString(R.string.no_permission)
            DiagnosticCode.BackgroundLocationNoPermission -> getString(R.string.no_permission)
            DiagnosticCode.PedometerNoPermission -> getString(R.string.no_permission)
            DiagnosticCode.BarometerPoor -> getString(R.string.quality_poor)
            DiagnosticCode.MagnetometerPoor -> getString(R.string.quality_poor)
            DiagnosticCode.AccelerometerPoor -> getString(R.string.quality_poor)
            DiagnosticCode.GPSPoor -> getString(R.string.quality_poor)
            DiagnosticCode.GPSTimedOut -> getString(R.string.gps_signal_lost)
            DiagnosticCode.SunsetAlertsBlocked -> getString(R.string.notifications_blocked)
            DiagnosticCode.StormAlertsBlocked -> getString(R.string.notifications_blocked)
            DiagnosticCode.DailyForecastNotificationsBlocked -> getString(R.string.notifications_blocked)
            DiagnosticCode.FlashlightNotificationsBlocked -> getString(R.string.notifications_blocked)
            DiagnosticCode.PedometerNotificationsBlocked -> getString(R.string.notifications_blocked)
            DiagnosticCode.WeatherNotificationsBlocked -> getString(R.string.notifications_blocked)
        }
    }

    private fun getCodeTitle(code: DiagnosticCode): String {
        return when (code) {
            DiagnosticCode.AltitudeOverridden -> getString(R.string.altitude)
            DiagnosticCode.LocationOverridden -> getString(R.string.gps)
            DiagnosticCode.LocationUnset -> getString(R.string.gps)
            DiagnosticCode.PowerSavingMode -> getString(R.string.pref_low_power_mode_title)
            DiagnosticCode.BatteryHealthPoor -> getString(R.string.tool_battery_title)
            DiagnosticCode.BatteryUsageRestricted -> getString(R.string.tool_battery_title)
            DiagnosticCode.CameraUnavailable -> getString(R.string.camera)
            DiagnosticCode.BarometerUnavailable -> getString(R.string.barometer)
            DiagnosticCode.MagnetometerUnavailable -> getString(R.string.pref_compass_sensor_title)
            DiagnosticCode.AccelerometerUnavailable -> getString(R.string.gravity)
            DiagnosticCode.GPSUnavailable -> getString(R.string.gps)
            DiagnosticCode.FlashlightUnavailable -> getString(R.string.flashlight_title)
            DiagnosticCode.PedometerUnavailable -> getString(R.string.pedometer)
            DiagnosticCode.CameraNoPermission -> getString(R.string.camera)
            DiagnosticCode.LocationNoPermission -> getString(R.string.gps)
            DiagnosticCode.BackgroundLocationNoPermission -> getString(R.string.gps)
            DiagnosticCode.PedometerNoPermission -> getString(R.string.pedometer)
            DiagnosticCode.BarometerPoor -> getString(R.string.barometer)
            DiagnosticCode.MagnetometerPoor -> getString(R.string.pref_compass_sensor_title)
            DiagnosticCode.AccelerometerPoor -> getString(R.string.gravity)
            DiagnosticCode.GPSPoor -> getString(R.string.gps)
            DiagnosticCode.GPSTimedOut -> getString(R.string.gps)
            DiagnosticCode.SunsetAlertsBlocked -> getString(R.string.sunset_alerts)
            DiagnosticCode.StormAlertsBlocked -> getString(R.string.storm_alerts)
            DiagnosticCode.DailyForecastNotificationsBlocked -> getString(R.string.todays_forecast)
            DiagnosticCode.FlashlightNotificationsBlocked -> getString(R.string.flashlight_title)
            DiagnosticCode.PedometerNotificationsBlocked -> getString(R.string.pedometer)
            DiagnosticCode.WeatherNotificationsBlocked -> getString(R.string.weather)
            DiagnosticCode.LightSensorUnavailable -> getString(R.string.tool_light_meter_title)
        }
    }

    private fun getAffectedTools(code: DiagnosticCode): List<String> {
        val weather = getString(R.string.weather)
        val navigation = getString(R.string.navigation)
        val backtrack = getString(R.string.backtrack)
        val astronomy = getString(R.string.astronomy)
        val speedometer = getString(R.string.speedometer)
        val odometer = getString(R.string.odometer)
        val waterBoil = getString(R.string.water_boil_timer_title)
        val inclinometer = getString(R.string.inclinometer_title)
        val level = getString(R.string.tool_bubble_level_title)
        val solar = getString(R.string.tool_solar_panel_title)
        val lightMeter = getString(R.string.tool_light_meter_title)
        val metalDetector = getString(R.string.tool_metal_detector_title)
        val sightingCompass = getString(R.string.sighting_compass)
        val flashlight = getString(R.string.flashlight_title)

        val locationAffectedTools = listOf(
            navigation,
            backtrack,
            astronomy,
            speedometer,
            odometer,
            solar
        )

        val accelAffectedTools = listOf(
            navigation,
            inclinometer,
            level,
            solar
        )

        return when (code) {
            DiagnosticCode.AltitudeOverridden -> listOf(weather, waterBoil)
            DiagnosticCode.LocationOverridden -> locationAffectedTools
            DiagnosticCode.LocationUnset -> locationAffectedTools
            DiagnosticCode.PowerSavingMode -> listOf(backtrack, odometer)
            DiagnosticCode.BatteryHealthPoor -> listOf()
            DiagnosticCode.BatteryUsageRestricted -> listOf(backtrack, astronomy, odometer)
            DiagnosticCode.CameraUnavailable -> listOf(navigation, sightingCompass)
            DiagnosticCode.BarometerUnavailable -> listOf(weather)
            DiagnosticCode.MagnetometerUnavailable -> listOf(navigation, metalDetector, solar)
            DiagnosticCode.AccelerometerUnavailable -> accelAffectedTools
            DiagnosticCode.GPSUnavailable -> locationAffectedTools
            DiagnosticCode.FlashlightUnavailable -> listOf(flashlight)
            DiagnosticCode.PedometerUnavailable -> listOf(odometer)
            DiagnosticCode.CameraNoPermission -> listOf(navigation, sightingCompass)
            DiagnosticCode.LocationNoPermission -> locationAffectedTools
            DiagnosticCode.BackgroundLocationNoPermission -> listOf(
                backtrack,
                astronomy,
                speedometer,
                odometer
            )
            DiagnosticCode.PedometerNoPermission -> listOf(odometer)
            DiagnosticCode.BarometerPoor -> listOf(weather)
            DiagnosticCode.MagnetometerPoor -> listOf(navigation, metalDetector, solar)
            DiagnosticCode.AccelerometerPoor -> accelAffectedTools
            DiagnosticCode.GPSPoor -> locationAffectedTools
            DiagnosticCode.GPSTimedOut -> locationAffectedTools
            DiagnosticCode.SunsetAlertsBlocked -> listOf(astronomy)
            DiagnosticCode.StormAlertsBlocked -> listOf(weather)
            DiagnosticCode.DailyForecastNotificationsBlocked -> listOf(weather)
            DiagnosticCode.FlashlightNotificationsBlocked -> listOf(flashlight)
            DiagnosticCode.PedometerNotificationsBlocked -> listOf(odometer)
            DiagnosticCode.WeatherNotificationsBlocked -> listOf(weather)
            DiagnosticCode.LightSensorUnavailable -> listOf(lightMeter)
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
            DiagnosticCode.CameraNoPermission -> getString(
                R.string.grant_permission,
                getString(R.string.camera)
            )
            DiagnosticCode.LocationNoPermission -> getString(
                R.string.grant_permission,
                getString(R.string.location)
            )
            DiagnosticCode.BackgroundLocationNoPermission -> getString(
                R.string.grant_permission, getString(
                    R.string.background_location_permission
                )
            )
            DiagnosticCode.PedometerNoPermission -> getString(
                R.string.grant_permission,
                getString(R.string.activity_recognition)
            )
            DiagnosticCode.BarometerPoor -> getString(R.string.no_resolution)
            DiagnosticCode.MagnetometerPoor -> getString(
                R.string.calibrate_compass_dialog_content,
                getString(android.R.string.ok)
            )
            DiagnosticCode.AccelerometerPoor -> getString(R.string.no_resolution)
            DiagnosticCode.GPSPoor -> getString(R.string.get_gps_signal)
            DiagnosticCode.GPSTimedOut -> getString(R.string.get_gps_signal)
            DiagnosticCode.SunsetAlertsBlocked -> getString(
                R.string.unblock_notification_channel,
                getString(R.string.sunset_alert_channel_title)
            )
            DiagnosticCode.StormAlertsBlocked -> getString(
                R.string.unblock_notification_channel,
                getString(R.string.alerts)
            )
            DiagnosticCode.DailyForecastNotificationsBlocked -> getString(
                R.string.unblock_notification_channel,
                getString(R.string.todays_forecast)
            )
            DiagnosticCode.FlashlightNotificationsBlocked -> getString(
                R.string.unblock_notification_channel,
                getString(R.string.flashlight_title)
            )
            DiagnosticCode.PedometerNotificationsBlocked -> getString(
                R.string.unblock_notification_channel,
                getString(R.string.odometer)
            )
            DiagnosticCode.WeatherNotificationsBlocked -> getString(
                R.string.unblock_notification_channel,
                getString(R.string.weather)
            )
            DiagnosticCode.LightSensorUnavailable -> getString(R.string.no_resolution)
        }
    }

    private fun getAction(code: DiagnosticCode): Action? {
        return when (code) {
            DiagnosticCode.AltitudeOverridden -> navigateAction(R.id.calibrateAltimeterFragment)
            DiagnosticCode.LocationOverridden -> navigateAction(R.id.calibrateGPSFragment)
            DiagnosticCode.LocationUnset -> navigateAction(R.id.calibrateGPSFragment)
            DiagnosticCode.PowerSavingMode -> navigateAction(R.id.powerSettingsFragment)
            DiagnosticCode.BatteryHealthPoor -> null
            DiagnosticCode.BatteryUsageRestricted -> intentAction(Intents.batteryOptimizationSettings())
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
        }
    }

    private fun locationSourcesAction(): Action {
        return Action(getString(R.string.settings)) {
            try {
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    private fun notificationAction(): Action {
        return intentAction(Intents.appSettings(requireContext()))
    }

    private fun permissionAction(): Action {
        return intentAction(Intents.appSettings(requireContext()))
    }

    private fun intentAction(to: Intent, title: String = getString(R.string.settings)): Action {
        return Action(title) {
            startActivity(to)
        }
    }

    private fun navigateAction(
        @IdRes to: Int,
        title: String = getString(R.string.settings)
    ): Action {
        return Action(title) {
            findNavController().navigate(to)
        }
    }

    data class Action(val title: String, val action: () -> Unit)

}