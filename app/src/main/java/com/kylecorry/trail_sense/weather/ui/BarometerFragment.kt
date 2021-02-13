package com.kylecorry.trail_sense.weather.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ActivityWeatherBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.formatHM
import com.kylecorry.trail_sense.shared.sensors.*
import com.kylecorry.trail_sense.weather.domain.*
import com.kylecorry.trail_sense.weather.domain.WeatherService
import com.kylecorry.trail_sense.weather.domain.sealevel.NullPressureConverter
import com.kylecorry.trail_sense.weather.infrastructure.persistence.PressureRepo
import com.kylecorry.trailsensecore.domain.units.PressureUnits
import com.kylecorry.trailsensecore.domain.units.UnitService
import com.kylecorry.trailsensecore.domain.weather.*
import com.kylecorry.trailsensecore.infrastructure.sensors.asLiveData
import com.kylecorry.trailsensecore.infrastructure.sensors.read
import com.kylecorry.trailsensecore.infrastructure.time.Throttle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

class BarometerFragment : Fragment() {

    private val barometer by lazy { sensorService.getBarometer() }
    private val altimeter by lazy { sensorService.getAltimeter() }
    private val thermometer by lazy { sensorService.getThermometer() }

    private var altitude = 0F
    private var useSeaLevelPressure = false
    private var units = PressureUnits.Hpa

    private val prefs by lazy { UserPreferences(requireContext()) }

    private var _binding: ActivityWeatherBinding? = null
    private val binding get() = _binding!!

    private lateinit var chart: PressureChart
    private lateinit var navController: NavController

    private lateinit var weatherService: WeatherService
    private val sensorService by lazy { SensorService(requireContext()) }
    private val unitService = UnitService()
    private val formatService by lazy { FormatService(requireContext()) }
    private val pressureRepo by lazy { PressureRepo.getInstance(requireContext()) }

    private val throttle = Throttle(20)

    private var pressureSetpoint: PressureAltitudeReading? = null

    private var readingHistory: List<PressureAltitudeReading> = listOf()

    private var valueSelectedTime = 0L

    private var leftQuickAction: QuickActionButton? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        leftQuickAction = QuickActionClouds(binding.weatherLeftQuickAction, this)
        leftQuickAction?.onCreate()

        navController = findNavController()

        weatherService = WeatherService(
            prefs.weather.stormAlertThreshold,
            prefs.weather.dailyForecastChangeThreshold,
            prefs.weather.hourlyForecastChangeThreshold,
            prefs.weather.seaLevelFactorInRapidChanges,
            prefs.weather.seaLevelFactorInTemp
        )

        chart = PressureChart(
            binding.chart,
            resources.getColor(R.color.colorPrimary, null),
            object : IPressureChartSelectedListener {
                override fun onNothingSelected() {
                    if (pressureSetpoint == null) {
                        binding.pressureMarker.text = ""
                    }
                }

                override fun onValueSelected(timeAgo: Duration, pressure: Float) {
                    val formatted = formatService.formatPressure(pressure, units)
                    binding.pressureMarker.text = getString(
                        R.string.pressure_reading_time_ago,
                        formatted,
                        timeAgo.formatHM(false)
                    )
                    valueSelectedTime = System.currentTimeMillis()
                }

            }
        )

        binding.temperatureBtn.setOnClickListener {
            navController.navigate(R.id.action_action_weather_to_thermometerFragment)
        }

        binding.pressure.setOnLongClickListener {
            pressureSetpoint = if (pressureSetpoint == null) {
                PressureAltitudeReading(
                    Instant.now(),
                    barometer.pressure,
                    altimeter.altitude,
                    thermometer.temperature
                )
            } else {
                null
            }

            prefs.weather.pressureSetpoint = pressureSetpoint

            pressureSetpoint?.let {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        pressureRepo.addPressure(PressureReadingEntity.from(it))
                    }
                }
            }

            true
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityWeatherBinding.inflate(inflater, container, false)

        pressureRepo.getPressures().observe(viewLifecycleOwner) {
            readingHistory = it.map { it.toPressureAltitudeReading() }.sortedBy { it.time }
        }

        barometer.asLiveData().observe(viewLifecycleOwner, { update() })
        thermometer.asLiveData().observe(viewLifecycleOwner, { update() })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        leftQuickAction?.onDestroy()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        leftQuickAction?.onResume()

        useSeaLevelPressure = prefs.weather.useSeaLevelPressure
        altitude = altimeter.altitude
        units = prefs.pressureUnits

        pressureSetpoint = prefs.weather.pressureSetpoint

        lifecycleScope.launch {
            withContext(Dispatchers.IO){
                if (!altimeter.hasValidReading) {
                    altimeter.read()
                }
            }
            withContext(Dispatchers.Main){
                altitude = altimeter.altitude
                update()
            }
        }

        update()
    }

    override fun onPause() {
        super.onPause()
        leftQuickAction?.onPause()
    }


    private fun update() {
        if (context == null) return
        if (barometer.pressure == 0.0f) return

        if (throttle.isThrottled()) {
            return
        }

        val readings = if (useSeaLevelPressure) {
            getSeaLevelPressureHistory()
        } else {
            getPressureHistory()
        }

        displayChart(readings)

        val setpoint = getSetpoint()
        val tendency = weatherService.getTendency(readings, setpoint)
        displayTendency(tendency)

        updateForecast(readings, setpoint)

        val pressure = getCurrentPressure()
        displayPressure(pressure)

        if (setpoint != null && System.currentTimeMillis() - valueSelectedTime > 2000) {
            displaySetpoint(setpoint)
        } else if (System.currentTimeMillis() - valueSelectedTime > 2000) {
            binding.pressureMarker.text = ""
        }
    }

    private fun displaySetpoint(setpoint: PressureReading) {
        val converted = convertPressure(setpoint)
        val formatted = formatService.formatPressure(converted.value, units)

        val timeAgo = Duration.between(setpoint.time, Instant.now())
        binding.pressureMarker.text = getString(
            R.string.pressure_setpoint_format,
            formatted,
            timeAgo.formatHM(true)
        )
    }

    private fun getSeaLevelPressureHistory(includeCurrent: Boolean = false): List<PressureReading> {
        val readings = getReadingHistory().toMutableList()
        if (includeCurrent) {
            readings.add(
                PressureAltitudeReading(
                    Instant.now(),
                    barometer.pressure,
                    altimeter.altitude,
                    thermometer.temperature
                )
            )
        }
        return weatherService.convertToSeaLevel(
            readings, prefs.weather.requireDwell, prefs.weather.maxNonTravellingAltitudeChange,
            prefs.weather.maxNonTravellingPressureChange
        )
    }

    private fun getPressureHistory(includeCurrent: Boolean = false): List<PressureReading> {
        val readings = getReadingHistory().toMutableList()
        if (includeCurrent) {
            readings.add(
                PressureAltitudeReading(
                    Instant.now(),
                    barometer.pressure,
                    altimeter.altitude,
                    thermometer.temperature
                )
            )
        }
        return NullPressureConverter().convert(readings)
    }

    private fun displayChart(readings: List<PressureReading>) {
        val displayReadings = readings.filter {
            Duration.between(
                it.time,
                Instant.now()
            ) <= prefs.weather.pressureHistory
        }

        if (displayReadings.size >= 2) {
            val totalTime = Duration.between(
                displayReadings.first().time, displayReadings.last().time
            )
            var hours = totalTime.toHours()
            val minutes = totalTime.toMinutes() % 60

            when (hours) {
                0L -> binding.pressureHistoryDuration.text = context?.resources?.getQuantityString(
                    R.plurals.last_minutes,
                    minutes.toInt(),
                    minutes
                )
                else -> {
                    if (minutes >= 30) hours++
                    binding.pressureHistoryDuration.text =
                        context?.resources?.getQuantityString(
                            R.plurals.last_hours,
                            hours.toInt(),
                            hours
                        )
                }
            }

        }

        if (displayReadings.isNotEmpty()) {
            chart.setUnits(units)

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

            chart.plot(chartData)
        }
    }

    private fun displayTendency(tendency: PressureTendency) {
        val converted = convertPressure(PressureReading(Instant.now(), tendency.amount))
        val formatted = formatService.formatPressure(converted.value, units)
        binding.tendencyAmount.text =
            getString(R.string.pressure_tendency_format_2, formatted)

        when (tendency.characteristic) {
            PressureCharacteristic.Falling, PressureCharacteristic.FallingFast -> {
                binding.barometerTrend.setImageResource(R.drawable.ic_arrow_down)
                binding.barometerTrend.visibility = View.VISIBLE
            }
            PressureCharacteristic.Rising, PressureCharacteristic.RisingFast -> {
                binding.barometerTrend.setImageResource(R.drawable.ic_arrow_up)
                binding.barometerTrend.visibility = View.VISIBLE
            }
            else -> binding.barometerTrend.visibility = View.INVISIBLE
        }
    }

    private fun updateForecast(readings: List<PressureReading>, setpoint: PressureReading?) {
        val shortTerm = weatherService.getHourlyWeather(readings, setpoint)
        val longTerm = weatherService.getDailyWeather(readings)

        binding.weatherNowLbl.text = getShortTermWeatherDescription(shortTerm)
        binding.weatherNowImg.setImageResource(
            getWeatherImage(
                shortTerm,
                readings.lastOrNull() ?: PressureReading(Instant.now(), barometer.pressure)
            )
        )
        binding.weatherLaterLbl.text = getLongTermWeatherDescription(longTerm)
    }

    private fun getSetpoint(): PressureReading? {
        val setpoint = pressureSetpoint
        return if (useSeaLevelPressure) {
            setpoint?.seaLevel(prefs.weather.seaLevelFactorInTemp)
        } else {
            setpoint?.pressureReading()
        }
    }

    private fun getCurrentPressure(): PressureReading {
        val pressures = if (useSeaLevelPressure) {
            getSeaLevelPressureHistory(true)
        } else {
            getPressureHistory(true)
        }

        return when {
            pressures.isNotEmpty() -> {
                pressures.last()
            }
            useSeaLevelPressure -> {
                PressureAltitudeReading(
                    Instant.now(),
                    barometer.pressure,
                    altimeter.altitude,
                    thermometer.temperature
                ).seaLevel(prefs.weather.seaLevelFactorInTemp)
            }
            else -> {
                PressureReading(Instant.now(), barometer.pressure)
            }
        }
    }

    private fun displayPressure(pressure: PressureReading) {
        val formatted = formatService.formatPressure(convertPressure(pressure).value, units)
        binding.pressure.text = formatted
    }

    private fun convertPressure(pressure: PressureReading): PressureReading {
        val converted = unitService.convert(pressure.value, PressureUnits.Hpa, units)
        return pressure.copy(value = converted)
    }

    private fun getReadingHistory(): List<PressureAltitudeReading> {
        return readingHistory
    }

    private fun getWeatherImage(weather: Weather, currentPressure: PressureReading): Int {
        return when (weather) {
            Weather.ImprovingFast -> if (currentPressure.isLow()) R.drawable.cloudy else R.drawable.sunny
            Weather.ImprovingSlow -> if (currentPressure.isHigh()) R.drawable.sunny else R.drawable.partially_cloudy
            Weather.WorseningSlow -> if (currentPressure.isLow()) R.drawable.light_rain else R.drawable.cloudy
            Weather.WorseningFast -> if (currentPressure.isLow()) R.drawable.heavy_rain else R.drawable.light_rain
            Weather.Storm -> R.drawable.storm
            else -> R.drawable.steady
        }
    }

    private fun getShortTermWeatherDescription(weather: Weather): String {
        return when (weather) {
            Weather.ImprovingFast -> getString(R.string.weather_improving_fast)
            Weather.ImprovingSlow -> getString(R.string.weather_improving_slow)
            Weather.WorseningSlow -> getString(R.string.weather_worsening_slow)
            Weather.WorseningFast -> getString(R.string.weather_worsening_fast)
            Weather.Storm -> getString(R.string.weather_storm_incoming)
            else -> getString(R.string.weather_not_changing)
        }
    }

    private fun getLongTermWeatherDescription(weather: Weather): String {
        return when (weather) {
            Weather.ImprovingFast, Weather.ImprovingSlow -> getString(R.string.forecast_improving)
            Weather.WorseningSlow, Weather.WorseningFast, Weather.Storm -> getString(R.string.forecast_worsening)
            else -> ""
        }
    }

}
