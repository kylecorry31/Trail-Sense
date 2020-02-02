package com.kylecorry.trail_sense.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
import com.kylecorry.trail_sense.Constants
import com.kylecorry.trail_sense.roundPlaces
import com.kylecorry.trail_sense.sensors.barometer.Barometer
import com.kylecorry.trail_sense.toZonedDateTime
import com.kylecorry.trail_sense.database.PressureHistoryRepository
import com.kylecorry.trail_sense.weather.*
import java.time.*


class BarometerFragment : Fragment(), Observer {

    private lateinit var barometer: Barometer
    private lateinit var gps: GPS

    private var altitude = 0F
    private var useSeaLevelPressure = false
    private var gotGpsReading = false
    private var units = Constants.PRESSURE_UNITS_HPA
    private var pressureConverter: ISeaLevelPressureConverter = NullPressureConverter()

    private val pressureTendencyRepository = PressureTendencyRepository()

    private lateinit var pressureTxt: TextView
    private lateinit var stormWarningTxt: TextView
    private lateinit var barometerInterpTxt: TextView
    private lateinit var chart: AnyChartView
    private lateinit var trendImg: ImageView

    private lateinit var areaChart: Cartesian

    private var chartInitialized = false


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_weather, container, false)

        barometer =
            Barometer(context!!)
        gps = GPS(context!!)

        pressureTxt = view.findViewById(R.id.pressure)
        stormWarningTxt = view.findViewById(R.id.stormWarning)
        barometerInterpTxt = view.findViewById(R.id.barometerInterpretation)
        chart = view.findViewById(R.id.chart)
        trendImg = view.findViewById(R.id.barometer_trend)

        return view
    }

    override fun onResume() {
        super.onResume()
        PressureHistoryRepository.addObserver(this)
        barometer.addObserver(this)
        barometer.start()

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        useSeaLevelPressure = prefs.getBoolean(getString(R.string.pref_use_sea_level_pressure), false)

        if (useSeaLevelPressure){
            pressureConverter = DerivativeSeaLevelPressureConverter(Constants.MAXIMUM_NATURAL_PRESSURE_CHANGE)
        }

        units = prefs.getString(getString(R.string.pref_pressure_units), Constants.PRESSURE_UNITS_HPA) ?: Constants.PRESSURE_UNITS_HPA

        if (!useSeaLevelPressure)
            createBarometerChart()

        gps.updateLocation {
            if (context != null) {
                gotGpsReading = true

                altitude = gps.altitude.value


                if (useSeaLevelPressure) {
                    updatePressure()
                    createBarometerChart()
                }
            }
        }
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
            if (!chartInitialized) {
                createBarometerChart()
            } else {
                updateBarometerChartData()
            }
        }
    }

    private fun updatePressure(){

        if (useSeaLevelPressure && !gotGpsReading) return
        if (context == null) return

        val readings = PressureHistoryRepository.getAll(context!!)
        val convertedReadings = pressureConverter.convert(readings)

        val pressure = getCalibratedPressure(barometer.pressure.value)
        val symbol =
            WeatherUtils.getPressureSymbol(
                units
            )

        val format =
            WeatherUtils.getDecimalFormat(units)

        pressureTxt.text = "${format.format(pressure )} $symbol"

        val pressureDirection = WeatherUtils.getPressureTendency(convertedReadings)

        when {
            WeatherUtils.isFalling(
                pressureDirection
            ) -> {
                trendImg.setImageResource(R.drawable.ic_arrow_down)
                trendImg.visibility = View.VISIBLE
            }
            WeatherUtils.isRising(
                pressureDirection
            ) -> {
                trendImg.setImageResource(R.drawable.ic_arrow_up)
                trendImg.visibility = View.VISIBLE
            }
            else -> trendImg.visibility = View.INVISIBLE
        }

        barometerInterpTxt.text = pressureTendencyRepository.getDescription(pressureDirection)


        if (WeatherUtils.isStormIncoming(convertedReadings)){
            stormWarningTxt.text = getString(R.string.storm_incoming_warning)
        } else {
            stormWarningTxt.text = ""
        }
    }

    private fun getCalibratedPressure(pressure: Float): Float {
        var calibratedPressure = pressure

        if (useSeaLevelPressure){
            calibratedPressure =
                SeaLevelPressureCalibrator.calibrate(
                    pressure,
                    altitude
                )
        }
        return WeatherUtils.convertPressureToUnits(
            calibratedPressure,
            units
        )
    }

    private fun createBarometerChart(){
        if (context == null) return
        areaChart = area()
        areaChart.credits().enabled(false)
        areaChart.animation(true)
        areaChart.title(getString(R.string.pressure_chart_title))
        updateBarometerChartData()
        areaChart.yAxis(0).title(false)
        areaChart.yScale().ticks().interval(0.05)

        val min: Number = WeatherUtils.convertPressureToUnits(
            1015f,
            units
        ).roundPlaces(2)

        areaChart.yScale().softMinimum(min)
        areaChart.xScale(ScaleTypes.DATE_TIME)

        areaChart.xAxis(0).labels().enabled(false)
        areaChart.getSeriesAt(0).color(String.format("#%06X", 0xFFFFFF and resources.getColor(R.color.colorPrimary, null)))
        chart.setChart(areaChart)
        chartInitialized = true
    }

    private fun updateBarometerChartData(){
        val seriesData = mutableListOf<DataEntry>()

        val readings = PressureHistoryRepository.getAll(context!!)

        if (readings.size >= 2){
            val totalTime = Duration.between(
                readings.first().time, readings.last().time)
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

        val convertedPressures = pressureConverter.convert(readings)

        convertedPressures.forEach {
            val date = it.time.toZonedDateTime()
            seriesData.add(
                PressureDataEntry(
                    (date.toEpochSecond() + date.offset.totalSeconds) * 1000,
                        WeatherUtils.convertPressureToUnits(
                            it.value,
                            units)
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
