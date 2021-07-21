package com.kylecorry.trail_sense.settings.ui

import android.Manifest
import android.os.Bundle
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.RequestCodes
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import com.kylecorry.trailsensecore.infrastructure.system.PackageUtils
import com.kylecorry.trailsensecore.infrastructure.system.PermissionUtils
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils

class ExperimentalSettingsFragment : CustomPreferenceFragment() {

    private val prefs by lazy { UserPreferences(requireContext()) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.experimental_preferences, rootKey)

        val sensorChecker = SensorChecker(requireContext())
        preference(R.string.pref_experimental_metal_direction)?.isVisible =
            sensorChecker.hasGyroscope()

        preference(R.string.pref_depth_enabled)?.isVisible = sensorChecker.hasBarometer()

        onClick(switch(R.string.pref_experimental_maps)) {
            PackageUtils.setComponentEnabled(
                requireContext(),
                "com.kylecorry.trail_sense.AliasMainActivity",
                prefs.navigation.areMapsEnabled
            )
        }

        onClick(switch(R.string.pref_experimental_sighting_compass)) {
            if (prefs.navigation.isSightingCompassEnabled && !PermissionUtils.isCameraEnabled(
                    requireContext()
                )
            ) {
                // TODO: Extract this to PermissionUtils for fragments
                // TODO: If previously denied, allow the user to open the settings
                requestPermissions(
                    listOf(Manifest.permission.CAMERA).toTypedArray(),
                    RequestCodes.REQUEST_CODE_CAMERA_PERMISSION
                )
            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (!wasCameraPermissionGranted(requestCode)) {
            prefs.navigation.isSightingCompassEnabled = false
            switch(R.string.pref_experimental_sighting_compass)?.isChecked = false
            UiUtils.longToast(requireContext(), getString(R.string.camera_permission_denied))
        }
    }

    private fun wasCameraPermissionGranted(requestCode: Int): Boolean {
        return requestCode == RequestCodes.REQUEST_CODE_CAMERA_PERMISSION &&
                PermissionUtils.isCameraEnabled(requireContext())
    }

}