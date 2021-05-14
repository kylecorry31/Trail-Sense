package com.kylecorry.trail_sense.settings

import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Window
import android.view.WindowManager
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.BacktrackScheduler
import com.kylecorry.trailsensecore.domain.units.Distance
import com.kylecorry.trailsensecore.domain.units.DistanceUnits
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import java.time.Duration

class NavigationSettingsFragment : CustomPreferenceFragment() {

    private var prefNearbyRadius: Preference? = null
    private var prefBacktrack: SwitchPreferenceCompat? = null
    private var prefLeftQuickAction: ListPreference? = null
    private var prefRightQuickAction: ListPreference? = null
    private val formatService by lazy { FormatServiceV2(requireContext()) }

    private lateinit var prefs: UserPreferences

    private fun bindPreferences() {
        prefNearbyRadius = preference(R.string.pref_nearby_radius_holder)
        prefBacktrack = switch(R.string.pref_backtrack_enabled)
        prefLeftQuickAction = list(R.string.pref_navigation_quick_action_left)
        prefRightQuickAction = list(R.string.pref_navigation_quick_action_right)
    }

    private fun restartBacktrack() {
        if (prefs.backtrackEnabled) {
            BacktrackScheduler.stop(requireContext())
            BacktrackScheduler.start(requireContext())
        }
    }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.navigation_preferences, rootKey)
        val userPrefs = UserPreferences(requireContext())
        prefs = userPrefs
        bindPreferences()

        val actions = QuickActionUtils.navigation(requireContext())
        val actionNames = actions.map { QuickActionUtils.getName(requireContext(), it) }
        val actionValues = actions.map { it.id.toString() }

        prefLeftQuickAction?.entries = actionNames.toTypedArray()
        prefRightQuickAction?.entries = actionNames.toTypedArray()

        prefLeftQuickAction?.entryValues = actionValues.toTypedArray()
        prefRightQuickAction?.entryValues = actionValues.toTypedArray()


        prefBacktrack?.isEnabled = !(prefs.isLowPowerModeOn && prefs.lowPowerModeDisablesBacktrack)

        prefBacktrack?.setOnPreferenceClickListener {
            if (prefs.backtrackEnabled) {
                BacktrackScheduler.start(requireContext())
            } else {
                BacktrackScheduler.stop(requireContext())
            }
            true
        }

        val prefBacktrackInterval = preference(R.string.pref_backtrack_interval)
        prefBacktrackInterval?.summary =
            formatService.formatDuration(prefs.backtrackRecordFrequency)

        prefBacktrackInterval?.setOnPreferenceClickListener {
            val title = it.title.toString()
            CustomUiUtils.pickDuration(
                requireContext(),
                prefs.backtrackRecordFrequency,
                title,
                getString(R.string.actual_frequency_disclaimer)
            ) {
                if (it != null && !it.isZero) {
                    prefs.backtrackRecordFrequency = it
                    prefBacktrackInterval.summary = formatService.formatDuration(it)
                    restartBacktrack()

                    if (it < Duration.ofMinutes(10)) {
                        UiUtils.alert(
                            requireContext(),
                            getString(R.string.battery_warning),
                            getString(R.string.backtrack_battery_warning),
                            getString(R.string.dialog_ok)
                        )
                    }

                }
            }
            true
        }

        val distanceUnits = prefs.distanceUnits

        prefNearbyRadius?.setOnPreferenceClickListener {
            CustomUiUtils.pickDistance(
                requireContext(),
                if (distanceUnits == UserPreferences.DistanceUnits.Meters) listOf(
                    DistanceUnits.Meters,
                    DistanceUnits.Kilometers
                ) else listOf(DistanceUnits.Feet, DistanceUnits.Miles, DistanceUnits.NauticalMiles),
                Distance.meters(userPrefs.navigation.maxBeaconDistance)
                    .convertTo(userPrefs.baseDistanceUnits).toRelativeDistance(),
                it.title.toString()
            ) { distance ->
                if (distance != null && distance.distance > 0) {
                    userPrefs.navigation.maxBeaconDistance = distance.meters().distance
                    updateNearbyRadius()
                }
            }
            true
        }

        val prefBacktrackPathColor = preference(R.string.pref_backtrack_path_color)
        prefBacktrackPathColor?.icon?.setTint(
                prefs.navigation.backtrackPathColor.color
        )

        prefBacktrackPathColor?.setOnPreferenceClickListener {
            CustomUiUtils.pickColor(
                requireContext(),
                prefs.navigation.backtrackPathColor,
                it.title.toString()
            ) {
                if (it != null) {
                    prefs.navigation.backtrackPathColor = it
                    prefBacktrackPathColor.icon?.setTint(it.color)
                }
            }
            true
        }

        editText(R.string.pref_num_visible_beacons)
            ?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }

        updateNearbyRadius()
    }

    private fun updateNearbyRadius() {
        prefNearbyRadius?.summary = formatService.formatDistance(
            Distance.meters(prefs.navigation.maxBeaconDistance).convertTo(prefs.baseDistanceUnits)
                .toRelativeDistance(),
            2
        )
    }
}