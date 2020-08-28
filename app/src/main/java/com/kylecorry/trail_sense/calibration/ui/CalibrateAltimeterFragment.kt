package com.kylecorry.trail_sense.calibration.ui

import android.app.AlertDialog
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.calibration.infrastructure.AltimeterCalibrator
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trail_sense.shared.Throttle
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.*
import kotlin.math.roundToInt


class CalibrateAltimeterFragment : Fragment() {

    private lateinit var barometer: IBarometer
    private lateinit var altimeterCalibrator: AltimeterCalibrator
    private lateinit var gps: IGPS
    private lateinit var altimeter: IAltimeter
    private lateinit var prefs: UserPreferences
    private lateinit var sensorService: SensorService
    private val throttle = Throttle(20)
    private var altimeterStarted = false
    private lateinit var distanceUnits: UserPreferences.DistanceUnits

    private lateinit var altitudeTxt: TextView
    private lateinit var autoAltitudeSwitch: SwitchCompat
    private lateinit var altitudeOffsetsSwitch: SwitchCompat
    private lateinit var fineTuneSwitch: SwitchCompat
    private lateinit var altitudeOverrideEdit: EditText
    private lateinit var altitudeOverrideBtn: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sensor_altimeter, container, false)

        altitudeTxt = view.findViewById(R.id.altitude_value)
        autoAltitudeSwitch = view.findViewById(R.id.auto_altitude)
        altitudeOffsetsSwitch = view.findViewById(R.id.altitude_offsets)
        altitudeOverrideEdit = view.findViewById(R.id.altitude_override)
        altitudeOverrideBtn = view.findViewById(R.id.altitude_override_button)
        fineTuneSwitch = view.findViewById(R.id.fine_tune)

        prefs = UserPreferences(requireContext())
        sensorService = SensorService(requireContext())

        gps = GPS(requireContext())
        barometer = sensorService.getBarometer()
        altimeter = sensorService.getAltimeter(gps)

        altimeterCalibrator = AltimeterCalibrator(requireContext())

        distanceUnits = prefs.distanceUnits

        altitudeOffsetsSwitch.isChecked = prefs.useAltitudeOffsets
        autoAltitudeSwitch.isChecked = prefs.useAutoAltitude
        fineTuneSwitch.isChecked = prefs.useFineTuneAltitude
        altitudeOverrideEdit.setText(
            LocationMath.convertToBaseUnit(
                prefs.altitudeOverride,
                distanceUnits
            ).toString()
        )

        altitudeOffsetsSwitch.isEnabled = prefs.useAutoAltitude
        altitudeOverrideBtn.isEnabled = !prefs.useAutoAltitude
        altitudeOverrideEdit.isEnabled = !prefs.useAutoAltitude
        fineTuneSwitch.isEnabled = prefs.useAutoAltitude

        if (!prefs.weather.hasBarometer) {
            fineTuneSwitch.visibility = View.GONE
        }

        autoAltitudeSwitch.setOnCheckedChangeListener { _, isChecked ->

            prefs.useAutoAltitude = isChecked

            stopAltimeter()
            altimeter = sensorService.getAltimeter()
            startAltimeter()

            altitudeOffsetsSwitch.isEnabled = isChecked
            altitudeOverrideBtn.isEnabled = !isChecked
            altitudeOverrideEdit.isEnabled = !isChecked
            fineTuneSwitch.isEnabled = isChecked
            updateAltitude()
        }

        altitudeOffsetsSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.useAltitudeOffsets = isChecked
            updateAltitude()
        }

        fineTuneSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.useFineTuneAltitude = isChecked
            stopAltimeter()
            altimeter = sensorService.getAltimeter()
            startAltimeter()
            updateAltitude()
        }

        altitudeOverrideBtn.setOnClickListener {
            val that = this
            val dialog: AlertDialog? = activity?.let {
                val builder = AlertDialog.Builder(it)
                builder.apply {
                    setPositiveButton(R.string.altitude_from_gps) { dialog, _ ->
                        gps.start(that::updateElevationFromGPS)
                        dialog.dismiss()
                    }
                    setNegativeButton(R.string.altitude_from_pressure) { dialog, _ ->
                        barometer.start(that::updateElevationFromBarometer)
                        dialog.dismiss()
                    }
                    setNeutralButton(R.string.dialog_cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                }
                builder.create()
            }
            dialog?.show()
            updateAltitude()
        }

        altitudeOverrideEdit.doAfterTextChanged {
            updateAltitude()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        startAltimeter()
    }

    override fun onPause() {
        super.onPause()
        barometer.stop(this::updateElevationFromBarometer)
        gps.stop(this::updateElevationFromGPS)
        stopAltimeter()
    }

    private fun updateElevationFromGPS(): Boolean {
        val elevation = gps.altitude
        altitudeOverrideEdit.setText(
            LocationMath.convertToBaseUnit(
                elevation,
                distanceUnits
            ).toString()
        )
        updateAltitude()
        return false
    }

    private fun startAltimeter() {
        if (altimeterStarted) {
            return
        }
        altimeterStarted = true
        altimeter.start(this::updateAltitude)
    }

    private fun stopAltimeter() {
        altimeterStarted = false
        altimeter.stop(this::updateAltitude)
    }

    private fun updateElevationFromBarometer(): Boolean {
        val pressure = barometer.pressure

        // TODO: Display dialog to enter sea level pressure

        val altitude =
            SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure)
        altitudeOverrideEdit.setText(
            LocationMath.convertToBaseUnit(
                altitude,
                distanceUnits
            ).toString()
        )
        updateAltitude()
        return false
    }

    private fun updateAltitude(): Boolean {

        if (throttle.isThrottled()) {
            return true
        }

        val altitudeOverrideValue =
            if (altitudeOverrideEdit.text.isNullOrEmpty()) 0f else altitudeOverrideEdit.text.toString()
                .toFloatOrNull()
        if (altitudeOverrideValue != null) {
            if (distanceUnits == UserPreferences.DistanceUnits.Feet) {
                prefs.altitudeOverride = LocationMath.convertToMeters(
                    altitudeOverrideValue,
                    UserPreferences.DistanceUnits.Feet
                )
            } else {
                prefs.altitudeOverride = altitudeOverrideValue
            }
        }

        altitudeTxt.text = if (distanceUnits == UserPreferences.DistanceUnits.Meters) {
            "${altimeter.altitude.roundToInt()} m"
        } else {
            "${
                LocationMath.convertToBaseUnit(altimeter.altitude, distanceUnits)
                    .roundToInt()
            } ft"
        }
        return true
    }


}