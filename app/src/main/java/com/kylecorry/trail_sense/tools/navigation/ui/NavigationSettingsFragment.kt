package com.kylecorry.trail_sense.tools.navigation.ui

import android.os.Bundle
import android.text.InputType
import androidx.preference.ListPreference
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayerPreferenceManager
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerPreferences
import com.kylecorry.trail_sense.shared.permissions.alertNoActivityRecognitionPermission
import com.kylecorry.trail_sense.shared.permissions.requestActivityRecognition
import com.kylecorry.trail_sense.shared.preferences.setupDistanceSetting
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class NavigationSettingsFragment : AndromedaPreferenceFragment() {

    private var prefLeftButton: ListPreference? = null
    private var prefRightButton: ListPreference? = null
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val hasCompass by lazy { SensorService(requireContext()).hasCompass() }

    private lateinit var prefs: UserPreferences

    private fun bindPreferences() {
        prefLeftButton = list(R.string.pref_navigation_quick_action_left)
        prefRightButton = list(R.string.pref_navigation_quick_action_right)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.navigation_preferences, rootKey)
        val userPrefs = UserPreferences(requireContext())
        prefs = userPrefs
        bindPreferences()

        val actions = Tools.getQuickActions(requireContext())
        val actionNames = actions.map { it.name }
        val actionValues = actions.map { it.id.toString() }

        prefLeftButton?.entries = actionNames.toTypedArray()
        prefRightButton?.entries = actionNames.toTypedArray()

        prefLeftButton?.entryValues = actionValues.toTypedArray()
        prefRightButton?.entryValues = actionValues.toTypedArray()

        setupDistanceSetting(
            getString(R.string.pref_nearby_radius_holder),
            { relative(Distance.meters(userPrefs.navigation.maxBeaconDistance), userPrefs) },
            { distance ->
                if (distance != null && distance.distance > 0) {
                    userPrefs.navigation.maxBeaconDistance = distance.meters().distance
                }
            },
            DistanceUtils.hikingDistanceUnits
        )

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

        val layerManager =
            MapLayerPreferenceManager("navigation", listOf(
                MapLayerPreferences.photoMaps(requireContext()),
                MapLayerPreferences.contours(requireContext())
            ), getString(R.string.pref_nearby_radar))
        layerManager.populatePreferences(preferenceScreen)
    }

    private fun relative(distance: Distance, prefs: UserPreferences): Distance {
        return distance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
    }

    private fun onCurrentPaceSpeedometerSelected() {
        requestActivityRecognition { hasPermission ->
            if (!hasPermission) {
                alertNoActivityRecognitionPermission()
            }
        }
    }
}