package com.kylecorry.trail_sense.weather.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.system.UiUtils
import com.kylecorry.trail_sense.weather.domain.HeatAlert
import com.kylecorry.trail_sense.weather.domain.WeatherService

class ThermometerFragment : Fragment() {

    private lateinit var tempTxt: TextView
    private lateinit var humidityTxt: TextView
    private lateinit var dewPointTxt: TextView
    private lateinit var heatAlertImg: ImageView

    private val sensorService by lazy { SensorService(requireContext()) }
    private val thermometer by lazy { sensorService.getThermometer() }
    private val hygrometer by lazy { sensorService.getHygrometer() }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }
    private val weatherService by lazy {
        WeatherService(
            prefs.weather.stormAlertThreshold,
            prefs.weather.dailyForecastChangeThreshold,
            prefs.weather.hourlyForecastChangeThreshold,
            prefs.weather.seaLevelFactorInRapidChanges,
            prefs.weather.seaLevelFactorInTemp
        )
    }

    private var heatAlertTitle = ""
    private var heatAlertContent = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_thermometer_hygrometer, container, false)

        tempTxt = view.findViewById(R.id.temperature)
        humidityTxt = view.findViewById(R.id.humidity)
        dewPointTxt = view.findViewById(R.id.dew_point)
        heatAlertImg = view.findViewById(R.id.heat_alert)

        heatAlertImg.setOnClickListener {
            UiUtils.alert(requireContext(), heatAlertTitle, heatAlertContent)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        thermometer.start(this::onTemperatureUpdate)
        hygrometer.start(this::onHumidityUpdate)
    }

    override fun onPause() {
        super.onPause()
        thermometer.stop(this::onTemperatureUpdate)
        hygrometer.stop(this::onHumidityUpdate)
    }

    private fun updateUI() {

        val hasTemp = thermometer.hasValidReading
        val hasHumidity = hygrometer.hasValidReading

        if (!hasTemp) {
            tempTxt.text = getString(R.string.dash)
        } else {
            tempTxt.text =
                formatService.formatTemperature(thermometer.temperature, prefs.temperatureUnits)
        }

        if (!hasHumidity) {
            humidityTxt.text = getString(R.string.no_humidity_data)
        } else {
            humidityTxt.text = formatService.formatHumidity(hygrometer.humidity)
        }


        if (hasTemp && hasHumidity) {
            val heatIndex =
                weatherService.getHeatIndex(thermometer.temperature, hygrometer.humidity)
            val alert = weatherService.getHeatAlert(heatIndex)
            val dewPoint = weatherService.getDewPoint(thermometer.temperature, hygrometer.humidity)
            dewPointTxt.text = getString(
                R.string.dew_point,
                formatService.formatTemperature(dewPoint, prefs.temperatureUnits)
            )
            showHeatAlert(alert)
        } else if (hasTemp) {
            val alert =
                weatherService.getHeatAlert(
                    weatherService.getHeatIndex(
                        thermometer.temperature,
                        50f
                    )
                )
            showHeatAlert(alert)
        } else {
            showHeatAlert(HeatAlert.Normal)
        }

    }

    private fun showHeatAlert(alert: HeatAlert) {
        if (alert != HeatAlert.Normal) {
            heatAlertImg.visibility = View.VISIBLE
        } else {
            heatAlertImg.visibility = View.INVISIBLE
        }

        val alertColor = when (alert) {
            HeatAlert.FrostbiteCaution, HeatAlert.FrostbiteWarning, HeatAlert.FrostbiteDanger -> UiUtils.color(
                requireContext(),
                R.color.colorAccent
            )
            else -> UiUtils.color(requireContext(), R.color.colorPrimary)
        }

        heatAlertImg.imageTintList = ColorStateList.valueOf(alertColor)

        heatAlertTitle = alert.toString()
        heatAlertContent = alert.toString()
    }

    private fun onTemperatureUpdate(): Boolean {
        updateUI()
        return true
    }

    private fun onHumidityUpdate(): Boolean {
        updateUI()
        return true
    }
}
