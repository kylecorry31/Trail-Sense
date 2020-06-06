package com.kylecorry.trail_sense.weather.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.Barometer
import com.kylecorry.trail_sense.shared.sensors.GPS
import com.kylecorry.trail_sense.shared.sensors.IBarometer
import com.kylecorry.trail_sense.shared.sensors.IGPS
import com.kylecorry.trail_sense.shared.toZonedDateTime
import com.kylecorry.trail_sense.weather.domain.*
import com.kylecorry.trail_sense.weather.domain.classifier.PressureClassification
import com.kylecorry.trail_sense.weather.domain.classifier.StandardPressureClassifier
import com.kylecorry.trail_sense.weather.domain.forcasting.DailyForecaster
import com.kylecorry.trail_sense.weather.domain.forcasting.HourlyForecaster
import com.kylecorry.trail_sense.weather.domain.forcasting.IWeatherForecaster
import com.kylecorry.trail_sense.weather.domain.forcasting.Weather
import com.kylecorry.trail_sense.weather.domain.sealevel.ISeaLevelPressureConverter
import com.kylecorry.trail_sense.weather.domain.sealevel.NullPressureConverter
import com.kylecorry.trail_sense.weather.domain.sealevel.SeaLevelPressureConverterFactory
import com.kylecorry.trail_sense.weather.domain.tendency.DropPressureTendencyCalculator
import com.kylecorry.trail_sense.weather.domain.tendency.PressureCharacteristic
import com.kylecorry.trail_sense.weather.infrastructure.PressureHistoryRepository
import java.time.Duration
import java.time.Instant
import java.util.*


class BarometerFragment : Fragment(), Observer {

    private lateinit var barometer: IBarometer
    private lateinit var gps: IGPS

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

    private lateinit var chart: PressureChart

    private lateinit var weatherService: WeatherService


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_weather, container, false)

        barometer = Barometer(requireContext())
        gps = GPS(requireContext())
        prefs = UserPreferences(requireContext())

        weatherService = WeatherService(prefs.weather.stormAlertThreshold)

        pressureTxt = view.findViewById(R.id.pressure)
        weatherNowTxt = view.findViewById(R.id.weather_now_lbl)
        weatherNowImg = view.findViewById(R.id.weather_now_img)
        weatherLaterTxt = view.findViewById(R.id.weather_later_lbl)
        chart = PressureChart(
            view.findViewById(R.id.chart),
            resources.getColor(R.color.colorPrimary, null)
        )
        trendImg = view.findViewById(R.id.barometer_trend)
        historyDurationTxt = view.findViewById(R.id.pressure_history_duration)

        return view
    }

    override fun onResume() {
        super.onResume()
        PressureHistoryRepository.addObserver(this)
        barometer.start(this::onPressureUpdate)
        gps.start(this::onLocationUpdate)

        useSeaLevelPressure = prefs.weather.useSeaLevelPressure
        altitude = gps.altitude
        units = prefs.pressureUnits

        updateBarometerChartData()
    }

    override fun onPause() {
        super.onPause()
        barometer.stop(this::onPressureUpdate)
        gps.stop(this::onLocationUpdate)
        PressureHistoryRepository.deleteObserver(this)
    }

    override fun update(o: Observable?, arg: Any?) {
        if (o == PressureHistoryRepository) {
            updateBarometerChartData()
        }
    }

    private fun onPressureUpdate(): Boolean {
        updatePressure()
        return true
    }

    private fun onLocationUpdate(): Boolean {
        altitude = gps.altitude
        if (useSeaLevelPressure) {
            updatePressure()
        }
        return false
    }

    private fun updatePressure() {
        if (context == null) return
        if (barometer.pressure == 0.0f) return

        val readings = PressureHistoryRepository.getAll(requireContext())

        val allReadings = mutableListOf<PressureAltitudeReading>()
        allReadings.addAll(readings)
        allReadings.add(
            PressureAltitudeReading(
                Instant.now(),
                barometer.pressure,
                altitude
            )
        )

        val convertedReadings = if (useSeaLevelPressure) {
            weatherService.convertToSeaLevel(allReadings)
        } else {
            NullPressureConverter().convert(allReadings)
        }

        val pressure = convertedReadings.last().value
        val symbol = PressureUnitUtils.getSymbol(units)

        val format = PressureUnitUtils.getDecimalFormat(units)

        pressureTxt.text = "${format.format(PressureUnitUtils.convert(pressure, units))}  $symbol"

        val convertedPressureHistory = convertedReadings.subList(0, convertedReadings.lastIndex)

        val pressureDirection = weatherService.getTendency(convertedPressureHistory)

        when (pressureDirection.characteristic) {
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

        val shortTerm = weatherService.getHourlyWeather(convertedPressureHistory)
        val longTerm = weatherService.getDailyWeather(convertedPressureHistory)

        weatherNowTxt.text = getShortTermWeatherDescription(shortTerm)
        weatherNowImg.setImageResource(getWeatherImage(shortTerm, pressure))
        weatherLaterTxt.text = getLongTermWeatherDescription(longTerm)
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


    private fun updateBarometerChartData() {
        val historyDuration = prefs.weather.pressureHistory
        val now = Instant.now()
        val readings = PressureHistoryRepository.getAll(requireContext()).filter {
            Duration.between(it.time, now) <= historyDuration
        }

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

        val convertedPressures = if (useSeaLevelPressure) {
            weatherService.convertToSeaLevel(readings)
        } else {
            NullPressureConverter().convert(readings)
        }

        if (convertedPressures.isNotEmpty()) {
            val filter = LowPassFilter(0.6, convertedPressures.first().value.toDouble())

            chart.setUnits(units)

            val chartData = convertedPressures.map {
                val date = it.time.toZonedDateTime()
                Pair(
                    ((date.toEpochSecond() + date.offset.totalSeconds) * 1000) as Number,
                    (PressureUnitUtils.convert(
                        filter.filter(it.value.toDouble()).toFloat(),
                        units
                    )) as Number
                )
            }

            chart.plot(chartData)
        }
    }
}
