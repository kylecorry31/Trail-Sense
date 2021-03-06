package com.kylecorry.trail_sense.calibration.ui

import android.os.Bundle
import android.text.InputType
import androidx.preference.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.CustomPreferenceFragment
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.domain.geo.GeoService
import com.kylecorry.trailsensecore.domain.units.Quality
import com.kylecorry.trailsensecore.infrastructure.sensors.compass.ICompass
import com.kylecorry.trailsensecore.infrastructure.sensors.gps.IGPS
import com.kylecorry.trailsensecore.infrastructure.time.Throttle


class CalibrateCompassFragment : CustomPreferenceFragment() {

    private lateinit var prefs: UserPreferences
    private val formatService by lazy { FormatServiceV2(requireContext()) }
    private lateinit var sensorService: SensorService
    private val throttle = Throttle(20)

    private lateinit var azimuthTxt: Preference
    private lateinit var legacyCompassSwitch: SwitchPreferenceCompat
    private lateinit var compassSmoothingBar: SeekBarPreference
    private lateinit var declinationTxt: Preference
    private lateinit var trueNorthSwitch: SwitchPreferenceCompat
    private lateinit var autoDeclinationSwitch: SwitchPreferenceCompat
    private lateinit var declinationOverrideEdit: EditTextPreference
    private lateinit var declinationFromGpsBtn: Preference
    private lateinit var calibrateBtn: Preference

    private lateinit var compass: ICompass
    private lateinit var gps: IGPS
    private val geoService = GeoService()

    private var prevQuality = Quality.Unknown

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.compass_calibration, rootKey)

        setIconColor(UiUtils.androidTextColorSecondary(requireContext()))

        prefs = UserPreferences(requireContext())
        sensorService = SensorService(requireContext())

        compass = sensorService.getCompass()
        gps = sensorService.getGPS(false)
        bindPreferences()
    }

    private fun bindPreferences() {
        azimuthTxt = findPreference(getString(R.string.pref_holder_azimuth))!!
        legacyCompassSwitch = findPreference(getString(R.string.pref_use_legacy_compass))!!
        compassSmoothingBar = findPreference(getString(R.string.pref_compass_filter_amt))!!
        declinationTxt = findPreference(getString(R.string.pref_holder_declination))!!
        trueNorthSwitch = findPreference(getString(R.string.pref_use_true_north))!!
        autoDeclinationSwitch = findPreference(getString(R.string.pref_auto_declination))!!
        declinationOverrideEdit = findPreference(getString(R.string.pref_declination_override))!!
        declinationFromGpsBtn =
            findPreference(getString(R.string.pref_declination_override_gps_btn))!!
        calibrateBtn = findPreference(getString(R.string.pref_calibrate_compass_btn))!!

        declinationOverrideEdit.summary =
            getString(R.string.degree_format, prefs.declinationOverride)
        declinationOverrideEdit.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER.or(InputType.TYPE_NUMBER_FLAG_DECIMAL)
                .or(InputType.TYPE_NUMBER_FLAG_SIGNED)
        }

        trueNorthSwitch.setOnPreferenceClickListener {
            resetCompass()
            true
        }

        autoDeclinationSwitch.setOnPreferenceClickListener {
            update()
            true
        }

        declinationFromGpsBtn.setOnPreferenceClickListener {
            updateDeclinationFromGps()
            true
        }

        legacyCompassSwitch.setOnPreferenceClickListener {
            resetCompass()
            true
        }

        compassSmoothingBar.setOnPreferenceClickListener {
            resetCompass()
            true
        }

        calibrateBtn.setOnPreferenceClickListener {
            UiUtils.alert(
                requireContext(), getString(R.string.calibrate_compass_dialog_title), getString(
                    R.string.calibrate_compass_dialog_content, getString(R.string.dialog_ok)
                ),
                R.string.dialog_ok
            )
            true
        }
    }

    override fun onResume() {
        super.onResume()
        startCompass()
        if (!gps.hasValidReading) {
            gps.start(this::onLocationUpdate)
        }
    }

    override fun onPause() {
        super.onPause()
        stopCompass()
        gps.stop(this::onLocationUpdate)
        gps.stop(this::onUpdateDeclinationFromGpsCallback)
    }

    private fun onLocationUpdate(): Boolean {
        update()
        return false
    }

    private fun updateDeclinationFromGps() {
        if (gps.hasValidReading) {
            onUpdateDeclinationFromGpsCallback()
        } else {
            gps.start(this::onUpdateDeclinationFromGpsCallback)
        }
    }

    private fun getDeclination(): Float {
        return if (!prefs.useAutoDeclination){
            prefs.declinationOverride
        } else {
            geoService.getDeclination(gps.location, gps.altitude)
        }
    }

    private fun onUpdateDeclinationFromGpsCallback(): Boolean {
        val declination = geoService.getDeclination(gps.location, gps.altitude)
        prefs.declinationOverride = declination
        declinationOverrideEdit.text = declination.toString()
        UiUtils.shortToast(requireContext(), getString(R.string.declination_override_updated_toast))
        return false
    }

    private fun resetCompass() {
        stopCompass()
        compass = sensorService.getCompass()
        startCompass()
    }

    private fun startCompass() {
        compass.start(this::onCompassUpdate)
    }

    private fun stopCompass() {
        compass.stop(this::onCompassUpdate)
    }

    private fun onDeclinationUpdate(): Boolean {
        update()
        return false
    }

    private fun onCompassUpdate(): Boolean {
        update()
        return true
    }

    private fun update() {

        if (throttle.isThrottled()) {
            return
        }

        if (prevQuality != Quality.Unknown && prevQuality != compass.quality) {
            if (compass.quality.ordinal > prevQuality.ordinal) {
                UiUtils.shortToast(
                    requireContext(),
                    getString(R.string.compass_accuracy_improved, getCompassAccuracy())
                )
            }
            prevQuality = compass.quality
        }

        compass.declination = getDeclination()

        calibrateBtn.summary = getString(R.string.compass_reported_accuracy, getCompassAccuracy())
        azimuthTxt.summary = getString(R.string.degree_format, compass.bearing.value)
        declinationTxt.summary = getString(R.string.degree_format, compass.declination)
        declinationOverrideEdit.summary =
            getString(R.string.degree_format, prefs.declinationOverride)
    }


    private fun getCompassAccuracy(): String {
        return formatService.formatQuality(compass.quality)
    }


}