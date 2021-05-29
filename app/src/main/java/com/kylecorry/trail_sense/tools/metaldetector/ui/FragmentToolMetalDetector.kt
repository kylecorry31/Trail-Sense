package com.kylecorry.trail_sense.tools.metaldetector.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolMetalDetectorBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.weather.domain.LowPassFilter
import com.kylecorry.trailsensecore.domain.math.Quaternion
import com.kylecorry.trailsensecore.domain.math.Vector3
import com.kylecorry.trailsensecore.domain.metaldetection.MetalDetectionService
import com.kylecorry.trailsensecore.infrastructure.sensors.accelerometer.GravitySensor
import com.kylecorry.trailsensecore.infrastructure.sensors.magnetometer.LowPassMagnetometer
import com.kylecorry.trailsensecore.infrastructure.sensors.magnetometer.Magnetometer
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import com.kylecorry.trailsensecore.infrastructure.time.Throttle
import com.kylecorry.trailsensecore.infrastructure.vibration.Vibrator
import com.kylecorry.trailsensecore.infrastructure.view.BoundFragment
import java.time.Duration
import kotlin.math.roundToInt

class FragmentToolMetalDetector : BoundFragment<FragmentToolMetalDetectorBinding>() {
    private val magnetometer by lazy { Magnetometer(requireContext()) }
    private val vibrator by lazy { Vibrator(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }
    private val metalDetectionService = MetalDetectionService()
    private val lowPassMagnetometer by lazy { LowPassMagnetometer(requireContext()) }
    private val orientation by lazy { SensorService(requireContext()).getGyroscope() }
    private val gravity by lazy { GravitySensor(requireContext()) }

    private val filter = LowPassFilter(0.2f, 0f)

    private var isVibrating = false

    private lateinit var chart: MetalDetectorChart
    private var lastReadingTime = System.currentTimeMillis() + 1000L

    private var threshold = 65f

    private val readings = mutableListOf<Float>()

    private val throttle = Throttle(20)
    private val prefs by lazy { UserPreferences(requireContext()) }

    private var calibratedField = Vector3.zero
    private var calibratedOrientation = Quaternion.zero

    private val calibrateTimer = Intervalometer {
        calibratedField = lowPassMagnetometer.magneticField
        calibratedOrientation = orientation.orientation
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chart = MetalDetectorChart(
            binding.metalChart,
            UiUtils.color(requireContext(), R.color.colorPrimary)
        )
        binding.calibrateBtn.setOnClickListener {
            binding.threshold.progress =
                metalDetectionService.getFieldStrength(magnetometer.magneticField).roundToInt() + 5
            if (prefs.metalDetector.showMetalDirection) {
                calibratedField = lowPassMagnetometer.magneticField
                calibratedOrientation = orientation.orientation
                calibrateTimer.stop()
            }
        }
        binding.magnetometerView.isVisible = prefs.metalDetector.showMetalDirection
    }

    override fun onResume() {
        super.onResume()
        binding.magnetometerView.setSinglePoleMode(prefs.metalDetector.showSinglePole)
        magnetometer.start(this::onMagnetometerUpdate)
        if (prefs.metalDetector.showMetalDirection) {
            lowPassMagnetometer.start(this::onLowPassMagnetometerUpdate)
            orientation.start(this::onMagnetometerUpdate)
            gravity.start(this::onMagnetometerUpdate)
            calibrateTimer.once(Duration.ofSeconds(2))
        }
    }

    override fun onPause() {
        super.onPause()
        magnetometer.stop(this::onMagnetometerUpdate)
        if (prefs.metalDetector.showMetalDirection) {
            lowPassMagnetometer.stop(this::onLowPassMagnetometerUpdate)
            orientation.stop(this::onMagnetometerUpdate)
            gravity.stop(this::onMagnetometerUpdate)
            calibrateTimer.stop()
        }
        vibrator.stop()
        isVibrating = false
    }

    private fun onLowPassMagnetometerUpdate(): Boolean {
        update()
        return true
    }

    private fun update() {

        if (throttle.isThrottled()) {
            return
        }

        if (prefs.metalDetector.showMetalDirection) {
            val orientation = orientation.orientation.subtractRotation(calibratedOrientation)
            val metal = metalDetectionService.removeGeomagneticField(
                lowPassMagnetometer.magneticField,
                calibratedField,
                orientation
            )
            val direction = metalDetectionService.getMetalDirection(
                metal,
                gravity.acceleration
            )
            binding.magnetometerView.setFieldStrength(metalDetectionService.getFieldStrength(metal))
            binding.magnetometerView.setMetalDirection(direction)
            binding.magnetometerView.setSensitivity(prefs.metalDetector.directionSensitivity)
        }
        val magneticField =
            filter.filter(metalDetectionService.getFieldStrength(magnetometer.magneticField))

        if (System.currentTimeMillis() - lastReadingTime > 20 && magneticField != 0f) {
            readings.add(magneticField)
            if (readings.size > 150) {
                readings.removeAt(0)
            }
            lastReadingTime = System.currentTimeMillis()
            chart.plot(readings)
        }

        threshold = binding.threshold.progress.toFloat()
        binding.thresholdAmount.text = formatService.formatMagneticField(threshold)

        val metalDetected = metalDetectionService.isMetal(magneticField, threshold)
        binding.magneticField.text = formatService.formatMagneticField(magneticField)
        binding.metalDetected.visibility = if (metalDetected) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }

        if (metalDetected && !isVibrating) {
            isVibrating = true
            vibrator.vibrate(Duration.ofMillis(VIBRATION_DURATION))
        } else if (!metalDetected) {
            isVibrating = false
            vibrator.stop()
        }
    }

    private fun onMagnetometerUpdate(): Boolean {
        update()
        return true
    }

    companion object {
        private const val VIBRATION_DURATION = 100L
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolMetalDetectorBinding {
        return FragmentToolMetalDetectorBinding.inflate(layoutInflater, container, false)
    }

}