package com.kylecorry.survival_aid.weather

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.anychart.AnyChartView
import com.kylecorry.survival_aid.R
import com.kylecorry.survival_aid.navigator.gps.GPS
import com.kylecorry.survival_aid.navigator.gps.UnitSystem
import com.kylecorry.survival_aid.roundPlaces
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.roundToInt
import com.anychart.AnyChart.area
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.anychart.charts.Cartesian
import com.anychart.graphics.vector.text.HAlign
import com.kylecorry.survival_aid.toZonedDateTime


class WeatherFragment : Fragment(), Observer {


    private val unitSystem = UnitSystem.IMPERIAL
    private lateinit var moonPhase: MoonPhase
    private lateinit var barometer: Barometer
    private lateinit var gps: GPS

    private var altitude = 0.0

    private lateinit var moonTxt: TextView
    private lateinit var pressureTxt: TextView
    private lateinit var stormWarningTxt: TextView
    private lateinit var barometerInterpTxt: TextView
    private lateinit var seaLevelTxt: TextView
    private lateinit var sunriseTxt: TextView
    private lateinit var sunsetTxt: TextView
    private lateinit var chart: AnyChartView

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
        seaLevelTxt = view.findViewById(R.id.sealevel_pressure)
        sunriseTxt = view.findViewById(R.id.sunrise)
        sunsetTxt = view.findViewById(R.id.sunset)
        chart = view.findViewById(R.id.chart)

        createBarometerChart()
        return view
    }

    override fun onResume() {
        super.onResume()
        barometer.addObserver(this)
        barometer.start()

        updateMoonPhase()

        gps.updateLocation { location ->
            val sunrise = Sun.getSunrise(location)
            val sunset = Sun.getSunset(location)

            altitude = gps.altitude

            sunriseTxt.text = sunrise.format(DateTimeFormatter.ofPattern("h:mm a"))
            sunsetTxt.text = sunset.format(DateTimeFormatter.ofPattern("h:mm a"))

            updatePressure()
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
        val pressure = barometer.pressure
        val symbol = if (unitSystem == UnitSystem.IMPERIAL) "inHg" else "hPa"

        pressureTxt.text = "${convertPressure(pressure)} $symbol"

        if (altitude != 0.0) {
            val seaLevelPressure = barometer.getSeaLevelPressure(altitude.toFloat())
            seaLevelTxt.text = "${convertPressure(seaLevelPressure)} $symbol at sea-level"
        }

        val pressureDirection = WeatherUtils.getBarometricChangeDirection(PressureHistory.readings)

        barometerInterpTxt.text = pressureDirection.readableName

        if (WeatherUtils.isStormIncoming(PressureHistory.readings)){
            stormWarningTxt.text = getString(R.string.storm_incoming_warning)
        } else {
            stormWarningTxt.text = ""
        }
    }

    private fun convertPressure(pressure: Float): Float {
        return if (unitSystem == UnitSystem.IMPERIAL) WeatherUtils.hPaToInches(pressure).roundPlaces(2) else pressure.roundToInt().toFloat()
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


    private fun createBarometerChart(){
        areaChart = area()
        areaChart.credits().enabled(false)
        areaChart.animation(true)
        areaChart.title("Pressure")
        updateBarometerChartData()
        areaChart.xAxis(0).title(false)
        areaChart.yAxis(0).title(false)
        areaChart.yScale().ticks().interval(0.05)
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

        PressureHistory.readings.forEach { pressureReading: PressureReading ->
            val date = pressureReading.time.toZonedDateTime()
            val formatted = date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) + "<br>" + date.format(
                DateTimeFormatter.ofPattern("h:mm a"))
            seriesData.add(PressureDataEntry(formatted, convertPressure(pressureReading.reading)))
        }
        areaChart.data(seriesData)
    }

    private inner class PressureDataEntry internal constructor(
        x: String,
        value: Number
    ) : ValueDataEntry(x, value)
}
