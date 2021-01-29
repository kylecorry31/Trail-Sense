package com.kylecorry.trail_sense.tools.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.maps.infrastructure.TrailSenseMaps
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.health.infrastructure.HealthSense
import com.kylecorry.trailsensecore.infrastructure.flashlight.Flashlight
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker


class ToolsFragment : PreferenceFragmentCompat() {

    private lateinit var navController: NavController

    private val sensorChecker by lazy { SensorChecker(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = findNavController()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.tools, rootKey)
        bindPreferences()
    }

    private fun bindPreferences() {
        navigateOnClick(
            findPreference(getString(R.string.tool_user_guide)),
            R.id.action_action_experimental_tools_to_guideListFragment
        )
        navigateOnClick(
            findPreference(getString(R.string.tool_bubble_level)),
            R.id.action_action_experimental_tools_to_levelFragment
        )
        navigateOnClick(
            findPreference(getString(R.string.tool_inclinometer)),
            R.id.action_toolsFragment_to_inclinometerFragment
        )
        navigateOnClick(
            findPreference(getString(R.string.tool_inventory)),
            R.id.action_action_experimental_tools_to_action_inventory
        )

        val maps = findPreference<Preference>(getString(R.string.tool_trail_sense_maps))
        maps?.isVisible = TrailSenseMaps.isInstalled(requireContext())
        onClick(maps) { TrailSenseMaps.open(requireContext()) }

        val health = findPreference<Preference>(getString(R.string.tool_health_sense))
        health?.isVisible = HealthSense.isInstalled(requireContext())
        onClick(health) { HealthSense.open(requireContext()) }

        val depth = findPreference<Preference>(getString(R.string.tool_depth))
        depth?.isVisible = sensorChecker.hasBarometer()
        navigateOnClick(depth, R.id.action_action_experimental_tools_to_toolDepthFragment)

        navigateOnClick(
            findPreference(getString(R.string.tool_cliff_height)),
            R.id.action_action_experimental_tools_to_toolCliffHeightFragment
        )

        navigateOnClick(
            findPreference(getString(R.string.tool_whistle)),
            R.id.action_action_experimental_tools_to_toolWhistleFragment
        )

        navigateOnClick(
            findPreference(getString(R.string.tool_distance_convert)),
            R.id.action_action_experimental_tools_to_fragmentDistanceConverter
        )

        navigateOnClick(
            findPreference(getString(R.string.tool_solar_panel)),
            R.id.action_action_experimental_tools_to_fragmentToolSolarPanel
        )

        navigateOnClick(
            findPreference(getString(R.string.tool_boil)),
            R.id.action_action_experimental_tools_to_waterPurificationFragment
        )

        navigateOnClick(
            findPreference(getString(R.string.tool_clock)),
            R.id.action_action_experimental_tools_to_toolClockFragment
        )

        navigateOnClick(
            findPreference(getString(R.string.tool_lightning)),
            R.id.action_action_experimental_tools_to_fragmentToolLightning
        )

        navigateOnClick(
            findPreference(getString(R.string.tool_ruler)),
            R.id.action_action_experimental_tools_to_rulerFragment
        )

        navigateOnClick(
            findPreference(getString(R.string.tool_battery)),
            R.id.action_action_experimental_tools_to_fragmentToolBattery
        )

        navigateOnClick(
            findPreference(getString(R.string.tool_triangulate)),
            R.id.action_action_experimental_tools_to_fragmentToolTriangulate
        )

        navigateOnClick(
            findPreference(getString(R.string.tool_metal_detector)),
            R.id.action_action_experimental_tools_to_fragmentToolMetalDetector
        )

        navigateOnClick(
            findPreference(getString(R.string.tool_notes)),
            R.id.action_action_experimental_tools_to_fragmentToolNotes
        )

        navigateOnClick(
            findPreference(getString(R.string.tool_backtrack)),
            R.id.action_action_experimental_tools_to_fragmentBacktrack
        )

        navigateOnClick(
            findPreference(getString(R.string.tool_coordinate_convert)),
            R.id.action_action_experimental_tools_to_fragmentToolCoordinateConvert
        )

        val thermometer = findPreference<Preference>(getString(R.string.tool_thermometer))
        thermometer?.isVisible = !sensorChecker.hasBarometer()
        navigateOnClick(
            thermometer,
            R.id.action_action_experimental_tools_to_thermometerFragment
        )

        val isExperimentalEnabled = prefs.experimentalEnabled
        val whiteNoise = findPreference<Preference>(getString(R.string.tool_white_noise))
        whiteNoise?.isVisible = isExperimentalEnabled
        navigateOnClick(whiteNoise, R.id.action_action_experimental_tools_to_fragmentToolWhiteNoise)

        val flashlight = findPreference<Preference>(getString(R.string.tool_flashlight))
        flashlight?.isVisible = Flashlight.hasFlashlight(requireContext())
        navigateOnClick(
            flashlight,
            R.id.action_action_experimental_tools_to_fragmentToolFlashlight
        )
    }

    private fun onClick(pref: Preference?, action: () -> Unit) {
        pref?.setOnPreferenceClickListener {
            action.invoke()
            true
        }
    }

    private fun navigateOnClick(pref: Preference?, @IdRes action: Int, bundle: Bundle? = null) {
        pref?.setOnPreferenceClickListener {
            navController.navigate(action, bundle)
            false
        }
    }

}