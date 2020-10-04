package com.kylecorry.trail_sense.settings

import android.os.Bundle
import android.text.InputType
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.preference.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.units.DistanceUnits
import com.kylecorry.trailsensecore.domain.units.UnitService

class NavigationSettingsFragment : PreferenceFragmentCompat() {

    private var prefMaxBeaconDistanceKm: EditTextPreference? = null
    private var prefMaxBeaconDistanceMi: EditTextPreference? = null
    private val unitService = UnitService()
    private val formatService by lazy { FormatService(requireContext()) }

    private lateinit var prefs: UserPreferences

    private fun bindPreferences() {
        prefMaxBeaconDistanceKm = editText(R.string.pref_max_beacon_distance)
        prefMaxBeaconDistanceMi = editText(R.string.pref_max_beacon_distance_miles)
    }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.navigation_preferences, rootKey)
        val userPrefs = UserPreferences(requireContext())
        prefs = userPrefs
        bindPreferences()

        val distanceUnits = prefs.distanceUnits

        if (distanceUnits == UserPreferences.DistanceUnits.Feet) {
            prefMaxBeaconDistanceKm?.isVisible = false
            prefMaxBeaconDistanceMi?.isVisible = true
        } else {
            prefMaxBeaconDistanceKm?.isVisible = true
            prefMaxBeaconDistanceMi?.isVisible = false
        }


        val maxDistance = prefs.navigation.maxBeaconDistance
        prefMaxBeaconDistanceMi?.summary = formatService.formatDistance(
            unitService.convert(
                maxDistance,
                DistanceUnits.Meters,
                DistanceUnits.Miles
            ), DistanceUnits.Miles
        )
        prefMaxBeaconDistanceKm?.summary = formatService.formatDistance(
            unitService.convert(
                maxDistance,
                DistanceUnits.Meters,
                DistanceUnits.Kilometers
            ), DistanceUnits.Kilometers
        )

        prefMaxBeaconDistanceMi?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER.or(InputType.TYPE_NUMBER_FLAG_DECIMAL)
        }

        prefMaxBeaconDistanceKm?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER.or(InputType.TYPE_NUMBER_FLAG_DECIMAL)
        }

        prefMaxBeaconDistanceMi?.setOnPreferenceChangeListener { _, newValue ->
            val miles = newValue.toString().toFloatOrNull() ?: 62f
            prefs.navigation.maxBeaconDistance =
                unitService.convert(miles, DistanceUnits.Miles, DistanceUnits.Meters)
            prefMaxBeaconDistanceMi?.summary =
                formatService.formatDistance(miles, DistanceUnits.Miles)
            prefMaxBeaconDistanceKm?.summary = formatService.formatDistance(
                unitService.convert(
                    miles,
                    DistanceUnits.Miles,
                    DistanceUnits.Kilometers
                ), DistanceUnits.Kilometers
            )
            true
        }

        prefMaxBeaconDistanceKm?.setOnPreferenceChangeListener { _, newValue ->
            val km = newValue.toString().toFloatOrNull() ?: 100f
            preferenceManager.sharedPreferences.edit {
                putString(
                    getString(R.string.pref_max_beacon_distance_miles),
                    unitService.convert(km, DistanceUnits.Kilometers, DistanceUnits.Miles)
                        .toString()
                )
            }
            prefMaxBeaconDistanceMi?.summary = formatService.formatDistance(
                unitService.convert(
                    km,
                    DistanceUnits.Kilometers,
                    DistanceUnits.Miles
                ), DistanceUnits.Miles
            )
            prefMaxBeaconDistanceKm?.summary =
                formatService.formatDistance(km, DistanceUnits.Kilometers)
            true
        }

        editText(R.string.pref_num_visible_beacons)
            ?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }
    }

    private fun editText(@StringRes id: Int): EditTextPreference? {
        return preferenceManager.findPreference(getString(id))
    }
}