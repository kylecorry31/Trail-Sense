package com.kylecorry.trail_sense.calibration.ui

import android.Manifest
import android.os.Bundle
import androidx.preference.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.speedometer.infrastructure.PedometerService
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import com.kylecorry.trailsensecore.infrastructure.system.PermissionUtils
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer


class CalibrateOdometerFragment : PreferenceFragmentCompat() {

    private lateinit var strideLengthPref: Preference
    private lateinit var permissionPref: Preference
    private lateinit var odometerSourceList: ListPreference
    private val userPrefs by lazy { UserPreferences(requireContext()) }
    private val formatService by lazy { FormatServiceV2(requireContext()) }
    private var wasEnabled = false
    private val cache by lazy { Cache(requireContext()) }

    private val intervalometer = Intervalometer {
        updateStrideLength()
        updatePermissionRequestPreference()
        if (wasEnabled != userPrefs.usePedometer){
            updatePedometerService()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.odometer_calibration, rootKey)
        bindPreferences()
    }

    private fun bindPreferences() {
        strideLengthPref = findPreference(getString(R.string.pref_stride_length_holder))!!
        odometerSourceList = findPreference(getString(R.string.pref_odometer_source))!!
        permissionPref = findPreference(getString(R.string.pref_odometer_request_permission))!!

        permissionPref.setOnPreferenceClickListener {
            val intent = IntentUtils.appSettings(requireContext())
            startActivityForResult(intent, 1000)
            true
        }

        odometerSourceList.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue == "pedometer") {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    PermissionUtils.requestPermissions(
                        requireActivity(),
                        listOf(Manifest.permission.ACTIVITY_RECOGNITION), REQUEST_CODE
                    )
                }
            }
            true
        }

        strideLengthPref.setOnPreferenceClickListener {
            CustomUiUtils.pickDistance(
                requireContext(),
                listOf(userPrefs.baseDistanceUnits),
                userPrefs.strideLength.convertTo(userPrefs.baseDistanceUnits),
                getString(R.string.pref_stride_length_title)
            ) {
                if (it != null){
                    userPrefs.strideLength = it
                    updateStrideLength()
                }
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        wasEnabled = userPrefs.usePedometer
        intervalometer.interval(20)
    }

    override fun onPause() {
        intervalometer.stop()
        super.onPause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE) {
            updatePedometerService()
        }
    }

    private fun updatePermissionRequestPreference() {
        val hasActivityRecognition =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                PermissionUtils.hasPermission(
                    requireContext(),
                    Manifest.permission.ACTIVITY_RECOGNITION
                )
            } else {
                true
            }
        permissionPref.isVisible =
            (userPrefs.usePedometer && !hasActivityRecognition) || (!userPrefs.usePedometer && !PermissionUtils.isBackgroundLocationEnabled(
                requireContext()
            ))
    }

    private fun updatePedometerService() {
        if (userPrefs.usePedometer) {
            if (cache.getBoolean("pedometer_battery_sent") != true){
                UiUtils.alert(
                    requireContext(),
                    getString(R.string.pedometer),
                    getString(R.string.pedometer_disclaimer),
                    getString(R.string.dialog_ok)
                )
                cache.putBoolean("pedometer_battery_sent", true)
            }
            PedometerService.start(requireContext())
        } else {
            PedometerService.stop(requireContext())
        }

        wasEnabled = userPrefs.usePedometer
    }

    private fun updateStrideLength() {
        strideLengthPref.isEnabled = userPrefs.usePedometer
        strideLengthPref.summary = formatService.formatDistance(
            userPrefs.strideLength.convertTo(userPrefs.baseDistanceUnits),
            2
        )
    }


    companion object {
        private const val REQUEST_CODE = 10
    }

}