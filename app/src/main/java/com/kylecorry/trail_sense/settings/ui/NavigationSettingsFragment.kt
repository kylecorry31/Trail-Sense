package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import android.text.InputType
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.QuickActionUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.permissions.alertNoActivityRecognitionPermission
import com.kylecorry.trail_sense.shared.permissions.requestActivityRecognition
import com.kylecorry.trail_sense.shared.sensors.SensorService

class NavigationSettingsFragment : AndromedaPreferenceFragment() {

    private var prefNearbyRadius: Preference? = null
    private var prefLeftButton: ListPreference? = null
    private var prefRightButton: ListPreference? = null
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val hasCompass by lazy { SensorService(requireContext()).hasCompass() }

    private lateinit var prefs: UserPreferences

    private fun bindPreferences() {
        prefNearbyRadius = preference(R.string.pref_nearby_radius_holder)
        prefLeftButton = list(R.string.pref_navigation_quick_action_left)
        prefRightButton = list(R.string.pref_navigation_quick_action_right)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.navigation_preferences, rootKey)
        val userPrefs = UserPreferences(requireContext())
        prefs = userPrefs
        bindPreferences()

        val actions = QuickActionUtils.navigation(requireContext())
        val actionNames = actions.map { QuickActionUtils.getName(requireContext(), it) }
        val actionValues = actions.map { it.id.toString() }

        prefLeftButton?.entries = actionNames.toTypedArray()
        prefRightButton?.entries = actionNames.toTypedArray()

        prefLeftButton?.entryValues = actionValues.toTypedArray()
        prefRightButton?.entryValues = actionValues.toTypedArray()


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

        editText(R.string.pref_num_visible_beacons)
            ?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }

        onChange(list(R.string.pref_navigation_speedometer_type)) {
            if (it == "instant_pedometer") {
                onCurrentPaceSpeedometerSelected()
            }
        }

        // Hide preferences if there is no compass
        if (!hasCompass) {
            // Set multi beacons to true (enables other settings)
            switch(R.string.pref_display_multi_beacons)?.isChecked = true

            // Hide the preferences
            listOf(
                preference(R.string.pref_display_multi_beacons),
                preference(R.string.pref_nearby_radar),
                preference(R.string.pref_show_linear_compass),
                preference(R.string.pref_show_calibrate_on_navigate_dialog)
            ).forEach {
                it?.isVisible = false
            }
        } else {
            preference(R.string.pref_show_dial_ticks_when_no_compass)?.isVisible = false
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