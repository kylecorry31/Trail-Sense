package com.kylecorry.trail_sense.settings.ui

import android.os.Build
import android.os.Bundle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.alerts.NotificationSubsystem.GroupBehavior
import com.kylecorry.trail_sense.shared.debugging.isPlayStoreBuild
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
        preference(R.string.pref_plugins_enabled)?.isVisible = !isPlayStoreBuild()

        onClick(preference(R.string.pref_cliff_height_enabled)) {
            requireMainActivity().updateBottomNavigation()
        }

        onClick(switch(R.string.pref_widget_icon_compatibility)) {
            // Refresh all widgets so the new icon rendering takes effect immediately
            Tools.triggerWidgetUpdate(requireContext(), null)
        }

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
