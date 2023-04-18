package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import android.text.InputType
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.infrastructure.subsystem.BacktrackSubsystem
import com.kylecorry.trail_sense.navigation.paths.ui.commands.ChangeBacktrackFrequencyCommand
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.QuickActionUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.permissions.RequestRemoveBatteryRestrictionCommand
import com.kylecorry.trail_sense.shared.permissions.alertNoActivityRecognitionPermission
import com.kylecorry.trail_sense.shared.permissions.requestActivityRecognition
import java.time.Duration

class NavigationSettingsFragment : AndromedaPreferenceFragment() {

    private var prefNearbyRadius: Preference? = null
    private var prefBacktrack: SwitchPreferenceCompat? = null
    private var prefleftButton: ListPreference? = null
    private var prefrightButton: ListPreference? = null
    private val formatService by lazy { FormatService.getInstance(requireContext()) }

    private lateinit var prefs: UserPreferences

    private fun bindPreferences() {
        prefNearbyRadius = preference(R.string.pref_nearby_radius_holder)
        prefBacktrack = switch(R.string.pref_backtrack_enabled)
        prefleftButton = list(R.string.pref_navigation_quick_action_left)
        prefrightButton = list(R.string.pref_navigation_quick_action_right)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.navigation_preferences, rootKey)
        val userPrefs = UserPreferences(requireContext())
        prefs = userPrefs
        bindPreferences()

        val actions = QuickActionUtils.navigation(requireContext())
        val actionNames = actions.map { QuickActionUtils.getName(requireContext(), it) }
        val actionValues = actions.map { it.id.toString() }

        prefleftButton?.entries = actionNames.toTypedArray()
        prefrightButton?.entries = actionNames.toTypedArray()

        prefleftButton?.entryValues = actionValues.toTypedArray()
        prefrightButton?.entryValues = actionValues.toTypedArray()


        prefBacktrack?.isEnabled = !(prefs.isLowPowerModeOn && prefs.lowPowerModeDisablesBacktrack)

        prefBacktrack?.setOnPreferenceClickListener {
            val backtrack = BacktrackSubsystem.getInstance(requireContext())
            if (prefs.backtrackEnabled) {
                backtrack.enable(true)
                RequestRemoveBatteryRestrictionCommand(requireContext()).execute()
            } else {
                backtrack.disable()
            }
            true
        }

        val prefBacktrackInterval = preference(R.string.pref_backtrack_interval)
        prefBacktrackInterval?.summary =
            formatService.formatDuration(prefs.backtrackRecordFrequency, includeSeconds = true)

        prefBacktrackInterval?.setOnPreferenceClickListener {
            ChangeBacktrackFrequencyCommand(requireContext()) {
                prefBacktrackInterval.summary =
                    formatService.formatDuration(it, includeSeconds = true)
            }.execute()
            true
        }

        prefNearbyRadius?.setOnPreferenceClickListener {
            val units = formatService.sortDistanceUnits(DistanceUtils.hikingDistanceUnits)

            CustomUiUtils.pickDistance(
                requireContext(),
                units,
                Distance.meters(userPrefs.navigation.maxBeaconDistance)
                    .convertTo(userPrefs.baseDistanceUnits).toRelativeDistance(),
                it.title.toString()
            ) { distance, _ ->
                if (distance != null && distance.distance > 0) {
                    userPrefs.navigation.maxBeaconDistance = distance.meters().distance
                    updateNearbyRadius()
                }
            }
            true
        }

        val prefBacktrackPathColor = preference(R.string.pref_backtrack_path_color)
        prefBacktrackPathColor?.icon?.setTint(
            prefs.navigation.defaultPathColor.color
        )

        prefBacktrackPathColor?.setOnPreferenceClickListener {
            CustomUiUtils.pickColor(
                requireContext(),
                prefs.navigation.defaultPathColor,
                it.title.toString()
            ) {
                if (it != null) {
                    prefs.navigation.defaultPathColor = it
                    prefBacktrackPathColor.icon?.setTint(it.color)
                }
            }
            true
        }

        editText(R.string.pref_num_visible_beacons)
            ?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }

        val backtrackHistory = preference(R.string.pref_backtrack_history_days)
        backtrackHistory?.summary =
            formatService.formatDays(prefs.navigation.backtrackHistory.toDays().toInt())
        backtrackHistory?.setOnPreferenceClickListener {
            Pickers.number(
                requireContext(),
                it.title.toString(),
                null,
                prefs.navigation.backtrackHistory.toDays().toInt(),
                allowDecimals = false,
                allowNegative = false,
                hint = getString(R.string.days)
            ) { days ->
                if (days != null) {
                    prefs.navigation.backtrackHistory = Duration.ofDays(days.toLong())
                    it.summary = formatService.formatDays(if (days.toInt() > 0) days.toInt() else 1)
                }
            }
            true
        }

        onChange(list(R.string.pref_navigation_speedometer_type)) {
            if (it == "instant_pedometer") {
                onCurrentPaceSpeedometerSelected()
            }
        }

        updateNearbyRadius()
    }

    private fun onCurrentPaceSpeedometerSelected() {
        requestActivityRecognition { hasPermission ->
            if (!hasPermission) {
                alertNoActivityRecognitionPermission()
            }
        }
    }

    private fun updateNearbyRadius() {
        prefNearbyRadius?.summary = formatService.formatDistance(
            Distance.meters(prefs.navigation.maxBeaconDistance).convertTo(prefs.baseDistanceUnits)
                .toRelativeDistance(),
            2
        )
    }
}