package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import android.text.InputType
import android.text.util.Linkify.WEB_URLS
import androidx.core.text.toSpannable
import androidx.core.text.util.LinkifyCompat
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.CoroutineAlerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.sense.altitude.FusedAltimeter
import com.kylecorry.andromeda.sense.barometer.IBarometer
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.andromeda.sense.readAll
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Pressure
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.URLs
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.dem.DEMRepo
import com.kylecorry.trail_sense.shared.dem.DigitalElevationModelLoader
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.SensorService
import kotlinx.coroutines.Dispatchers


class CalibrateAltimeterFragment : AndromedaPreferenceFragment() {

    private lateinit var barometer: IBarometer
    private lateinit var gps: IGPS
    private lateinit var altimeter: IAltimeter
    private lateinit var prefs: UserPreferences
    private lateinit var sensorService: SensorService
    private val throttle = Throttle(20)
    private var altimeterStarted = false
    private lateinit var distanceUnits: DistanceUnits

    private lateinit var altitudeTxt: Preference
    private lateinit var calibrationModeList: ListPreference
    private lateinit var altitudeOverridePref: Preference
    private lateinit var altitudeOverrideGpsBtn: Preference
    private lateinit var altitudeOverrideBarometerEdit: EditTextPreference
    private lateinit var accuracyPref: Preference
    private lateinit var clearCachePref: Preference
    private lateinit var continuousCalibrationPref: SwitchPreferenceCompat
    private lateinit var forceCalibrationPref: Preference
    private lateinit var demPref: Preference
    private lateinit var clearDemPref: Preference

    private lateinit var lastMode: UserPreferences.AltimeterMode
    private val updateTimer = CoroutineTimer { updateAltitude() }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }

    private val overridePopulationRunner = CoroutineQueueRunner(dispatcher = Dispatchers.Default)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.altimeter_calibration, rootKey)

        setIconColor(Resources.androidTextColorSecondary(requireContext()))

        prefs = UserPreferences(requireContext())
        sensorService = SensorService(requireContext())

        gps = CustomGPS(requireContext().applicationContext)
        barometer = sensorService.getBarometer()
        altimeter = sensorService.getAltimeter()

        distanceUnits = prefs.baseDistanceUnits

        bindPreferences()
    }

    private fun bindPreferences() {
        altitudeTxt = findPreference(getString(R.string.pref_holder_altitude))!!
        calibrationModeList = findPreference(getString(R.string.pref_altimeter_calibration_mode))!!
        altitudeOverridePref = findPreference(getString(R.string.pref_altitude_override))!!
        altitudeOverrideGpsBtn = findPreference(getString(R.string.pref_altitude_from_gps_btn))!!
        altitudeOverrideBarometerEdit =
            findPreference(getString(R.string.pref_altitude_override_sea_level))!!
        accuracyPref = preference(R.string.pref_altimeter_accuracy_holder)!!
        clearCachePref = preference(R.string.pref_altimeter_clear_cache_holder)!!
        continuousCalibrationPref = switch(R.string.pref_altimeter_continuous_calibration)!!
        forceCalibrationPref = preference(R.string.pref_fused_altimeter_force_calibration_holder)!!
        demPref = preference(R.string.pref_load_dem_button)!!
        clearDemPref = preference(R.string.pref_clear_dem_button)!!

        val altitudeOverride = Distance.meters(prefs.altitudeOverride).convertTo(distanceUnits)
        altitudeOverridePref.summary = formatService.formatDistance(altitudeOverride)

        updateConditionalPreferences()

        altitudeOverrideBarometerEdit.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER.or(InputType.TYPE_NUMBER_FLAG_DECIMAL)
                .or(InputType.TYPE_NUMBER_FLAG_SIGNED)
        }

        altitudeOverrideGpsBtn.setOnPreferenceClickListener {
            setOverrideFromGPS()
            true
        }

        altitudeOverrideBarometerEdit.setOnPreferenceChangeListener { _, newValue ->
            val seaLevelPressure = newValue.toString().toFloatOrNull() ?: 0.0f
            setOverrideFromBarometer(seaLevelPressure)
            true
        }

        altitudeOverridePref.setOnPreferenceClickListener {
            CustomUiUtils.pickElevation(
                requireContext(),
                Distance.meters(prefs.altitudeOverride).convertTo(distanceUnits),
                it.title.toString()
            ) { elevation, _ ->
                if (elevation != null) {
                    setAltitudeOverride(elevation)
                }
            }
            true
        }

        val samples = (1..8).toList()
        accuracyPref.summary = prefs.altimeterSamples.toString()
        onClick(accuracyPref) {
            val idx = samples.indexOf(prefs.altimeterSamples)
            Pickers.item(
                requireContext(),
                getString(R.string.samples),
                samples.map { it.toString() },
                idx
            ) {
                if (it != null) {
                    prefs.altimeterSamples = samples[it]
                    accuracyPref.summary = samples[it].toString()
                    restartAltimeter()
                }
            }
        }

        onClick(clearCachePref) {
            FusedAltimeter.clearCachedCalibration(PreferencesSubsystem.getInstance(requireContext()).preferences)
            toast(getString(R.string.done))
        }

        onClick(continuousCalibrationPref) {
            restartAltimeter()
        }

        onClick(forceCalibrationPref) {
            updateForceCalibrationInterval()
        }

        onClick(demPref) {
            loadDEM()
        }

        onClick(clearDemPref) {
            clearDEM()
        }

        updateForceCalibrationIntervalSummary()

        // Update the altitude override to the current altitude
        if (prefs.altimeterMode == UserPreferences.AltimeterMode.Barometer) {
            updateAltitudeOverride()
        }

        lastMode = prefs.altimeterMode
    }

    private fun updateConditionalPreferences() {
        val hasBarometer = prefs.weather.hasBarometer
        val mode = prefs.altimeterMode
        val isFused =
            hasBarometer && (mode == UserPreferences.AltimeterMode.GPSBarometer || mode == UserPreferences.AltimeterMode.DigitalElevationModelBarometer)

        // Cache, continuous calibration, and force calibration interval are only available on the fused barometer
        clearCachePref.isVisible = isFused
        continuousCalibrationPref.isVisible = isFused
        forceCalibrationPref.isVisible = isFused

        // Overrides are available on the barometer or the manual mode
        val canProvideOverrides =
            mode == UserPreferences.AltimeterMode.Barometer || mode == UserPreferences.AltimeterMode.Override
        altitudeOverridePref.isVisible = canProvideOverrides
        altitudeOverrideGpsBtn.isVisible = canProvideOverrides
        altitudeOverrideBarometerEdit.isVisible = canProvideOverrides && hasBarometer

        // Sample size is only available when the GPS is being used
        accuracyPref.isVisible =
            mode == UserPreferences.AltimeterMode.GPS || mode == UserPreferences.AltimeterMode.GPSBarometer

        val isModeDem = mode == UserPreferences.AltimeterMode.DigitalElevationModel ||
                mode == UserPreferences.AltimeterMode.DigitalElevationModelBarometer

        demPref.isVisible = isModeDem
        inBackground {
            val version = DEMRepo.getInstance().getVersion()
            onMain {
                demPref.summary = version ?: getString(R.string.built_in_dem)
            }
        }
        clearDemPref.isVisible = isModeDem && DEM.isExternalModel()

        // Calibration mode options
        val options = listOfNotNull(
            if (hasBarometer) getString(R.string.altimeter_mode_gps_barometer) to "gps_barometer" else null,
            getString(R.string.gps) to "gps",
            if (hasBarometer) getString(R.string.barometer) to "barometer" else null,
            if (hasBarometer) getString(R.string.altimeter_mode_dem_barometer) to "dem_barometer" else null,
            getString(R.string.digital_elevation_model_abbreviation) to "dem",
            getString(R.string.manual) to "override"
        )

        calibrationModeList.entries = options.map { it.first }.toTypedArray()
        calibrationModeList.entryValues = options.map { it.second }.toTypedArray()
        // Save the default value
        if (AppServiceRegistry.get<PreferencesSubsystem>().preferences.getString(calibrationModeList.key) == null) {
            calibrationModeList.value = if (hasBarometer) "gps_barometer" else "gps"
        }

    }

    private fun restartAltimeter() {
        stopAltimeter()
        altimeter = sensorService.getAltimeter()
        startAltimeter()
    }

    override fun onResume() {
        super.onResume()
        startAltimeter()
        updateTimer.interval(200)
    }

    override fun onPause() {
        super.onPause()
        stopAltimeter()
        updateTimer.stop()
        overridePopulationRunner.cancel()
    }

    private fun startAltimeter() {
        if (altimeterStarted) {
            return
        }
        altimeterStarted = true
        altimeter.start(this::updateAltitude)
    }

    private fun stopAltimeter() {
        altimeterStarted = false
        altimeter.stop(this::updateAltitude)
    }

    private fun setOverrideFromBarometer(seaLevelPressure: Float) {
        inBackground {
            onDefault {
                overridePopulationRunner.replace {
                    barometer.read()
                    val elevation = Geology.getAltitude(
                        Pressure.hpa(barometer.pressure),
                        Pressure.hpa(seaLevelPressure)
                    )

                    onIO {
                        prefs.altitudeOverride = elevation.meters().distance
                        prefs.seaLevelPressureOverride = seaLevelPressure
                    }

                    onMain {
                        restartAltimeter()
                        toast(getString(R.string.elevation_override_updated_toast))
                    }
                }
            }
        }
    }

    private fun setOverrideFromGPS() {
        inBackground {
            onDefault {
                overridePopulationRunner.replace {
                    readAll(listOf(gps, barometer))
                    val elevation = gps.altitude
                    val seaLevelPressure = Meteorology.getSeaLevelPressure(
                        Pressure.hpa(barometer.pressure),
                        Distance.meters(elevation)
                    )

                    onIO {
                        prefs.altitudeOverride = elevation
                        prefs.seaLevelPressureOverride = seaLevelPressure.pressure
                    }

                    onMain {
                        restartAltimeter()
                        toast(getString(R.string.elevation_override_updated_toast))
                    }
                }
            }
        }
    }

    private fun updateAltitudeOverride() {
        inBackground {
            onDefault {
                overridePopulationRunner.replace {
                    altimeter.read()
                    prefs.altitudeOverride = altimeter.altitude
                }
            }
        }
    }

    private fun onAltimeterModeChanged() {
        lastMode = prefs.altimeterMode
        restartAltimeter()
        updateConditionalPreferences()

        // Update the altitude override to the current altitude
        if (prefs.altimeterMode == UserPreferences.AltimeterMode.Barometer) {
            updateAltitudeOverride()
        }
    }

    private fun updateForceCalibrationInterval() {
        CustomUiUtils.pickDuration(
            requireContext(),
            prefs.altimeter.fusedAltimeterForcedRecalibrationInterval,
            getString(R.string.fused_altimeter_force_calibration)
        ) {
            if (it != null) {
                prefs.altimeter.fusedAltimeterForcedRecalibrationInterval = it
                updateForceCalibrationIntervalSummary()
            }
        }
    }

    private fun updateForceCalibrationIntervalSummary() {
        val interval =
            formatService.formatDuration(prefs.altimeter.fusedAltimeterForcedRecalibrationInterval)
        forceCalibrationPref.summary =
            getString(R.string.fused_altimeter_force_calibration_summary, interval)
    }

    private fun updateAltitude(): Boolean {
        if (throttle.isThrottled()) {
            return true
        }

        val altitude = Distance.meters(altimeter.altitude).convertTo(distanceUnits)
        altitudeTxt.summary = formatService.formatDistance(altitude)

        if (lastMode != prefs.altimeterMode) {
            onAltimeterModeChanged()
        }

        val altitudeOverride = Distance.meters(prefs.altitudeOverride).convertTo(distanceUnits)
        altitudeOverridePref.summary = formatService.formatDistance(altitudeOverride)

        return true
    }

    private fun setAltitudeOverride(elevation: Distance) {
        inBackground {
            onDefault {
                overridePopulationRunner.replace {
                    if (prefs.altimeterMode == UserPreferences.AltimeterMode.Barometer) {
                        // Calculate sea level pressure from the new elevation
                        barometer.read()
                        val seaLevelPressure = Meteorology.getSeaLevelPressure(
                            Pressure.hpa(barometer.pressure),
                            elevation.meters()
                        )
                        prefs.altitudeOverride = elevation.meters().distance
                        prefs.seaLevelPressureOverride = seaLevelPressure.pressure
                    } else {
                        prefs.altitudeOverride = elevation.meters().distance
                    }

                    onMain {
                        restartAltimeter()
                    }
                }
            }
        }
    }

    private fun loadDEM() {
        inBackground(state = BackgroundMinimumState.Created) {
            val instructions = getString(
                R.string.digital_elevation_model_link,
                URLs.DEM
            ).toSpannable()
            LinkifyCompat.addLinks(instructions, WEB_URLS)
            val cancelled = CoroutineAlerts.dialog(
                requireContext(), getString(R.string.plugin_digital_elevation_model),
                instructions, allowLinks = true
            )

            if (cancelled) {
                return@inBackground
            }

            val source = IntentUriPicker(
                this@CalibrateAltimeterFragment,
                requireContext()
            ).open(listOf("application/zip")) ?: return@inBackground

            val loader = DigitalElevationModelLoader()
            try {
                Alerts.withLoading(requireContext(), getString(R.string.loading)) {
                    loader.load(source)
                    onMain {
                        onAltimeterModeChanged()
                        toast(getString(R.string.loaded))
                    }
                }
            } catch (_: Exception) {
                onMain {
                    toast(getString(R.string.invalid_dem_file))
                }
            }
        }
    }

    private fun clearDEM() {
        inBackground {
            val cancelled = CoroutineAlerts.dialog(requireContext(), getString(R.string.remove_dem))
            if (cancelled) {
                return@inBackground
            }
            val loader = DigitalElevationModelLoader()
            Alerts.withLoading(requireContext(), getString(R.string.loading)) {
                loader.clear()
                onMain {
                    onAltimeterModeChanged()
                    toast(getString(R.string.done))
                }
            }
        }
    }

}