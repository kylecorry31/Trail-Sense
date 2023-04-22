package com.kylecorry.trail_sense.tools.metaldetector.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.sense.accelerometer.GravitySensor
import com.kylecorry.andromeda.sense.magnetometer.LowPassMagnetometer
import com.kylecorry.andromeda.sense.magnetometer.Magnetometer
import com.kylecorry.sol.math.Quaternion
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.filters.LowPassFilter
import com.kylecorry.sol.science.physics.PhysicsService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolMetalDetectorBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import java.time.Duration
import kotlin.math.roundToInt

class FragmentToolMetalDetector : BoundFragment<FragmentToolMetalDetectorBinding>() {
    private val magnetometer by lazy { Magnetometer(requireContext()) }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val metalDetectionService = PhysicsService()
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

    private val calibrateTimer = Timer {
        calibratedField = lowPassMagnetometer.magneticField
        calibratedOrientation = orientation.orientation
    }

    private val haptics by lazy { HapticSubsystem.getInstance(requireContext()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chart = MetalDetectorChart(
            binding.metalChart,
            Resources.getAndroidColorAttr(requireContext(), R.attr.colorPrimary)
        )
        binding.calibrateBtn.setOnClickListener {
            binding.threshold.progress = magnetometer.magneticField.magnitude().roundToInt() + 5
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
        haptics.off()
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
                null // TODO: Once a better orientation is calculated, use that
            )
            val direction = metalDetectionService.getMetalDirection(
                metal,
                gravity.acceleration
            )
            binding.magnetometerView.setFieldStrength(metal.magnitude())
            binding.magnetometerView.setMetalDirection(direction)
            binding.magnetometerView.setSensitivity(prefs.metalDetector.directionSensitivity)
        }
        val magneticField = filter.filter(magnetometer.magneticField.magnitude())

        if (System.currentTimeMillis() - lastReadingTime > 20 && magneticField != 0f) {
            readings.add(magneticField)
            if (readings.size > 150) {
                readings.removeAt(0)
            }
            lastReadingTime = System.currentTimeMillis()
            chart.plot(readings, threshold)
        }

        threshold = binding.threshold.progress.toFloat()
        binding.thresholdAmount.text = formatService.formatMagneticField(threshold)

        val metalDetected = metalDetectionService.isMetal(magneticField, threshold)
        binding.metalDetectorTitle.title.text = formatService.formatMagneticField(magneticField)
        binding.metalDetectorTitle.title.setCompoundDrawables(
            Resources.dp(requireContext(), 24f).toInt(),
            right = if (metalDetected) R.drawable.metal else null
        )
        CustomUiUtils.setImageColor(
            binding.metalDetectorTitle.title,
            Resources.androidTextColorSecondary(requireContext())
        )

        if (metalDetected && !isVibrating) {
            isVibrating = true
            haptics.interval(VIBRATION_DURATION)
        } else if (!metalDetected) {
            isVibrating = false
            haptics.off()
        }
    }

    private fun onMagnetometerUpdate(): Boolean {
        update()
        return true
    }

    companion object {
        private val VIBRATION_DURATION = Duration.ofMillis(200)
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolMetalDetectorBinding {
        return FragmentToolMetalDetectorBinding.inflate(layoutInflater, container, false)
    }

}