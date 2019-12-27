package com.kylecorry.survival_aid.weather

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.anychart.AnyChartView
import com.kylecorry.survival_aid.R
import com.kylecorry.survival_aid.navigator.gps.GPS
import java.time.format.DateTimeFormatter
import java.util.*
import com.anychart.AnyChart.area
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.anychart.charts.Cartesian
import com.anychart.graphics.vector.text.HAlign
import com.kylecorry.survival_aid.roundPlaces
import com.kylecorry.survival_aid.toZonedDateTime


class WeatherFragment : Fragment(), Observer {

    private lateinit var moonPhase: MoonPhase
    private lateinit var barometer: Barometer
    private lateinit var gps: GPS

    private var altitude = 0.0
    private var useSeaLevelPressure = false
    private var gotGpsReading = false
    private var units = "hPa"

    private lateinit var moonTxt: TextView
    private lateinit var pressureTxt: TextView
    private lateinit var stormWarningTxt: TextView
    private lateinit var barometerInterpTxt: TextView
    private lateinit var sunriseTxt: TextView
    private lateinit var sunsetTxt: TextView
    private lateinit var chart: AnyChartView
    private lateinit var trendImg: ImageView

    private lateinit var areaChart: Cartesian


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_weather, container, false)

        moonPhase = MoonPhase()
        barometer = Barometer(context!!)
        gps = GPS(context!!)

        moonTxt = view.findViewById(R.id.moonphase)
        pressureTxt = view.findViewById(R.id.pressure)
        stormWarningTxt = view.findViewById(R.id.stormWarning)
        barometerInterpTxt = view.findViewById(R.id.barometerInterpretation)
        sunriseTxt = view.findViewById(R.id.sunrise)
        sunsetTxt = view.findViewById(R.id.sunset)
        chart = view.findViewById(R.id.chart)
        trendImg = view.findViewById(R.id.barometer_trend)
        return view
    }

    override fun onResume() {
        super.onResume()
        barometer.addObserver(this)
        barometer.start()

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        useSeaLevelPressure = prefs.getBoolean(getString(R.string.pref_use_sea_level_pressure), false)
        units = prefs.getString(getString(R.string.pref_pressure_units), "hPa") ?: "hPa"

        updateMoonPhase()

        if (!useSeaLevelPressure)
            createBarometerChart()

        gps.updateLocation { location ->
            gotGpsReading = true
            val sunrise = Sun.getSunrise(location)
            val sunset = Sun.getSunset(location)

            altitude = gps.altitude

            sunriseTxt.text = sunrise.format(DateTimeFormatter.ofPattern("h:mm a"))
            sunsetTxt.text = sunset.format(DateTimeFormatter.ofPattern("h:mm a"))

            updatePressure()
            createBarometerChart()
        }
    }

    override fun onPause() {
        super.onPause()
        barometer.stop()
        barometer.deleteObserver(this)
    }

    override fun update(o: Observable?, arg: Any?) {
        if (o == barometer) updatePressure()
    }

    private fun updatePressure(){

        if (useSeaLevelPressure && !gotGpsReading) return

        val pressure = getCalibratedPressure(barometer.pressure)
        val symbol = WeatherUtils.getPressureSymbol(units)

        val format = WeatherUtils.getDecimalFormat(units)

        pressureTxt.text = "${format.format(pressure )} $symbol"

        val pressureDirection = WeatherUtils.get3HourChangeDirection(PressureHistory.readings)
        val instantPressureDirection = WeatherUtils.get15MinuteChangeDirection(PressureHistory.readings)

        when (pressureDirection) {
            WeatherUtils.BarometricChange.FALLING -> {
                trendImg.setImageResource(R.drawable.ic_arrow_down)
                trendImg.visibility = View.VISIBLE
            }
            WeatherUtils.BarometricChange.RISING -> {
                trendImg.setImageResource(R.drawable.ic_arrow_up)
                trendImg.visibility = View.VISIBLE
            }
            else -> trendImg.visibility = View.INVISIBLE
        }

        barometerInterpTxt.text = instantPressureDirection.readableName

        if (WeatherUtils.isStormIncoming(PressureHistory.readings)){
            stormWarningTxt.text = getString(R.string.storm_incoming_warning)
        } else {
            stormWarningTxt.text = ""
        }
    }

    private fun updateMoonPhase(){
        moonTxt.text = when (moonPhase.getPhase()) {
            MoonPhase.Phase.WANING_CRESCENT -> "Waning Crescent"
            MoonPhase.Phase.WAXING_CRESCENT -> "Waxing Crescent"
            MoonPhase.Phase.WANING_GIBBOUS -> "Waning Gibbous"
            MoonPhase.Phase.WAXING_GIBBOUS -> "Waxing Gibbous"
            MoonPhase.Phase.FIRST_QUARTER -> "First Quarter"
            MoonPhase.Phase.LAST_QUARTER -> "Last Quarter"
            MoonPhase.Phase.FULL -> "Full Moon"
            else -> "New Moon"
        }
    }

    private fun getCalibratedPressure(pressure: Float): Float {
        var calibratedPressure = pressure

        if (useSeaLevelPressure){
            calibratedPressure = WeatherUtils.convertToSeaLevelPressure(pressure, altitude.toFloat())
        }
        return WeatherUtils.convertPressureToUnits(calibratedPressure, units)
    }

    private fun createBarometerChart(){
        areaChart = area()
        areaChart.credits().enabled(false)
        areaChart.animation(true)
        areaChart.title("Pressure History")
        updateBarometerChartData()
        areaChart.xAxis(0).title(false)
        areaChart.yAxis(0).title(false)
        areaChart.yScale().ticks().interval(0.05)

        val min: Number = WeatherUtils.convertPressureToUnits(1015f, units).roundPlaces(2)

        areaChart.yScale().softMinimum(min)
        areaChart.xAxis(0).labels().useHtml(true)
        areaChart.xAxis(0).labels().hAlign(HAlign.CENTER)
        areaChart.getSeriesAt(0).color("#FF6D00")
        chart.setChart(areaChart)
    }

    private fun updateBarometerChartData(){
        val seriesData = mutableListOf<DataEntry>()

        if (PressureHistory.readings.isEmpty()){
            BarometerAlarmReceiver.loadFromFile(context!!)
        }

    PressureHistory.removeOldReadings()
        PressureHistory.readings.forEach { pressureReading: PressureReading ->
            val date = pressureReading.time.toZonedDateTime()
            val formatted =
                date.format(DateTimeFormatter.ofPattern("MMM dd")) + "<br>" + date.format(
                    DateTimeFormatter.ofPattern("h:mm a")
                )
            seriesData.add(
                PressureDataEntry(
                    formatted,
                    getCalibratedPressure(pressureReading.reading)
                )
            )
        }
        areaChart.data(seriesData)
    }

    private inner class PressureDataEntry internal constructor(
        x: String,
        value: Number
    ) : ValueDataEntry(x, value)
}
