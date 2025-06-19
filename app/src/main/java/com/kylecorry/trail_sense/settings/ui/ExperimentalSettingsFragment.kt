package com.kylecorry.trail_sense.settings.ui

import android.os.Build
import android.os.Bundle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.sol.science.meteorology.forecast.ForecastSource
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.alerts.NotificationSubsystem.GroupBehavior
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.requireMainActivity
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class ExperimentalSettingsFragment : AndromedaPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.experimental_preferences, rootKey)

        val sensors = SensorService(requireContext())
        val hasGyro = Sensors.hasGyroscope(requireContext())
        val hasCompass = sensors.hasCompass()

        preference(R.string.pref_experimental_metal_direction)?.isVisible = hasGyro && hasCompass

        onClick(preference(R.string.pref_cliff_height_enabled)) {
            requireMainActivity().updateBottomNavigation()
        }

        val sources = ForecastSource.entries
        val sourceNames = sources.map { it.name }
        val idMap = mapOf(
            ForecastSource.Sol to 1,
            ForecastSource.Zambretti to 2,
        )
        val sourceIds = sources.map { (idMap[it] ?: 0).toString() }

        val forecastSource = list(R.string.pref_weather_forecast_source)

        forecastSource?.entries = sourceNames.toTypedArray()
        forecastSource?.entryValues = sourceIds.toTypedArray()
        forecastSource?.isVisible =
            Tools.isToolAvailable(requireContext(), Tools.WEATHER) && isDebug()

        val notificationGrouping = list(R.string.pref_notification_group_behavior)

        val groupBehaviors = GroupBehavior.entries

        val groupBehaviorNames = mapOf(
            GroupBehavior.System to getString(R.string.theme_system),
            GroupBehavior.UngroupAll to getString(R.string.ungroup_all),
            GroupBehavior.UngroupHigh to getString(R.string.ungroup_alerts)
        )

        val groupBehaviorIds = mapOf(
            GroupBehavior.System to 1,
            GroupBehavior.UngroupAll to 2,
            GroupBehavior.UngroupHigh to 3
        )

        notificationGrouping?.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA

        notificationGrouping?.entries = groupBehaviors
            .map { groupBehaviorNames[it] ?: it.name }
            .toTypedArray()
        notificationGrouping?.entryValues = groupBehaviors
            .map { (groupBehaviorIds[it] ?: 0).toString() }
            .toTypedArray()
    }
}