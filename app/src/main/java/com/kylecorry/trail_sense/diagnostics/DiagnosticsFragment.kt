package com.kylecorry.trail_sense.diagnostics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentDiagnosticsBinding
import com.kylecorry.trail_sense.databinding.ListItemPlainIconBinding
import com.kylecorry.trail_sense.shared.AppColor
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
        diagnosticListView =
            ListView(binding.diagnosticsList, R.layout.list_item_plain_icon) { itemView, code ->
                val itemBinding = ListItemPlainIconBinding.bind(itemView)
                itemBinding.title.text = getCodeTitle(code)
                itemBinding.description.text = getCodeDescription(code)
                itemBinding.icon.setImageResource(android.R.drawable.stat_notify_error)
                CustomUiUtils.setImageColor(itemBinding.icon, getStatusTint(code.severity))
                // TODO: Allow the user to take action
                // TODO: Provide a description of what the results of the error are
//                itemBinding.root.setOnClickListener {
//                    result.fullMessage?.let {
//                        Alerts.dialog(
//                            requireContext(),
//                            result.title,
//                            it.message ?: result.message,
//                            okText = it.actionTitle ?: getString(android.R.string.ok)
//                        ) { cancelled ->
//                            if (!cancelled) {
//                                it.action.invoke()
//                            }
//                        }
//                    }
//                }
            }
        diagnosticListView.addLineSeparator()
        diagnostics = listOfNotNull(
            AccelerometerDiagnostic(requireContext(), this),
            MagnetometerDiagnostic(requireContext(), this),
            GPSDiagnostic(requireContext(), this),
            AltimeterDiagnostic(requireContext()),
            BatteryDiagnostic(requireContext(), this),
            CameraDiagnostic(requireContext()),
            FlashlightDiagnostic(requireContext()),
            PedometerDiagnostic(requireContext()),
            NotificationDiagnostic(requireContext())
        )
        scheduleUpdates(1000)
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
            DiagnosticCode.CameraUnavailable -> getString(R.string.gps_unavailable)
            DiagnosticCode.BarometerUnavailable -> getString(R.string.gps_unavailable)
            DiagnosticCode.MagnetometerUnavailable -> getString(R.string.gps_unavailable)
            DiagnosticCode.AccelerometerUnavailable -> getString(R.string.gps_unavailable)
            DiagnosticCode.GPSUnavailable -> getString(R.string.gps_unavailable)
            DiagnosticCode.FlashlightUnavailable -> getString(R.string.gps_unavailable)
            DiagnosticCode.PedometerUnavailable -> getString(R.string.gps_unavailable)
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
            DiagnosticCode.PowerSavingMode -> getString(R.string.tool_battery_title)
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
            DiagnosticCode.SunsetAlertsBlocked -> getString(R.string.pref_sunset_alerts_title)
            DiagnosticCode.StormAlertsBlocked -> getString(R.string.notification_storm_alert_channel_desc)
            DiagnosticCode.DailyForecastNotificationsBlocked -> getString(R.string.todays_forecast)
            DiagnosticCode.FlashlightNotificationsBlocked -> getString(R.string.flashlight_title)
            DiagnosticCode.PedometerNotificationsBlocked -> getString(R.string.pedometer)
            DiagnosticCode.WeatherNotificationsBlocked -> getString(R.string.weather)
        }
    }

}