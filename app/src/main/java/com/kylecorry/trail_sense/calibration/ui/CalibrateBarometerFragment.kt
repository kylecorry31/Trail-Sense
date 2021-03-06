package com.kylecorry.trail_sense.calibration.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.preference.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.CustomPreferenceFragment
import com.kylecorry.trail_sense.settings.ui.PressureChartPreference
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.weather.domain.PressureUnitUtils
import com.kylecorry.trail_sense.weather.domain.WeatherService
import com.kylecorry.trail_sense.weather.infrastructure.PressureCalibrationUtils
import com.kylecorry.trail_sense.weather.infrastructure.WeatherContextualService
import com.kylecorry.trail_sense.weather.infrastructure.persistence.PressureRepo
import com.kylecorry.trailsensecore.domain.units.PressureUnits
import com.kylecorry.trailsensecore.domain.units.UnitService
import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import com.kylecorry.trailsensecore.infrastructure.sensors.altimeter.IAltimeter
import com.kylecorry.trailsensecore.infrastructure.sensors.barometer.IBarometer
import com.kylecorry.trailsensecore.infrastructure.sensors.gps.IGPS
import com.kylecorry.trailsensecore.infrastructure.sensors.temperature.IThermometer
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.time.Throttle
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

class CalibrateBarometerFragment : CustomPreferenceFragment() {

    private lateinit var prefs: UserPreferences
    private lateinit var sensorService: SensorService
    private val throttle = Throttle(20)

    private var pressureTxt: Preference? = null
    private var seaLevelSwitch: SwitchPreferenceCompat? = null
    private var altitudeChangeSeekBar: SeekBarPreference? = null
    private var pressureChangeSeekBar: SeekBarPreference? = null
    private var altitudeOutlierSeekBar: SeekBarPreference? = null
    private var pressureSmoothingSeekBar: SeekBarPreference? = null
    private var altitudeSmoothingSeekBar: SeekBarPreference? = null
    private var experimentalCalibrationSwitch: SwitchPreferenceCompat? = null

    private var chart: PressureChartPreference? = null

    private val weatherForecastService by lazy { WeatherContextualService.getInstance(requireContext()) }

    private var readingHistory: List<PressureAltitudeReading> = listOf()

    private val sensorChecker by lazy { SensorChecker(requireContext()) }
    private lateinit var barometer: IBarometer
    private lateinit var altimeter: IAltimeter
    private lateinit var thermometer: IThermometer

    private lateinit var weatherService: WeatherService
    private lateinit var units: PressureUnits
    private val formatService by lazy { FormatService(requireContext()) }
    private val unitService = UnitService()

    private val pressureRepo by lazy { PressureRepo.getInstance(requireContext()) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.barometer_calibration, rootKey)

        setIconColor(UiUtils.androidTextColorSecondary(requireContext()))

        prefs = UserPreferences(requireContext())
        sensorService = SensorService(requireContext())
        units = prefs.pressureUnits

        barometer = sensorService.getBarometer()
        altimeter = sensorService.getGPSAltimeter()
        thermometer = sensorService.getThermometer()

        bindPreferences()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pressureRepo.getPressures().observe(viewLifecycleOwner) {
            readingHistory = it.map { it.toPressureAltitudeReading() }.sortedBy { it.time }
                .filter { it.time <= Instant.now() }
        }
    }

    private fun refreshWeatherService() {
        weatherService = WeatherService(
            prefs.weather.stormAlertThreshold,
            prefs.weather.dailyForecastChangeThreshold,
            prefs.weather.hourlyForecastChangeThreshold,
            prefs.weather.seaLevelFactorInRapidChanges,
            prefs.weather.seaLevelFactorInTemp
        )
        lifecycleScope.launch {
            WeatherContextualService.getInstance(requireContext()).setDataChanged()
        }
    }

    private fun bindPreferences() {
        experimentalCalibrationSwitch = switch(R.string.pref_experimental_barometer_calibration)
        altitudeOutlierSeekBar = seekBar(R.string.pref_barometer_altitude_outlier)
        pressureSmoothingSeekBar = seekBar(R.string.pref_barometer_pressure_smoothing)
        altitudeSmoothingSeekBar = seekBar(R.string.pref_barometer_altitude_smoothing)

        pressureTxt = findPreference(getString(R.string.pref_holder_pressure))
        seaLevelSwitch = findPreference(getString(R.string.pref_use_sea_level_pressure))
        altitudeChangeSeekBar = findPreference(getString(R.string.pref_barometer_altitude_change))
        chart = findPreference(getString(R.string.pref_holder_pressure_chart))
        pressureChangeSeekBar =
            findPreference(getString(R.string.pref_sea_level_pressure_change_thresh))

        experimentalCalibrationSwitch?.setOnPreferenceClickListener {
            refreshWeatherService()
            update()
            true
        }

        altitudeChangeSeekBar?.summary =
            (if (prefs.weather.maxNonTravellingAltitudeChange == 0f) "" else "± ") + formatService.formatSmallDistance(
                prefs.weather.maxNonTravellingAltitudeChange
            )

        pressureChangeSeekBar?.summary =
            (if (prefs.weather.maxNonTravellingPressureChange == 0f) "" else "± ") + getString(
                R.string.pressure_tendency_format_2, formatService.formatPressure(
                    unitService.convert(
                        prefs.weather.maxNonTravellingPressureChange,
                        PressureUnits.Hpa,
                        prefs.pressureUnits
                    ), prefs.pressureUnits
                )
            )

        altitudeOutlierSeekBar?.summary =
            (if (prefs.weather.altitudeOutlier == 0f) "" else "± ") + formatService.formatSmallDistance(
                prefs.weather.altitudeOutlier
            )

        pressureSmoothingSeekBar?.summary = formatService.formatPercentage(prefs.weather.pressureSmoothing.toInt())
        altitudeSmoothingSeekBar?.summary = formatService.formatPercentage(prefs.weather.altitudeSmoothing.toInt())



        seaLevelSwitch?.setOnPreferenceClickListener {
            if (!altimeter.hasValidReading) {
                altimeter.start(this::updateAltitude)
            }
            lifecycleScope.launch {
                weatherForecastService.setDataChanged()
            }
            refreshWeatherService()
            true
        }

        altitudeChangeSeekBar?.updatesContinuously = true
        altitudeChangeSeekBar?.setOnPreferenceChangeListener { _, newValue ->
            altitudeChangeSeekBar?.summary =
                (if (newValue.toString()
                        .toFloat() == 0f
                ) "" else "± ") + formatService.formatSmallDistance(
                    newValue.toString().toFloat()
                )
            true
        }

        pressureChangeSeekBar?.updatesContinuously = true
        pressureChangeSeekBar?.setOnPreferenceChangeListener { _, newValue ->
            val change = 20 * newValue.toString().toFloat() / 200f
            pressureChangeSeekBar?.summary =
                (if (change == 0f) "" else "± ") + getString(
                    R.string.pressure_tendency_format_2, formatService.formatPressure(
                        unitService.convert(
                            change,
                            PressureUnits.Hpa,
                            prefs.pressureUnits
                        ), prefs.pressureUnits
                    )
                )
            true
        }

        altitudeOutlierSeekBar?.updatesContinuously = true
        altitudeOutlierSeekBar?.setOnPreferenceChangeListener { _, newValue ->
            altitudeOutlierSeekBar?.summary =
                (if (newValue.toString()
                        .toFloat() == 0f
                ) "" else "± ") + formatService.formatSmallDistance(
                    newValue.toString().toFloat()
                )
            true
        }

        pressureSmoothingSeekBar?.updatesContinuously = true
        pressureSmoothingSeekBar?.setOnPreferenceChangeListener { _, newValue ->
            val change = 100 * newValue.toString().toFloat() / 1000f
            pressureSmoothingSeekBar?.summary = formatService.formatPercentage(change.toInt())
            true
        }

        altitudeSmoothingSeekBar?.updatesContinuously = true
        altitudeSmoothingSeekBar?.setOnPreferenceChangeListener { _, newValue ->
            val change = 100 * newValue.toString().toFloat() / 1000f
            altitudeSmoothingSeekBar?.summary = formatService.formatPercentage(change.toInt())
            true
        }

        preference(R.string.pref_barometer_info_holder)?.icon?.setTint(
            UiUtils.getAndroidColorAttr(
                requireContext(),
                android.R.attr.textColorSecondary
            )
        )

    }

    private fun updateChart() {
        val readings = PressureCalibrationUtils.calibratePressures(requireContext(), readingHistory)
        val displayReadings = readings.filter {
            Duration.between(
                it.time,
                Instant.now()
            ) <= prefs.weather.pressureHistory
        }
        if (displayReadings.isNotEmpty()) {
            chart?.setUnits(units)

            val chartData = displayReadings.map {
                val timeAgo = Duration.between(Instant.now(), it.time).seconds / (60f * 60f)
                Pair(
                    timeAgo as Number,
                    (PressureUnitUtils.convert(
                        it.value,
                        units
                    )) as Number
                )
            }

            chart?.plot(chartData)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshWeatherService()
        startBarometer()
        thermometer.start(this::updateTemperature)
        if (prefs.weather.useSeaLevelPressure && !altimeter.hasValidReading) {
            altimeter.start(this::updateAltitude)
        }
    }

    override fun onPause() {
        super.onPause()
        stopBarometer()
        altimeter.stop(this::updateAltitude)
        thermometer.stop(this::updateTemperature)
    }

    private fun updateAltitude(): Boolean {
        update()
        return false
    }

    private fun updateTemperature(): Boolean {
        update()
        return true
    }

    private fun startBarometer() {
        barometer.start(this::onPressureUpdate)
    }

    private fun stopBarometer() {
        barometer.stop(this::onPressureUpdate)
    }


    private fun onPressureUpdate(): Boolean {
        update()
        return true
    }

    private fun update() {

        if (throttle.isThrottled()) {
            return
        }

        updateChart()

        val isOnTheWallMode = prefs.altimeterMode == UserPreferences.AltimeterMode.Override || !sensorChecker.hasGPS()

        val seaLevelPressure = prefs.weather.useSeaLevelPressure

        val experimentalCalibration = prefs.weather.useExperimentalCalibration

        experimentalCalibrationSwitch?.isVisible = !isOnTheWallMode
        altitudeOutlierSeekBar?.isVisible = experimentalCalibration && !isOnTheWallMode
        pressureSmoothingSeekBar?.isVisible = experimentalCalibration && !isOnTheWallMode
        altitudeSmoothingSeekBar?.isVisible = experimentalCalibration && !isOnTheWallMode
        altitudeChangeSeekBar?.isVisible = !experimentalCalibration && !isOnTheWallMode
        pressureChangeSeekBar?.isVisible = !experimentalCalibration && !isOnTheWallMode
        switch(R.string.pref_sea_level_use_rapid)?.isVisible = !experimentalCalibration && !isOnTheWallMode
        switch(R.string.pref_sea_level_require_dwell)?.isVisible = !experimentalCalibration && !isOnTheWallMode


        val pressure = if (seaLevelPressure) {
            WeatherContextualService.getInstance(requireContext()).getSeaLevelPressure(
                PressureAltitudeReading(
                    Instant.now(),
                    barometer.pressure,
                    altimeter.altitude,
                    thermometer.temperature,
                    if (altimeter is IGPS) (altimeter as IGPS).verticalAccuracy else null
                ), readingHistory
            ).value
        } else {
            barometer.pressure
        }

        pressureTxt?.summary =
            formatService.formatPressure(PressureUnitUtils.convert(pressure, units), units)
    }


}