package com.kylecorry.trail_sense.weather.ui

import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.formatHM
import com.kylecorry.trail_sense.shared.sensors.IAltimeter
import com.kylecorry.trail_sense.shared.sensors.IBarometer
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.weather.domain.*
import com.kylecorry.trail_sense.weather.domain.classifier.PressureClassification
import com.kylecorry.trail_sense.weather.domain.forcasting.Weather
import com.kylecorry.trail_sense.weather.domain.sealevel.NullPressureConverter
import com.kylecorry.trail_sense.weather.domain.tendency.PressureCharacteristic
import com.kylecorry.trail_sense.weather.infrastructure.database.PressureHistoryRepository
import java.time.Duration
import java.time.Instant
import java.util.*

class BarometerFragment : Fragment(), Observer {

    private lateinit var barometer: IBarometer
    private lateinit var altimeter: IAltimeter

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

    private lateinit var chart: PressureChart

    private lateinit var weatherService: WeatherService
    private lateinit var sensorService: SensorService


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_weather, container, false)

        sensorService = SensorService(requireContext())

        barometer = sensorService.getBarometer()
        altimeter = sensorService.getAltimeter()
        prefs = UserPreferences(requireContext())

        weatherService = WeatherService(
            prefs.weather.stormAlertThreshold,
            prefs.weather.dailyForecastChangeThreshold,
            prefs.weather.hourlyForecastChangeThreshold
        )

        pressureTxt = view.findViewById(R.id.pressure)
        weatherNowTxt = view.findViewById(R.id.weather_now_lbl)
        weatherNowImg = view.findViewById(R.id.weather_now_img)
        weatherLaterTxt = view.findViewById(R.id.weather_later_lbl)
        pressureMarkerTxt = view.findViewById(R.id.pressure_marker)
        tendencyAmountTxt = view.findViewById(R.id.tendency_amount)
        chart = PressureChart(
            view.findViewById(R.id.chart),
            resources.getColor(R.color.colorPrimary, null),
            object : IPressureChartSelectedListener {
                override fun onNothingSelected() {
                    pressureMarkerTxt.text = ""
                }

                override fun onValueSelected(timeAgo: Duration, pressure: Float) {
                    val symbol = PressureUnitUtils.getSymbol(units)
                    val format = PressureUnitUtils.getDecimalFormat(units)
                    pressureMarkerTxt.text = getString(
                        R.string.pressure_reading_time_ago,
                        format.format(pressure),
                        symbol,
                        timeAgo.formatHM(true)
                    )
                }

            }
        )
        trendImg = view.findViewById(R.id.barometer_trend)
        historyDurationTxt = view.findViewById(R.id.pressure_history_duration)


        return view
    }

    override fun onResume() {
        super.onResume()
        PressureHistoryRepository.addObserver(this)
        startSensors()

        useSeaLevelPressure = prefs.weather.useSeaLevelPressure
        altitude = altimeter.altitude
        units = prefs.pressureUnits

        update()
    }

    private fun startSensors() {
        barometer.start(this::onPressureUpdate)

        if (altimeter.hasValidReading) {
            onAltitudeUpdate()
        } else {
            altimeter.start(this::onAltitudeUpdate)
        }
    }

    override fun onPause() {
        super.onPause()
        barometer.stop(this::onPressureUpdate)
        altimeter.stop(this::onAltitudeUpdate)
        PressureHistoryRepository.deleteObserver(this)
    }

    override fun update(o: Observable?, arg: Any?) {
        if (o == PressureHistoryRepository) {
            update()
        }
    }

    private fun onPressureUpdate(): Boolean {
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

        val readings = if (useSeaLevelPressure) {
            getSeaLevelPressureHistory()
        } else {
            getPressureHistory()
        }

        displayChart(readings)
        updateTendency(readings)
        updateForecast(readings)

        val pressure = getCurrentPressure()
        updatePressure(pressure)
    }

    private fun getSeaLevelPressureHistory(includeCurrent: Boolean = false): List<PressureReading> {
        val readings = getReadingHistory().toMutableList()
        if (includeCurrent) {
            readings.add(
                PressureAltitudeReading(
                    Instant.now(),
                    barometer.pressure,
                    altimeter.altitude
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
                    altimeter.altitude
                )
            )
        }
        return NullPressureConverter().convert(readings)
    }

    private fun displayChart(readings: List<PressureReading>) {
        if (readings.size >= 2) {
            val totalTime = Duration.between(
                readings.first().time, readings.last().time
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

        if (readings.isNotEmpty()) {
            val filter = LowPassFilter(0.6f, readings.first().value)

            chart.setUnits(units)

            val chartData = readings.map {
                val timeAgo = Duration.between(Instant.now(), it.time).seconds / (60f * 60f)
                Pair(
                    timeAgo as Number,
                    (PressureUnitUtils.convert(
                        filter.filter(it.value),
                        units
                    )) as Number
                )
            }

            chart.plot(chartData)
        }
    }

    private fun updateTendency(readings: List<PressureReading>) {
        val tendency = weatherService.getTendency(readings)

        val symbol = getPressureUnitString(units)
        val format = PressureUnitUtils.getTendencyDecimalFormat(units)
        val formattedTendencyAmount =
            format.format(PressureUnitUtils.convert(tendency.amount, units))
        tendencyAmountTxt.text =
            getString(R.string.pressure_tendency_format, formattedTendencyAmount, symbol)

        when (tendency.characteristic) {
            PressureCharacteristic.Falling -> {
                trendImg.setImageResource(R.drawable.ic_arrow_down)
                trendImg.visibility = View.VISIBLE
            }
            PressureCharacteristic.Rising -> {
                trendImg.setImageResource(R.drawable.ic_arrow_up)
                trendImg.visibility = View.VISIBLE
            }
            else -> trendImg.visibility = View.INVISIBLE
        }
    }

    private fun updateForecast(readings: List<PressureReading>) {
        val shortTerm = weatherService.getHourlyWeather(readings)
        val longTerm = weatherService.getDailyWeather(readings)

        weatherNowTxt.text = getShortTermWeatherDescription(shortTerm)
        weatherNowImg.setImageResource(
            getWeatherImage(
                shortTerm,
                readings.lastOrNull()?.value ?: SensorManager.PRESSURE_STANDARD_ATMOSPHERE
            )
        )
        weatherLaterTxt.text = getLongTermWeatherDescription(longTerm)
    }

    private fun getCurrentPressure(): Float {
        val readings = if (useSeaLevelPressure) {
            getSeaLevelPressureHistory(true)
        } else {
            getPressureHistory(true)
        }
        return readings.last().value
    }

    private fun updatePressure(pressure: Float) {
        val symbol = getPressureUnitString(units)
        val format = PressureUnitUtils.getDecimalFormat(units)
        pressureTxt.text = getString(
            R.string.pressure_format,
            format.format(PressureUnitUtils.convert(pressure, units)),
            symbol
        )
    }

    private fun getReadingHistory(): List<PressureAltitudeReading> {
        return PressureHistoryRepository.getAll(requireContext())
            .filter { Duration.between(it.time, Instant.now()) <= prefs.weather.pressureHistory }
    }

    private fun getWeatherImage(weather: Weather, currentPressure: Float): Int {
        val classification = weatherService.classifyPressure(currentPressure)

        return when (weather) {
            Weather.ImprovingFast -> if (classification == PressureClassification.Low) R.drawable.cloudy else R.drawable.sunny
            Weather.ImprovingSlow -> if (classification == PressureClassification.High) R.drawable.sunny else R.drawable.partially_cloudy
            Weather.WorseningSlow -> if (classification == PressureClassification.Low) R.drawable.light_rain else R.drawable.cloudy
            Weather.WorseningFast -> if (classification == PressureClassification.Low) R.drawable.heavy_rain else R.drawable.light_rain
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

    private fun getPressureUnitString(unit: PressureUnits): String {
        return when (unit) {
            PressureUnits.Hpa -> getString(R.string.units_hpa)
            PressureUnits.Mbar -> getString(R.string.units_mbar)
            PressureUnits.Inhg -> getString(R.string.units_inhg_short)
            PressureUnits.Psi -> getString(R.string.units_psi)
        }
    }

}
