package com.kylecorry.trail_sense.calibration.ui

import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.kylecorry.andromeda.core.coroutines.ControlledRunner
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.core.topics.asLiveData
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.location.GPS
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.PressureChartPreference
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.extensions.isDebug
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.weather.domain.RawWeatherObservation
import com.kylecorry.trail_sense.weather.infrastructure.WeatherObservation
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem
import java.time.Duration
import java.time.Instant

class CalibrateBarometerFragment : AndromedaPreferenceFragment() {

    private lateinit var prefs: UserPreferences
    private lateinit var sensorService: SensorService
    private val throttle = Throttle(20)
    private val updateTimer = Timer {
        update()
    }

    private var pressureTxt: Preference? = null
    private var seaLevelSwitch: SwitchPreferenceCompat? = null
    private var meanShiftedSwitch: SwitchPreferenceCompat? = null
    private var altitudeOutlierSeekBar: SeekBarPreference? = null
    private var pressureSmoothingSeekBar: SeekBarPreference? = null
    private var altitudeSmoothingSeekBar: SeekBarPreference? = null

    private var chart: PressureChartPreference? = null

    private var history: List<WeatherObservation> = listOf()
    private var uncalibratedHistory: List<Reading<RawWeatherObservation>> = listOf()
    private var showMeanShiftedReadings = false

    private lateinit var units: PressureUnits
    private val formatService by lazy { FormatService(requireContext()) }

    private val weatherSubsystem by lazy { WeatherSubsystem.getInstance(requireContext()) }

    private val runner = ControlledRunner<Unit>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.barometer_calibration, rootKey)

        setIconColor(Resources.androidTextColorSecondary(requireContext()))

        prefs = UserPreferences(requireContext())
        sensorService = SensorService(requireContext())
        units = prefs.pressureUnits

        bindPreferences()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        weatherSubsystem.weatherChanged.asLiveData().observe(viewLifecycleOwner) {
            inBackground {
                runner.cancelPreviousThenRun {
                    history = weatherSubsystem.getHistory()
                    uncalibratedHistory = weatherSubsystem.getRawHistory()
                    onMain {
                        updateChart()
                    }
                }
            }
        }
    }

    private fun bindPreferences() {
        altitudeOutlierSeekBar = seekBar(R.string.pref_barometer_altitude_outlier)
        pressureSmoothingSeekBar = seekBar(R.string.pref_barometer_pressure_smoothing)
        altitudeSmoothingSeekBar = seekBar(R.string.pref_barometer_altitude_smoothing)

        pressureTxt = findPreference(getString(R.string.pref_holder_pressure))
        seaLevelSwitch = findPreference(getString(R.string.pref_use_sea_level_pressure))
        meanShiftedSwitch = switch(R.string.pref_debug_show_mean_adj_sea_level)
        chart = findPreference(getString(R.string.pref_holder_pressure_chart))

        altitudeOutlierSeekBar?.summary =
            (if (prefs.weather.altitudeOutlier == 0f) "" else "± ") + formatSmallDistance(
                prefs.weather.altitudeOutlier
            )

        pressureSmoothingSeekBar?.summary =
            formatService.formatPercentage(prefs.weather.pressureSmoothing)
        altitudeSmoothingSeekBar?.summary =
            formatService.formatPercentage(prefs.weather.altitudeSmoothing)

        showMeanShiftedReadings = meanShiftedSwitch?.isChecked ?: false
        meanShiftedSwitch?.setOnPreferenceClickListener {
            showMeanShiftedReadings = meanShiftedSwitch?.isChecked ?: false
            updateChart()
            true
        }

        altitudeOutlierSeekBar?.updatesContinuously = true
        altitudeOutlierSeekBar?.setOnPreferenceChangeListener { _, newValue ->
            altitudeOutlierSeekBar?.summary =
                (if (newValue.toString()
                        .toFloat() == 0f
                ) "" else "± ") + formatSmallDistance(
                    newValue.toString().toFloat()
                )
            true
        }

        pressureSmoothingSeekBar?.updatesContinuously = true
        pressureSmoothingSeekBar?.setOnPreferenceChangeListener { _, newValue ->
            val change = 100 * newValue.toString().toFloat() / 1000f
            pressureSmoothingSeekBar?.summary = formatService.formatPercentage(change)
            true
        }

        altitudeSmoothingSeekBar?.updatesContinuously = true
        altitudeSmoothingSeekBar?.setOnPreferenceChangeListener { _, newValue ->
            val change = 100 * newValue.toString().toFloat() / 1000f
            altitudeSmoothingSeekBar?.summary = formatService.formatPercentage(change)
            true
        }

        preference(R.string.pref_barometer_info_holder)?.icon?.setTint(
            Resources.getAndroidColorAttr(
                requireContext(),
                android.R.attr.textColorSecondary
            )
        )

    }

    private fun formatSmallDistance(meters: Float): String {
        val distance = Distance.meters(meters).convertTo(prefs.baseDistanceUnits)
        return formatService.formatDistance(distance)
    }

    private fun updateChart() {
        val displayReadings = history.filter {
            Duration.between(
                it.time,
                Instant.now()
            ) <= prefs.weather.pressureHistory
        }.map { it.pressureReading() }

        val averageAltitude =
            if (!showMeanShiftedReadings) 0f else uncalibratedHistory.map { it.value.altitude }
                .average().toFloat()

        val displayRawReadings = uncalibratedHistory.filter {
            Duration.between(
                it.time,
                Instant.now()
            ) <= prefs.weather.pressureHistory
        }.map {
            if (prefs.weather.useSeaLevelPressure) {
                if (showMeanShiftedReadings) {
                    val seaLevel = Meteorology.getSeaLevelPressure(
                        Pressure.hpa(it.value.pressure),
                        Distance.meters(averageAltitude)
                    )
                    Reading(seaLevel, it.time)
                } else {
                    Reading(it.value.seaLevel(false), it.time)
                }
            } else {
                Reading(Pressure.hpa(it.value.pressure), it.time)
            }
        }
        if (displayReadings.isNotEmpty()) {
            chart?.plot(displayReadings, displayRawReadings)
        }
    }

    override fun onResume() {
        super.onResume()
        updateTimer.interval(200)
    }

    override fun onPause() {
        super.onPause()
        updateTimer.stop()
    }

    private fun update() {
        // TODO: Only call this on change
        if (throttle.isThrottled()) {
            return
        }

        val isOnTheWallMode =
            prefs.altimeterMode == UserPreferences.AltimeterMode.Override || !GPS.isAvailable(
                requireContext()
            )

        val seaLevelPressure = prefs.weather.useSeaLevelPressure

        altitudeOutlierSeekBar?.isVisible =
            !isOnTheWallMode && !prefs.weather.useExperimentalSeaLevelCalibration && seaLevelPressure
        pressureSmoothingSeekBar?.isVisible = !isOnTheWallMode && seaLevelPressure
        altitudeSmoothingSeekBar?.isVisible =
            !isOnTheWallMode && seaLevelPressure && !prefs.weather.useExperimentalSeaLevelCalibration
        meanShiftedSwitch?.isVisible = isDebug()

        val pressure = history.lastOrNull()?.pressure ?: Pressure.hpa(0f)

        pressureTxt?.summary =
            formatService.formatPressure(
                pressure.convertTo(units),
                Units.getDecimalPlaces(units)
            )
    }

}