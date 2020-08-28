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

    private var barometer: IBarometer? = null
    private lateinit var altimeterCalibrator: AltimeterCalibrator
    private lateinit var gps: IGPS
    private lateinit var prefs: UserPreferences
    private val throttle = Throttle(20)
    private var gpsStarted = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sensor_compass, container, false)

        prefs = UserPreferences(requireContext())
        barometer = if (prefs.weather.hasBarometer) Barometer(requireContext()) else null
        gps = GPS(requireContext())
        altimeterCalibrator = AltimeterCalibrator(requireContext())

        return view
    }

    override fun onResume() {
        super.onResume()
        barometer?.start(this::updateBarometer)
        if (prefs.useLocationFeatures) {
            startGps()
        }
    }

    override fun onPause() {
        super.onPause()
        barometer?.stop(this::updateBarometer)
        stopGps()
    }

    private fun startGps() {
        if (gpsStarted) {
            return
        }
        gpsStarted = true
        gps.start(this::updateGps)
    }

    private fun stopGps() {
        gpsStarted = false
        gps.stop(this::updateGps)
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