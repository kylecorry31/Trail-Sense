//package com.kylecorry.trail_sense.weather.ui
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.core.view.isInvisible
//import androidx.core.view.isVisible
//import androidx.lifecycle.lifecycleScope
//import androidx.navigation.NavController
//import androidx.navigation.fragment.findNavController
//import com.kylecorry.andromeda.alerts.toast
//import com.kylecorry.andromeda.core.sensors.asLiveData
//import com.kylecorry.andromeda.core.time.Throttle
//import com.kylecorry.andromeda.core.tryOrNothing
//import com.kylecorry.andromeda.fragments.BoundFragment
//import com.kylecorry.andromeda.location.IGPS
//import com.kylecorry.sol.science.meteorology.PressureCharacteristic
//import com.kylecorry.sol.science.meteorology.PressureTendency
//import com.kylecorry.sol.science.meteorology.Weather
//import com.kylecorry.sol.units.Pressure
//import com.kylecorry.sol.units.PressureUnits
//import com.kylecorry.trail_sense.R
//import com.kylecorry.trail_sense.databinding.ActivityWeatherBinding
//import com.kylecorry.trail_sense.quickactions.WeatherQuickActionBinder
//import com.kylecorry.trail_sense.shared.*
//import com.kylecorry.trail_sense.shared.sensors.SensorService
//import com.kylecorry.trail_sense.shared.views.UserError
//import com.kylecorry.trail_sense.weather.domain.PressureAltitudeReading
//import com.kylecorry.trail_sense.weather.domain.PressureReading
//import com.kylecorry.trail_sense.weather.domain.PressureUnitUtils
//import com.kylecorry.trail_sense.weather.domain.WeatherService
//import com.kylecorry.trail_sense.weather.infrastructure.WeatherContextualService
//import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler
//import com.kylecorry.trail_sense.weather.infrastructure.commands.MonitorWeatherCommand
//import com.kylecorry.trail_sense.weather.infrastructure.persistence.PressureRepo
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import java.time.Duration
//import java.time.Instant
//
//class OldWeatherFragment : BoundFragment<ActivityWeatherBinding>() {
//
//    private val barometer by lazy { sensorService.getBarometer() }
//    private val altimeter by lazy { sensorService.getGPSAltimeter() }
//    private val thermometer by lazy { sensorService.getThermometer() }
//
//    private var altitude = 0F
//    private var useSeaLevelPressure = false
//    private var units = PressureUnits.Hpa
//
//    private val prefs by lazy { UserPreferences(requireContext()) }
//
//    private lateinit var chart: PressureChart
//    private lateinit var navController: NavController
//
//    private lateinit var weatherService: WeatherService
//    private val sensorService by lazy { SensorService(requireContext()) }
//    private val formatService by lazy { FormatService(requireContext()) }
//    private val pressureRepo by lazy { PressureRepo.getInstance(requireContext()) }
//
//    private val throttle = Throttle(20)
//
//    private var readingHistory: List<PressureAltitudeReading> = listOf()
//
//    private var valueSelectedTime = 0L
//
//    private val weatherForecastService by lazy { WeatherContextualService.getInstance(requireContext()) }
//
//    private var loadAltitudeJob: Job? = null
//    private var logJob: Job? = null
//
//    private var isLogging = false
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        WeatherQuickActionBinder(
//            this,
//            binding,
//            prefs.weather
//        ).bind()
//
//        navController = findNavController()
//
//        weatherService = WeatherService(prefs.weather)
//
//        chart = PressureChart(binding.chart) { timeAgo, pressure ->
//            if (isLogging) {
//                return@PressureChart
//            }
//            if (timeAgo == null || pressure == null) {
//                binding.pressureMarker.isVisible = false
//                binding.logBtn.isVisible = true
//            } else {
//                val formatted = formatService.formatPressure(
//                    Pressure(pressure, units),
//                    Units.getDecimalPlaces(units)
//                )
//                binding.pressureMarker.text = getString(
//                    R.string.pressure_reading_time_ago,
//                    formatted,
//                    formatService.formatDuration(timeAgo, false)
//                )
//                binding.pressureMarker.isVisible = true
//                binding.logBtn.isInvisible = true
//                valueSelectedTime = System.currentTimeMillis()
//            }
//        }
//
//        binding.logBtn.setOnClickListener {
//            logJob = runInBackground {
//                isLogging = true
//                withContext(Dispatchers.Main) {
//                    binding.logBtn.isInvisible = true
//                    binding.logLoading.isVisible = true
//                }
//                withContext(Dispatchers.IO) {
//                    MonitorWeatherCommand(requireContext(), false).execute()
//                }
//                withContext(Dispatchers.Main) {
//                    toast(getString(R.string.pressure_logged))
//                    binding.logBtn.isInvisible = false
//                    binding.logLoading.isVisible = false
//                }
//                isLogging = false
//            }
//        }
//
//        pressureRepo.getPressures().observe(viewLifecycleOwner) {
//            readingHistory = it.map { it.toPressureAltitudeReading() }.sortedBy { it.time }
//                .filter { it.time <= Instant.now() }
//            lifecycleScope.launch {
//                updateForecast()
//            }
//        }
//
//        barometer.asLiveData().observe(viewLifecycleOwner, { update() })
//        thermometer.asLiveData().observe(viewLifecycleOwner, { update() })
//    }
//
//    override fun onResume() {
//        super.onResume()
//        useSeaLevelPressure = prefs.weather.useSeaLevelPressure
//        altitude = altimeter.altitude
//        units = prefs.pressureUnits
//
//        loadAltitudeJob = runInBackground {
//            withContext(Dispatchers.IO) {
//                if (!altimeter.hasValidReading) {
//                    altimeter.read()
//                }
//            }
//            withContext(Dispatchers.Main) {
//                altitude = altimeter.altitude
//                update()
//            }
//        }
//
//        update()
//
//        if (!prefs.weather.shouldMonitorWeather) {
//            val error = UserError(
//                ErrorBannerReason.WeatherMonitorOff,
//                getString(R.string.weather_monitoring_disabled),
//                R.drawable.ic_weather,
//                action = getString(R.string.enable)
//            ) {
//                prefs.weather.shouldMonitorWeather = true
//                WeatherUpdateScheduler.start(requireContext())
//                requireMainActivity().errorBanner.dismiss(ErrorBannerReason.WeatherMonitorOff)
//            }
//            requireMainActivity().errorBanner.report(error)
//        }
//    }
//
//    override fun onPause() {
//        super.onPause()
//        tryOrNothing {
//            loadAltitudeJob?.cancel()
//        }
//        tryOrNothing {
//            logJob?.cancel()
//        }
//        isLogging = false
//        requireMainActivity().errorBanner.dismiss(ErrorBannerReason.WeatherMonitorOff)
//    }
//
//
//    private fun update() {
//        if (!isBound) return
//        if (barometer.pressure == 0.0f) return
//
//        if (throttle.isThrottled()) {
//            return
//        }
//
//        val readings = getCalibratedPressures()
//
//        displayChart(readings)
//
//        val tendency = weatherService.getTendency(readings)
//        displayTendency(tendency)
//
//        val pressure = getCurrentPressure()
//        displayPressure(pressure)
//
//        if (System.currentTimeMillis() - valueSelectedTime > 5000) {
//            binding.pressureMarker.isVisible = false
//            if (!isLogging) {
//                binding.logBtn.isVisible = true
//            }
//        }
//    }
//
//    private fun getCalibratedPressures(includeCurrent: Boolean = false): List<PressureReading> {
//        val readings = readingHistory.toMutableList()
//        if (includeCurrent) {
//            readings.add(
//                PressureAltitudeReading(
//                    Instant.now(),
//                    barometer.pressure,
//                    altimeter.altitude,
//                    thermometer.temperature,
//                    if (altimeter is IGPS) (altimeter as IGPS).verticalAccuracy else null
//                )
//            )
//        }
//
//        return weatherService.calibrate(readings, prefs)
//    }
//
//    private fun displayChart(readings: List<PressureReading>) {
//        val displayReadings = readings.filter {
//            Duration.between(
//                it.time,
//                Instant.now()
//            ) <= prefs.weather.pressureHistory
//        }
//
//        if (displayReadings.isNotEmpty()) {
//            val totalTime = Duration.between(
//                displayReadings.first().time, Instant.now()
//            )
//            var hours = totalTime.toHours()
//            val minutes = totalTime.toMinutes() % 60
//
//            when (hours) {
//                0L -> binding.pressureHistoryDuration.text = context?.resources?.getQuantityString(
//                    R.plurals.last_minutes,
//                    minutes.toInt(),
//                    minutes
//                )
//                else -> {
//                    if (minutes >= 30) hours++
//                    binding.pressureHistoryDuration.text =
//                        context?.resources?.getQuantityString(
//                            R.plurals.last_hours,
//                            hours.toInt(),
//                            hours
//                        )
//                }
//            }
//
//        }
//
//        if (displayReadings.isNotEmpty()) {
//            chart.setUnits(units)
//
//            val chartData = displayReadings.map {
//                val timeAgo = Duration.between(Instant.now(), it.time).seconds / (60f * 60f)
//                Pair(
//                    timeAgo as Number,
//                    (PressureUnitUtils.convert(
//                        it.value,
//                        units
//                    )) as Number
//                )
//            }
//
//            chart.plot(chartData)
//        }
//    }
//
//    private fun displayTendency(tendency: PressureTendency) {
//        val formatted = formatService.formatPressure(
//            Pressure(tendency.amount, PressureUnits.Hpa).convertTo(units),
//            Units.getDecimalPlaces(units) + 1
//        )
//        binding.tendencyAmount.text =
//            getString(R.string.pressure_tendency_format_2, formatted)
//
//        when (tendency.characteristic) {
//            PressureCharacteristic.Falling, PressureCharacteristic.FallingFast -> {
//                binding.barometerTrend.setImageResource(R.drawable.ic_arrow_down)
//                binding.barometerTrend.visibility = View.VISIBLE
//            }
//            PressureCharacteristic.Rising, PressureCharacteristic.RisingFast -> {
//                binding.barometerTrend.setImageResource(R.drawable.ic_arrow_up)
//                binding.barometerTrend.visibility = View.VISIBLE
//            }
//            else -> binding.barometerTrend.visibility = View.INVISIBLE
//        }
//    }
//
//    private suspend fun updateForecast() {
//        val hourly = withContext(Dispatchers.IO) {
//            weatherForecastService.getHourlyForecast()
//        }
//
//        val daily = withContext(Dispatchers.IO) {
//            weatherForecastService.getDailyForecast()
//        }
//
//        withContext(Dispatchers.Main) {
//            binding.weatherNowLbl.text = formatService.formatShortTermWeather(
//                hourly
//            )
//            binding.weatherNowImg.setImageResource(
//                getWeatherImage(
//                    hourly,
//                    PressureReading(Instant.now(), barometer.pressure)
//                )
//            )
//            binding.weatherLaterLbl.text = getLongTermWeatherDescription(daily)
//        }
//    }
//
//    private fun getCurrentPressure(): PressureReading {
//        return if (useSeaLevelPressure) {
//            val reading = PressureAltitudeReading(
//                Instant.now(),
//                barometer.pressure,
//                altimeter.altitude,
//                thermometer.temperature,
//                if (altimeter is IGPS) (altimeter as IGPS).verticalAccuracy else null
//            )
//            weatherService.calibrate(readingHistory + listOf(reading), prefs).lastOrNull()
//                ?: reading.seaLevel(prefs.weather.seaLevelFactorInTemp)
//        } else {
//            PressureReading(Instant.now(), barometer.pressure)
//        }
//    }
//
//    private fun displayPressure(pressure: PressureReading) {
//        val formatted = formatService.formatPressure(
//            Pressure(pressure.value, PressureUnits.Hpa).convertTo(units),
//            Units.getDecimalPlaces(units)
//        )
//        binding.pressure.text = formatted
//    }
//
//    private fun getWeatherImage(weather: Weather, currentPressure: PressureReading): Int {
//        return when (weather) {
//            Weather.ImprovingFast -> if (currentPressure.isLow()) R.drawable.cloudy else R.drawable.sunny
//            Weather.ImprovingSlow -> if (currentPressure.isHigh()) R.drawable.sunny else R.drawable.partially_cloudy
//            Weather.WorseningSlow -> if (currentPressure.isLow()) R.drawable.light_rain else R.drawable.cloudy
//            Weather.WorseningFast -> if (currentPressure.isLow()) R.drawable.heavy_rain else R.drawable.light_rain
//            Weather.Storm -> R.drawable.storm
//            else -> R.drawable.steady
//        }
//    }
//
//    private fun getLongTermWeatherDescription(weather: Weather): String {
//        return when (weather) {
//            Weather.ImprovingFast, Weather.ImprovingSlow -> getString(R.string.forecast_improving)
//            Weather.WorseningSlow, Weather.WorseningFast, Weather.Storm -> getString(R.string.forecast_worsening)
//            else -> ""
//        }
//    }
//
//    override fun generateBinding(
//        layoutInflater: LayoutInflater,
//        container: ViewGroup?
//    ): ActivityWeatherBinding {
//        return ActivityWeatherBinding.inflate(layoutInflater, container, false)
//    }
//
//}
