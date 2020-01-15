package com.kylecorry.trail_sense.ui

import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.anychart.AnyChartView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.sensors.gps.GPS
import java.util.*
import com.anychart.AnyChart.area
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.anychart.charts.Cartesian
import com.anychart.enums.ScaleTypes
import com.kylecorry.trail_sense.database.PressureHistoryRepository
import com.kylecorry.trail_sense.models.AltitudeReading
import com.kylecorry.trail_sense.navigator.LocationMath
import com.kylecorry.trail_sense.sensors.barometer.Barometer
import com.kylecorry.trail_sense.models.PressureAltitudeReading
import com.kylecorry.trail_sense.toZonedDateTime
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt




class AltimeterFragment : Fragment(), Observer {

    private lateinit var barometer: Barometer
    private lateinit var gps: GPS

    private var gotGpsReading = false
    private var units = "meters"
    private var mode = "barometer_gps"

    private val CHART_DURATION = Duration.ofHours(4)

    private lateinit var altitudeTxt: TextView
    private lateinit var chart: AnyChartView

    private lateinit var areaChart: Cartesian

    private var chartInitialized = false


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_altimeter, container, false)

        barometer =
            Barometer(context!!)
        gps = GPS(context!!)

        altitudeTxt = view.findViewById(R.id.altitude)
        chart = view.findViewById(R.id.altitude_chart)

        return view
    }

    override fun onResume() {
        super.onResume()
        PressureHistoryRepository.addObserver(this)

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        units = prefs.getString(getString(R.string.pref_distance_units), "meters") ?: "meters"
        mode = prefs.getString(getString(R.string.pref_altimeter_mode), "barometer_gps") ?: "barometer_gps"

        if (mode == "gps" || mode == "barometer_gps") {
            gps.addObserver(this)
            gps.start()
        }

        if (mode == "barometer" || mode == "barometer_gps"){
            barometer.addObserver(this)
            barometer.start()
        }

        if (!chartInitialized){
            createAltitudeChart()
        }
    }

    override fun onPause() {
        super.onPause()
        barometer.stop()
        gps.stop()

        PressureHistoryRepository.deleteObserver(this)
        barometer.deleteObserver(this)
        gps.deleteObserver(this)
    }

    override fun update(o: Observable?, arg: Any?) {
        if (context == null) return
        if (o == barometer){
            updateAltitude()
        }
        if (o == PressureHistoryRepository) {
            if (!chartInitialized) {
                createAltitudeChart()
            } else {
                updateAltimeterChartData()
            }
        }
        if (o == gps){
            gotGpsReading = true
            updateAltitude()
        }
    }

    private fun updateAltitude() {
        if (mode != "barometer" && !gotGpsReading) return
        if (mode != "gps" && barometer.pressure.value == 0.0f) return
        if (context == null) return

        val rawPressureHistory = PressureHistoryRepository.getAll(context!!).filter { Duration.between(it.time, Instant.now()) < CHART_DURATION }
        val pressureHistory = mutableListOf<PressureAltitudeReading>()
        pressureHistory.addAll(rawPressureHistory)
        pressureHistory.add(
            PressureAltitudeReading(
                Instant.now(),
                barometer.pressure.value,
                gps.altitude.value
            )
        )

        val historicalAltitudes = getAltitudeHistory(pressureHistory)

        val altitude = historicalAltitudes.last().value

        altitudeTxt.text = "${LocationMath.convertToBaseUnit(altitude.toFloat(), units).roundToInt()} ${if (units == "meters") "m" else "ft"}"
    }

    private fun getCalibratedAltitude(gpsAltitude: Float, pressureAtGpsAltitude: Float, currentPressure: Float): Float {
        val gpsBarometricAltitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressureAtGpsAltitude)
        val currentBarometricAltitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, currentPressure)
        val change = currentBarometricAltitude - gpsBarometricAltitude
        return gpsAltitude + change
    }

    private fun createAltitudeChart(){
        areaChart = area()
        areaChart.credits().enabled(false)
        areaChart.animation(true)
        areaChart.title("Altitude History")
        updateAltimeterChartData()
        areaChart.yAxis(0).title(false)
        areaChart.yScale().ticks().interval(10)

//        areaChart.yScale().softMinimum(0)
        areaChart.xScale(ScaleTypes.DATE_TIME)


        areaChart.xAxis(0).labels().enabled(false)
        areaChart.getSeriesAt(0).color(String.format("#%06X", 0xFFFFFF and resources.getColor(R.color.colorPrimary, null)))
        chart.setChart(areaChart)
        chartInitialized = true
    }

    private fun getAltitudeHistory(pressureHistory: List<PressureAltitudeReading> = PressureHistoryRepository.getAll(context!!)): List<AltitudeReading> {

        val altitudeHistory = mutableListOf<AltitudeReading>()

        if (pressureHistory.isEmpty()) return altitudeHistory


        var referenceReading = pressureHistory.first()

        pressureHistory.forEach {

            // TODO: Make barometer + gps combo better
            if (mode == "barometer_gps" && Duration.between(referenceReading.time, it.time) >= Duration.ofMinutes(31)){
                referenceReading = it
            }

            val altitude: Float = if (mode == "gps"){
                it.altitude
            } else {
                getCalibratedAltitude(referenceReading.altitude.toFloat(), referenceReading.pressure, it.pressure)
            }

            altitudeHistory.add(
                AltitudeReading(
                    it.time,
                    altitude
                )
            )
        }

        return altitudeHistory
    }

    private fun updateAltimeterChartData(){
        val seriesData = mutableListOf<DataEntry>()

        val pressureHistory = PressureHistoryRepository.getAll(context!!).filter { Duration.between(it.time, Instant.now()) < CHART_DURATION }

        val readings = getAltitudeHistory(pressureHistory)

        if (readings.isEmpty()){
            areaChart.data(seriesData)
            return
        }

        if (readings.size >= 2){
            val totalTime = Duration.between(readings.first().time, readings.last().time)
            var hours = totalTime.toHours()
            val minutes = totalTime.toMinutes() - hours * 60

            when (hours) {
                0L -> areaChart.xAxis(0  ).title("$minutes minute${if (minutes == 1L) "" else "s"}")
                else -> {
                    if (minutes >= 30) hours++
                    areaChart.xAxis(0  ).title("$hours hour${if (hours == 1L) "" else "s"}")
                }
            }

        }

        readings.forEach {
            val date = it.time.toZonedDateTime()

            seriesData.add(
                PressureDataEntry(
                    (date.toEpochSecond() + date.offset.totalSeconds) * 1000,
                    LocationMath.convertToBaseUnit(it.value, units)
                )
            )
        }
        areaChart.data(seriesData)
    }

    private inner class PressureDataEntry internal constructor(
        x: Number,
        value: Number
    ) : ValueDataEntry(x, value)
}
