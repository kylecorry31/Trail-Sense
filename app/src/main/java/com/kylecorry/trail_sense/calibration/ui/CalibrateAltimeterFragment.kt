package com.kylecorry.trail_sense.calibration.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.calibration.infrastructure.AltimeterCalibrator
import com.kylecorry.trail_sense.shared.Throttle
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.*


class CalibrateAltimeterFragment : Fragment() {

    private lateinit var barometer: IBarometer
    private lateinit var altimeterCalibrator: AltimeterCalibrator
    private lateinit var altimeter: IAltimeter
    private lateinit var prefs: UserPreferences
    private lateinit var sensorService: SensorService
    private val throttle = Throttle(20)
    private var gpsStarted = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sensor_altimeter, container, false)

        prefs = UserPreferences(requireContext())
        sensorService = SensorService(requireContext())

        barometer = sensorService.getBarometer()
        altimeter = sensorService.getAltimeter()

        altimeterCalibrator = AltimeterCalibrator(requireContext())

        return view
    }

    override fun onResume() {
        super.onResume()
        barometer.start(this::updateBarometer)
        startGps()
    }

    override fun onPause() {
        super.onPause()
        barometer.stop(this::updateBarometer)
        stopGps()
    }

    private fun startGps() {
        if (gpsStarted) {
            return
        }
        gpsStarted = true
        altimeter.start(this::updateGps)
    }

    private fun stopGps() {
        gpsStarted = false
        altimeter.stop(this::updateGps)
    }


    private fun updateGps(): Boolean {
        updateBarometer()
        return true
    }

    private fun updateBarometer(): Boolean {

        if (throttle.isThrottled()) {
            return true
        }


        return true
    }


}