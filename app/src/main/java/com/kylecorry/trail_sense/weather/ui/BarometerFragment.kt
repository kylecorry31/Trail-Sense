package com.kylecorry.trail_sense.weather.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.formatHM
import com.kylecorry.trail_sense.shared.sensors.*
import com.kylecorry.trail_sense.shared.switchToFragment
import com.kylecorry.trail_sense.weather.domain.*
import com.kylecorry.trail_sense.weather.domain.WeatherService
import com.kylecorry.trail_sense.weather.domain.sealevel.NullPressureConverter
import com.kylecorry.trail_sense.weather.infrastructure.database.PressureHistoryRepository
import com.kylecorry.trail_sense.weather.infrastructure.database.PressureRepo
import com.kylecorry.trailsensecore.domain.units.PressureUnits
import com.kylecorry.trailsensecore.domain.units.UnitService
import com.kylecorry.trailsensecore.domain.weather.*
import com.kylecorry.trailsensecore.infrastructure.sensors.altimeter.IAltimeter
import com.kylecorry.trailsensecore.infrastructure.sensors.barometer.IBarometer
import com.kylecorry.trailsensecore.infrastructure.sensors.temperature.IThermometer
import com.kylecorry.trailsensecore.infrastructure.time.Throttle
import java.time.Duration
import java.time.Instant

class BarometerFragment : Fragment() {

    private lateinit var barometer: IBarometer
    private lateinit var altimeter: IAltimeter
    private lateinit var thermometer: IThermometer

    private var altitude = 0F
    private var useSeaLevelPressure = false
    private var units = PressureUnits.Hpa

    private lateinit var prefs: UserPreferences

    private lateinit var pressureTxt: TextView
    private lateinit var weatherNowTxt: TextView
    private lateinit var weatherNowImg: ImageView
    private lateinit var weatherLaterTxt: TextView
    private lateinit var trendImg: ImageView
    private lateinit var historyDurationTxt: TextView
    private lateinit var pressureMarkerTxt: TextView
    private lateinit var tendencyAmountTxt: TextView
    private lateinit var temperatureBtn: FloatingActionButton

    private lateinit var chart: PressureChart

    private lateinit var weatherService: WeatherService
    private lateinit var sensorService: SensorService
    private val unitService = UnitService()
    private val formatService by lazy { FormatService(requireContext()) }
    private val pressureRepo by lazy { PressureRepo(requireContext()) }

    private val throttle = Throttle(20)

    private var pressureSetpoint: PressureAltitudeReading? = null

    private var valueSelectedTime = 0L


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_weather, container, false)

        sensorService = SensorService(requireContext())

        barometer = sensorService.getBarometer()
        altimeter = sensorService.getAltimeter()
        thermometer = sensorService.getThermometer()
        prefs = UserPreferences(requireContext())

        weatherService = WeatherService(
            prefs.weather.stormAlertThreshold,
            prefs.weather.dailyForecastChangeThreshold,
            prefs.weather.hourlyForecastChangeThreshold,
            prefs.weather.seaLevelFactorInRapidChanges,
            prefs.weather.seaLevelFactorInTemp
        )

        pressureTxt = view.findViewById(R.id.pressure)
        weatherNowTxt = view.findViewById(R.id.weather_now_lbl)
        weatherNowImg = view.findViewById(R.id.weather_now_img)
        weatherLaterTxt = view.findViewById(R.id.weather_later_lbl)
        pressureMarkerTxt = view.findViewById(R.id.pressure_marker)
        tendencyAmountTxt = view.findViewById(R.id.tendency_amount)
        temperatureBtn = view.findViewById(R.id.temperature_btn)
        chart = PressureChart(
            view.findViewById(R.id.chart),
            resources.getColor(R.color.colorPrimary, null),
            object : IPressureChartSelectedListener {
                override fun onNothingSelected() {
                    if (pressureSetpoint == null) {
                        pressureMarkerTxt.text = ""
                    }
                }

                override fun onValueSelected(timeAgo: Duration, pressure: Float) {
                    val formatted = formatService.formatPressure(pressure, units)
                    pressureMarkerTxt.text = getString(
                        R.string.pressure_reading_time_ago,
                        formatted,
                        timeAgo.formatHM(false)
                    )
                    valueSelectedTime = System.currentTimeMillis()
                }

            }
        )
        trendImg = view.findViewById(R.id.barometer_trend)
        historyDurationTxt = view.findViewById(R.id.pressure_history_duration)

        temperatureBtn.setOnClickListener {
            switchToFragment(ThermometerFragment(), addToBackStack = true)
        }

        pressureTxt.setOnLongClickListener {
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

            true
        }


        return view
    }

    override fun onResume() {
        super.onResume()
        startSensors()

        useSeaLevelPressure = prefs.weather.useSeaLevelPressure
        altitude = altimeter.altitude
        units = prefs.pressureUnits

        pressureSetpoint = prefs.weather.pressureSetpoint

        update()
    }

    private fun startSensors() {
        barometer.start(this::onPressureUpdate)
        altimeter.start(this::onAltitudeUpdate)
        thermometer.start(this::onTemperatureUpdate)
    }

    override fun onPause() {
        super.onPause()
        barometer.stop(this::onPressureUpdate)
        altimeter.stop(this::onAltitudeUpdate)
        thermometer.stop(this::onTemperatureUpdate)
    }

    private fun onPressureUpdate(): Boolean {
        update()
        return true
    }

    private fun onTemperatureUpdate(): Boolean {
        update()
        return true
    }

    private fun onAltitudeUpdate(): Boolean {
        update()
        return false
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
            pressureMarkerTxt.text = ""
        }
    }

    private fun displaySetpoint(setpoint: PressureReading) {
        val converted = convertPressure(setpoint)
        val formatted = formatService.formatPressure(converted.value, units)

        val timeAgo = Duration.between(setpoint.time, Instant.now())
        pressureMarkerTxt.text = getString(
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
        return weatherService.convertToSeaLevel(readings)
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
                0L -> historyDurationTxt.text = context?.resources?.getQuantityString(
                    R.plurals.last_minutes,
                    minutes.toInt(),
                    minutes
                )
                else -> {
                    if (minutes >= 30) hours++
                    historyDurationTxt.text =
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
        tendencyAmountTxt.text =
            getString(R.string.pressure_tendency_format_2, formatted)

        when (tendency.characteristic) {
            PressureCharacteristic.Falling, PressureCharacteristic.FallingFast -> {
                trendImg.setImageResource(R.drawable.ic_arrow_down)
                trendImg.visibility = View.VISIBLE
            }
            PressureCharacteristic.Rising, PressureCharacteristic.RisingFast -> {
                trendImg.setImageResource(R.drawable.ic_arrow_up)
                trendImg.visibility = View.VISIBLE
            }
            else -> trendImg.visibility = View.INVISIBLE
        }
    }

    private fun updateForecast(readings: List<PressureReading>, setpoint: PressureReading?) {
        val shortTerm = weatherService.getHourlyWeather(readings, setpoint)
        val longTerm = weatherService.getDailyWeather(readings)

        weatherNowTxt.text = getShortTermWeatherDescription(shortTerm)
        weatherNowImg.setImageResource(
            getWeatherImage(
                shortTerm,
                readings.lastOrNull() ?: PressureReading(Instant.now(), barometer.pressure)
            )
        )
        weatherLaterTxt.text = getLongTermWeatherDescription(longTerm)
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
        return if (useSeaLevelPressure) {
            getSeaLevelPressureHistory(true)
        } else {
            getPressureHistory(true)
        }.last()
    }

    private fun displayPressure(pressure: PressureReading) {
        val formatted = formatService.formatPressure(convertPressure(pressure).value, units)
        pressureTxt.text = formatted
    }

    private fun convertPressure(pressure: PressureReading): PressureReading {
        val converted = unitService.convert(pressure.value, PressureUnits.Hpa, units)
        return pressure.copy(value = converted)
    }

    private fun getReadingHistory(): List<PressureAltitudeReading> {
        return pressureRepo.get().toList()
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
