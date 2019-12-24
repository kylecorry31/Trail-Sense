package com.kylecorry.survival_aid.weather

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kylecorry.survival_aid.R
import com.kylecorry.survival_aid.navigator.gps.UnitSystem
import com.kylecorry.survival_aid.roundPlaces
import java.util.*
import kotlin.math.roundToInt

class WeatherFragment : Fragment(), Observer {


    private val unitSystem = UnitSystem.IMPERIAL
    private lateinit var moonPhase: MoonPhase
    private lateinit var thermometer: Thermometer
    private lateinit var hygrometer: Hygrometer
    private lateinit var barometer: Barometer

    private var hasTemp = false
    private var hasHumidity = false

    private lateinit var moonTxt: TextView
    private lateinit var tempTxt: TextView
    private lateinit var humidityTxt: TextView
    private lateinit var feelsLikeTxt: TextView
    private lateinit var dewpointTxt: TextView
    private lateinit var pressureTxt: TextView
    private lateinit var tempWarningTxt: TextView
    private lateinit var stormWarningTxt: TextView
    private lateinit var barometerInterpTxt: TextView


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_weather, container, false)

        moonPhase = MoonPhase()
        thermometer = Thermometer(context!!)
        hygrometer = Hygrometer(context!!)
        barometer = Barometer(context!!)

        moonTxt = view.findViewById(R.id.moonphase)
        tempTxt = view.findViewById(R.id.temperature)
        humidityTxt = view.findViewById(R.id.humidity)
        feelsLikeTxt = view.findViewById(R.id.feelslike)
        dewpointTxt = view.findViewById(R.id.dewpoint)
        pressureTxt = view.findViewById(R.id.pressure)
        tempWarningTxt = view.findViewById(R.id.tempWarning)
        stormWarningTxt = view.findViewById(R.id.stormWarning)
        barometerInterpTxt = view.findViewById(R.id.barometerInterpretation)

        return view
    }

    override fun onResume() {
        super.onResume()
        thermometer.addObserver(this)
        hygrometer.addObserver(this)
        barometer.addObserver(this)

        thermometer.start()
        hygrometer.start()
        barometer.start()

        updateMoonPhase()
        //updateTemperature()
        //updateHumidity()
        //updatePressure()
    }

    override fun onPause() {
        super.onPause()
        thermometer.stop()
        hygrometer.stop()
        barometer.stop()

        thermometer.deleteObserver(this)
        hygrometer.deleteObserver(this)
        barometer.deleteObserver(this)
    }

    override fun update(o: Observable?, arg: Any?) {
        if (o == thermometer) updateTemperature()
        if (o == hygrometer) updateHumidity()
        if (o == barometer) updatePressure()
    }

    private fun updatePressure(){
        val pressure = barometer.pressure

        val symbol = if (unitSystem == UnitSystem.IMPERIAL) "in" else "hPa"

        val convertedPressure: Float = if (unitSystem == UnitSystem.IMPERIAL) WeatherUtils.hPaToInches(pressure).roundPlaces(2) else pressure.roundToInt().toFloat()

        pressureTxt.text = "Pressure: $convertedPressure $symbol"

        val pressureDirection = WeatherUtils.getBarometricChangeDirection(PressureHistory.readings)

        barometerInterpTxt.text = pressureDirection.readableName

        if (WeatherUtils.isStormIncoming(PressureHistory.readings)){
            stormWarningTxt.text = getString(R.string.storm_incoming_warning)
        } else {
            stormWarningTxt.text = ""
        }
    }

    private fun updateHumidity(){
        hasHumidity = true
        val humidity = hygrometer.humidity.roundToInt()

        humidityTxt.text = "$humidity% humidity"
        updateTempHumidityCombos()
    }

    private fun updateTemperature(){
        hasTemp = true
        val temp = if (unitSystem == UnitSystem.IMPERIAL) WeatherUtils.celsiusToFahrenheit(thermometer.temperature) else thermometer.temperature
        val symbol = if (unitSystem == UnitSystem.IMPERIAL) "F" else "C"
        tempTxt.text = "${temp.roundToInt()}°$symbol"
        updateTempHumidityCombos()
    }


    private fun updateTempHumidityCombos(){
        val temp = thermometer.temperature
        val humidity = hygrometer.humidity

        val heatIndex = convertTemp(WeatherUtils.getHeatIndex(temp, humidity)).roundToInt()
        val comfortLevel = WeatherUtils.getHumidityComfortLevel(temp, humidity)
        val dewPoint = convertTemp(WeatherUtils.getDewPoint(temp, humidity)).roundToInt()
        val heatAlert = WeatherUtils.getHeatAlert(temp, humidity)

        val symbol = if (unitSystem == UnitSystem.IMPERIAL) "F" else "C"

        if (heatIndex != convertTemp(temp).roundToInt()){
            feelsLikeTxt.text = "Feels like $heatIndex°$symbol"
        } else {
            feelsLikeTxt.text = ""
        }

        if (heatAlert != WeatherUtils.HeatAlert.NORMAL){
            tempWarningTxt.text = heatAlert.readableName.toUpperCase()
        } else {
            tempWarningTxt.text = ""
        }

        if (hasHumidity)
            dewpointTxt.text = "Dew point of $dewPoint°$symbol (${comfortLevel.readableName.toLowerCase()})"
    }


    private fun convertTemp(temp: Float): Float {
        if (unitSystem == UnitSystem.IMPERIAL){
            return WeatherUtils.celsiusToFahrenheit(temp)
        }

        return temp
    }


    private fun updateMoonPhase(){
        val phase = moonPhase.getPhase()
        val stringPhase = when {
            phase == MoonPhase.Phase.WANING_CRESCENT -> "Waning Crescent"
            phase == MoonPhase.Phase.WAXING_CRESCENT -> "Waxing Crescent"
            phase == MoonPhase.Phase.WANING_GIBBOUS -> "Waning Gibbous"
            phase == MoonPhase.Phase.WAXING_GIBBOUS -> "Waxing Gibbous"
            phase == MoonPhase.Phase.FIRST_QUARTER -> "First Quarter"
            phase == MoonPhase.Phase.LAST_QUARTER -> "Last Quarter"
            phase == MoonPhase.Phase.FULL -> "Full"
            else -> "New"
        }

        moonTxt.text = "Moon: $stringPhase"
    }
}
