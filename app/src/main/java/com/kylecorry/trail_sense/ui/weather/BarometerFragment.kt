package com.kylecorry.trail_sense.ui.weather

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.shared.math.LowPassFilter
import com.kylecorry.trail_sense.shared.sensors.gps.GPS
import java.util.*
import com.kylecorry.trail_sense.*
import com.kylecorry.trail_sense.shared.Constants
import com.kylecorry.trail_sense.weather.altimeter.AltitudeCalculatorFactory
import com.kylecorry.trail_sense.shared.sensors.barometer.Barometer
import com.kylecorry.trail_sense.weather.PressureHistoryRepository
import com.kylecorry.trail_sense.shared.PressureAltitudeReading
import com.kylecorry.trail_sense.shared.toZonedDateTime
import com.kylecorry.trail_sense.weather.*
import com.kylecorry.trail_sense.weather.forcasting.*
import com.kylecorry.trail_sense.weather.sealevel.AltimeterSeaLevelPressureConverter
import com.kylecorry.trail_sense.weather.sealevel.ISeaLevelPressureConverter
import com.kylecorry.trail_sense.weather.sealevel.NullPressureConverter
import java.time.*


class BarometerFragment : Fragment(), Observer {

    private lateinit var barometer: Barometer
    private lateinit var gps: GPS

    private var altitude = 0F
    private var useSeaLevelPressure = false
    private var units = Constants.PRESSURE_UNITS_HPA
    private var pressureConverter: ISeaLevelPressureConverter =
        NullPressureConverter()

    private val shortTermForecaster: IWeatherForecaster = HourlyForecaster()
    private val longTermForecaster: IWeatherForecaster = DailyForecaster()

    private lateinit var pressureTxt: TextView
    private lateinit var weatherNowTxt: TextView
    private lateinit var weatherNowImg: ImageView
    private lateinit var weatherLaterTxt: TextView
    private lateinit var trendImg: ImageView
    private lateinit var historyDurationTxt: TextView

    private lateinit var chart: ILineChart


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_weather, container, false)

        barometer = Barometer(context!!)
        gps = GPS(context!!)

        pressureTxt = view.findViewById(R.id.pressure)
        weatherNowTxt = view.findViewById(R.id.weather_now_lbl)
        weatherNowImg = view.findViewById(R.id.weather_now_img)
        weatherLaterTxt = view.findViewById(R.id.weather_later_lbl)
        chart = MpLineChart(
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
        barometer.addObserver(this)
        barometer.start()
        gps.addObserver(this)

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        useSeaLevelPressure =
            prefs.getBoolean(getString(R.string.pref_use_sea_level_pressure), false)

        if (useSeaLevelPressure) {
            val calculator = AltitudeCalculatorFactory(context!!).create()
            pressureConverter = AltimeterSeaLevelPressureConverter(calculator)
            altitude = gps.altitude.value
        }

        units =
            prefs.getString(getString(R.string.pref_pressure_units), Constants.PRESSURE_UNITS_HPA)
                ?: Constants.PRESSURE_UNITS_HPA

        updateBarometerChartData()

        gps.updateLocation {}
    }

    override fun onPause() {
        super.onPause()
        barometer.stop()
        PressureHistoryRepository.deleteObserver(this)
        barometer.deleteObserver(this)
    }

    override fun update(o: Observable?, arg: Any?) {
        if (o == barometer) updatePressure()
        if (o == PressureHistoryRepository) {
            updateBarometerChartData()
        }
        if (o == gps) {
            altitude = gps.altitude.value

            if (useSeaLevelPressure) {
                updatePressure()
            }
        }
    }

    private fun updatePressure() {
        if (context == null) return
        if (barometer.pressure.value == 0.0f) return

        val readings = PressureHistoryRepository.getAll(context!!)

        val allReadings = mutableListOf<PressureAltitudeReading>()
        allReadings.addAll(readings)
        allReadings.add(
            PressureAltitudeReading(
                Instant.now(),
                barometer.pressure.value,
                altitude
            )
        )

        val convertedReadings = pressureConverter.convert(allReadings)


        val pressure = convertedReadings.last().value
        val symbol =
            WeatherUtils.getPressureSymbol(
                units
            )

        val format =
            WeatherUtils.getDecimalFormat(units)

        pressureTxt.text = "${format.format(pressure)} $symbol"

        val pressureDirection = PressureTendencyCalculator.getPressureTendency(convertedReadings)

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

        val shortTerm = shortTermForecaster.forecast(convertedReadings)
        val longTerm = longTermForecaster.forecast(convertedReadings)

        weatherNowTxt.text = getShortTermWeatherDescription(shortTerm)
        weatherNowImg.setImageResource(getWeatherImage(shortTerm, pressure))
        weatherLaterTxt.text = getLongTermWeatherDescription(longTerm)
    }

    private fun getWeatherImage(weather: Weather, currentPressure: Float): Int {
        val isLow = WeatherUtils.isLowPressure(currentPressure)
        val isHigh = WeatherUtils.isHighPressure(currentPressure)

        return when (weather) {
            Weather.ImprovingFast -> if (isLow) R.drawable.cloudy else R.drawable.sunny
            Weather.ImprovingSlow -> if (isHigh) R.drawable.sunny else R.drawable.partially_cloudy
            Weather.WorseningSlow -> if (isLow) R.drawable.light_rain else R.drawable.cloudy
            Weather.WorseningFast -> if (isLow) R.drawable.heavy_rain else R.drawable.light_rain
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
        val readings = PressureHistoryRepository.getAll(context!!)

        if (readings.size >= 2) {
            val totalTime = Duration.between(
                readings.first().time, readings.last().time
            )
            var hours = totalTime.toHours()
            val minutes = totalTime.toMinutes() % 60

            when (hours) {
                0L -> historyDurationTxt.text = "$minutes minute${if (minutes == 1L) "" else "s"}"
                else -> {
                    if (minutes >= 30) hours++
                    historyDurationTxt.text = "$hours hour${if (hours == 1L) "" else "s"}"
                }
            }

        }

        val convertedPressures = pressureConverter.convert(readings)

        val filter = LowPassFilter(0.35, convertedPressures.first().value.toDouble())

        val chartData = convertedPressures.map {
            val date = it.time.toZonedDateTime()
            Pair(
                ((date.toEpochSecond() + date.offset.totalSeconds) * 1000) as Number,
                (WeatherUtils.convertPressureToUnits(
                    filter.filter(it.value.toDouble()).toFloat(),
                    units
                )) as Number
            )
        }

        chart.plot(chartData)
    }
}
