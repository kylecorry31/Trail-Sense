package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.SeekBarPreference
import com.kylecorry.andromeda.alerts.dialog
import com.kylecorry.andromeda.core.coroutines.ControlledRunner
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.sensors.IThermometer
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.toFloatCompat
import com.kylecorry.andromeda.core.topics.generic.asLiveData
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.diagnostics.DiagnosticCode
import com.kylecorry.trail_sense.diagnostics.GPSDiagnostic
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.alerts.AlertLoadingIndicator
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.thermometer.ThermometerSource
import com.kylecorry.trail_sense.shared.views.UserError
import com.kylecorry.trail_sense.weather.domain.RawWeatherObservation
import com.kylecorry.trail_sense.weather.domain.WeatherObservation
import com.kylecorry.trail_sense.weather.infrastructure.commands.BackfillHistoricalTemperaturesCommand
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem
import com.kylecorry.trail_sense.weather.ui.charts.TemperatureChartPreference
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

class ThermometerSettingsFragment : AndromedaPreferenceFragment() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private var calibratedThermometer: IThermometer? = null
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val temperatureUnits by lazy { prefs.temperatureUnits }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }

    private var temperatureTxt: Preference? = null
    private var minTempCalibratedC: EditTextPreference? = null
    private var maxTempCalibratedC: EditTextPreference? = null
    private var minTempUncalibratedC: EditTextPreference? = null
    private var maxTempUncalibratedC: EditTextPreference? = null
    private var minTempCalibratedF: EditTextPreference? = null
    private var maxTempCalibratedF: EditTextPreference? = null
    private var minTempUncalibratedF: EditTextPreference? = null
    private var maxTempUncalibratedF: EditTextPreference? = null
    private var smoothingSeekBar: SeekBarPreference? = null
    private var chart: TemperatureChartPreference? = null
    private var backfillPref: Preference? = null

    private val weather by lazy { WeatherSubsystem.getInstance(requireContext()) }
    private var history: List<WeatherObservation> = emptyList()
    private var uncalibratedHistory: List<Reading<RawWeatherObservation>> = emptyList()

    private val runner = ControlledRunner<Unit>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.thermometer_settings, rootKey)
        setIconColor(Resources.androidTextColorSecondary(requireContext()))

        // TODO: No need for multiple units, use a UnitPreference
        minTempCalibratedC = editText(R.string.pref_min_calibrated_temp_c)
        maxTempCalibratedC = editText(R.string.pref_max_calibrated_temp_c)
        minTempUncalibratedC = editText(R.string.pref_min_uncalibrated_temp_c)
        maxTempUncalibratedC = editText(R.string.pref_max_uncalibrated_temp_c)
        minTempCalibratedF = editText(R.string.pref_min_calibrated_temp_f)
        maxTempCalibratedF = editText(R.string.pref_max_calibrated_temp_f)
        minTempUncalibratedF = editText(R.string.pref_min_uncalibrated_temp_f)
        maxTempUncalibratedF = editText(R.string.pref_max_uncalibrated_temp_f)
        temperatureTxt = preference(R.string.pref_temperature_holder)
        smoothingSeekBar = seekBar(R.string.pref_temperature_smoothing)
        chart = findPreference(getString(R.string.pref_holder_temperature_chart))
        backfillPref = preference(R.string.pref_backfill_temperatures)

        smoothingSeekBar?.summary =
            formatService.formatPercentage(prefs.thermometer.smoothing * 100)

        smoothingSeekBar?.setOnPreferenceChangeListener { _, newValue ->
            val change = newValue.toString().toFloat() / 1000f
            smoothingSeekBar?.summary = formatService.formatPercentage(change * 100)
            true
        }

        if (prefs.temperatureUnits == TemperatureUnits.C) {
            minTempCalibratedF?.isVisible = false
            maxTempCalibratedF?.isVisible = false
            minTempUncalibratedF?.isVisible = false
            maxTempUncalibratedF?.isVisible = false
            minTempCalibratedC?.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as String).toFloatCompat() ?: 0f
                prefs.thermometer.minActualTemperatureF =
                    Temperature(temp, TemperatureUnits.C).convertTo(TemperatureUnits.F).temperature
                true
            }
            minTempUncalibratedC?.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as String).toFloatCompat() ?: 0f
                prefs.thermometer.minBatteryTemperatureF =
                    Temperature(temp, TemperatureUnits.C).convertTo(TemperatureUnits.F).temperature
                true
            }
            maxTempCalibratedC?.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as String).toFloatCompat() ?: 0f
                prefs.thermometer.maxActualTemperatureF =
                    Temperature(temp, TemperatureUnits.C).convertTo(TemperatureUnits.F).temperature
                true
            }
            maxTempUncalibratedC?.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as String).toFloatCompat() ?: 0f
                prefs.thermometer.maxBatteryTemperatureF =
                    Temperature(temp, TemperatureUnits.C).convertTo(TemperatureUnits.F).temperature
                true
            }
        } else {
            minTempCalibratedC?.isVisible = false
            maxTempCalibratedC?.isVisible = false
            minTempUncalibratedC?.isVisible = false
            maxTempUncalibratedC?.isVisible = false
            minTempCalibratedF?.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as String).toFloatCompat() ?: 0f
                prefs.thermometer.minActualTemperature =
                    Temperature(temp, TemperatureUnits.F).celsius().temperature
                true
            }
            minTempUncalibratedF?.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as String).toFloatCompat() ?: 0f
                prefs.thermometer.minBatteryTemperature =
                    Temperature(temp, TemperatureUnits.F).celsius().temperature
                true
            }
            maxTempCalibratedF?.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as String).toFloatCompat() ?: 0f
                prefs.thermometer.maxActualTemperature =
                    Temperature(temp, TemperatureUnits.F).celsius().temperature
                true
            }
            maxTempUncalibratedF?.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as String).toFloatCompat() ?: 0f
                prefs.thermometer.maxBatteryTemperature =
                    Temperature(temp, TemperatureUnits.F).celsius().temperature
                true
            }
        }

        backfillPref?.isVisible = prefs.thermometer.source == ThermometerSource.Historic
        onClick(backfillPref) {
            dialog(
                backfillPref?.title ?: "",
                getString(R.string.pref_backfill_temperatures_description)
            ) {
                if (!it) {
                    inBackground {
                        val loading =
                            AlertLoadingIndicator(requireContext(), getString(R.string.updating))
                        loading.show()
                        BackfillHistoricalTemperaturesCommand.create(requireContext()).execute()
                        loading.hide()
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val source = getString(R.string.pref_thermometer_source)
        val thermometerInvalidationKeys = listOf(
            R.string.pref_min_calibrated_temp_c,
            R.string.pref_max_calibrated_temp_c,
            R.string.pref_min_uncalibrated_temp_c,
            R.string.pref_max_uncalibrated_temp_c
        ).map { getString(it) } + listOf(source)
        PreferencesSubsystem.getInstance(requireContext()).preferences.onChange.asLiveData()
            .observe(viewLifecycleOwner) {
                if (thermometerInvalidationKeys.contains(it)) {
                    reloadThermometer()
                }

                if (it == source) {
                    onSourceChanged()
                }
            }

        observe(weather.weatherChanged) {
            inBackground {
                runner.cancelPreviousThenRun {
                    history = weather.getHistory()
                    uncalibratedHistory = weather.getRawHistory()
                    onMain {
                        updateChart()
                    }
                }
            }
        }
    }

    private fun updateChart() {
        chart?.isVisible = history.isNotEmpty()

        val displayReadings = history.filter {
            Duration.between(
                it.time,
                Instant.now()
            ) <= prefs.weather.pressureHistory
        }.map {
            Reading(
                it.temperature.convertTo(temperatureUnits).temperature, it.time
            )
        }

        val displayRawReadings = uncalibratedHistory.filter {
            Duration.between(
                it.time,
                Instant.now()
            ) <= prefs.weather.pressureHistory
        }.map {
            Reading(
                Temperature.celsius(it.value.temperature).convertTo(temperatureUnits).temperature,
                it.time
            )
        }

        if (displayReadings.isNotEmpty()) {
            chart?.plot(displayReadings, displayRawReadings)
        }
    }

    private fun onSourceChanged() {
        val source = prefs.thermometer.source
        when (source) {
            ThermometerSource.Historic -> setSmoothing(0f)
            ThermometerSource.Sensor -> setSmoothing(0.2f)
        }
        backfillPref?.isVisible = source == ThermometerSource.Historic
        displayErrors(source)
        resetCalibration()
    }

    private fun resetCalibration() {
        prefs.thermometer.resetThermometerCalibration()
        minTempCalibratedC?.text =
            DecimalFormatter.format(prefs.thermometer.minActualTemperature, 1)
        maxTempCalibratedC?.text =
            DecimalFormatter.format(prefs.thermometer.maxActualTemperature, 1)
        minTempUncalibratedC?.text =
            DecimalFormatter.format(prefs.thermometer.minBatteryTemperature, 1)
        maxTempUncalibratedC?.text =
            DecimalFormatter.format(prefs.thermometer.maxBatteryTemperature, 1)
        minTempCalibratedF?.text =
            DecimalFormatter.format(prefs.thermometer.minActualTemperatureF, 1)
        maxTempCalibratedF?.text =
            DecimalFormatter.format(prefs.thermometer.maxActualTemperatureF, 1)
        minTempUncalibratedF?.text =
            DecimalFormatter.format(prefs.thermometer.minBatteryTemperatureF, 1)
        maxTempUncalibratedF?.text =
            DecimalFormatter.format(prefs.thermometer.maxBatteryTemperatureF, 1)
    }

    private fun setSmoothing(smoothing: Float) {
        smoothingSeekBar?.value = (smoothing * 1000).roundToInt()
        smoothingSeekBar?.summary = formatService.formatPercentage(smoothing * 100)
    }

    private fun reloadThermometer() {
        calibratedThermometer?.stop(this::onThermometerUpdate)
        calibratedThermometer = sensorService.getThermometer()
        calibratedThermometer?.start(this::onThermometerUpdate)
    }

    override fun onResume() {
        super.onResume()
        reloadThermometer()
        val source = prefs.thermometer.source
        displayErrors(source)
    }

    override fun onPause() {
        super.onPause()
        calibratedThermometer?.stop(this::onThermometerUpdate)
        hideLocationUnsetError()
    }

    private fun onThermometerUpdate(): Boolean {
        val temperature = calibratedThermometer?.temperature ?: return true
        temperatureTxt?.summary =
            formatService.formatTemperature(
                Temperature(
                    temperature,
                    TemperatureUnits.C
                ).convertTo(prefs.temperatureUnits)
            )
        return true
    }

    private fun displayErrors(source: ThermometerSource) {
        if (source == ThermometerSource.Historic && isLocationUnset()) {
            showLocationUnsetError()
        } else {
            hideLocationUnsetError()
        }
    }

    private fun hideLocationUnsetError() {
        val activity = requireMainActivity()
        activity.errorBanner.dismiss(ErrorBannerReason.LocationNotSet)
    }

    private fun showLocationUnsetError() {
        val activity = requireMainActivity()
        val navController = findNavController()
        val error = UserError(
            ErrorBannerReason.LocationNotSet,
            getString(R.string.location_not_set) + "\n" + getString(R.string.for_historic_temperatures),
            R.drawable.satellite,
            getString(R.string.set)
        ) {
            activity.errorBanner.dismiss(ErrorBannerReason.LocationNotSet)
            navController.navigate(R.id.calibrateGPSFragment)
        }
        activity.errorBanner.report(error)
    }

    private fun isLocationUnset(): Boolean {
        val diagnostic = GPSDiagnostic(requireContext(), null)
        return diagnostic.scan().contains(DiagnosticCode.LocationUnset)
    }

}