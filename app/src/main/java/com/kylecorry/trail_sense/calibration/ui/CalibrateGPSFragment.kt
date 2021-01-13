package com.kylecorry.trail_sense.calibration.ui

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.preference.*
import androidx.recyclerview.widget.RecyclerView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.locationformat.LocationDecimalDegreesFormatter
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.GPS
import com.kylecorry.trailsensecore.infrastructure.sensors.gps.IGPS
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.CoordinatePreference
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import com.kylecorry.trailsensecore.infrastructure.time.Throttle


class CalibrateGPSFragment : PreferenceFragmentCompat() {

    private val prefs by lazy { UserPreferences(requireContext()) }
    private val sensorService by lazy { SensorService(requireContext()) }
    private val throttle = Throttle(20)

    private lateinit var locationTxt: Preference
    private lateinit var autoLocationSwitch: SwitchPreferenceCompat
    private lateinit var permissionBtn: Preference
    private lateinit var locationOverridePref: CoordinatePreference

    private lateinit var gps: IGPS
    private val realGps by lazy { GPS(requireContext()) }

    private val formatter = LocationDecimalDegreesFormatter()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.gps_calibration, rootKey)
        gps = sensorService.getGPS()
        bindPreferences()
    }

    private fun bindPreferences() {
        locationTxt = findPreference(getString(R.string.pref_holder_location))!!
        autoLocationSwitch = findPreference(getString(R.string.pref_auto_location))!!
        permissionBtn = findPreference(getString(R.string.pref_gps_request_permission))!!
        locationOverridePref = findPreference(getString(R.string.pref_gps_override))!!
        locationOverridePref.setGPS(realGps)
        locationOverridePref.setLocation(prefs.locationOverride)
        locationOverridePref.setTitle(getString(R.string.pref_gps_override_title))

        locationOverridePref.setOnLocationChangeListener {
            prefs.locationOverride = it ?: Coordinate.zero
            resetGPS()
            update()
        }

        autoLocationSwitch.setOnPreferenceClickListener {
            locationOverridePref.isEnabled = !prefs.useAutoLocation || !prefs.useAutoLocation
            resetGPS()
            update()
            true
        }

        permissionBtn.setOnPreferenceClickListener {
            val intent = IntentUtils.appSettings(requireContext())
            startActivityForResult(intent, 1000)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        if (gps.hasValidReading) {
            update()
        }
        startGPS()
    }

    override fun onPause() {
        super.onPause()
        stopGPS()
    }

    private fun resetGPS() {
        stopGPS()
        gps = sensorService.getGPS()
        startGPS()
    }

    private fun startGPS() {
        gps.start(this::onLocationUpdate)
    }

    private fun stopGPS() {
        gps.stop(this::onLocationUpdate)
    }


    private fun onLocationUpdate(): Boolean {
        update()
        return true
    }

    private fun update() {
        if (throttle.isThrottled()) {
            return
        }

        permissionBtn.isVisible = !prefs.useLocationFeatures
        autoLocationSwitch.isEnabled = prefs.useLocationFeatures
        locationOverridePref.isEnabled = !prefs.useAutoLocation || !prefs.useAutoLocation

        locationTxt.summary = formatter.format(gps.location)
    }


}