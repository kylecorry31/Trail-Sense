package com.kylecorry.trail_sense.tools.metaldetector.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.sound.ISoundPlayer
import com.kylecorry.sol.math.Quaternion
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.filters.LowPassFilter
import com.kylecorry.sol.science.physics.PhysicsService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolMetalDetectorBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryColor
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.whistle.infrastructure.Whistle
import java.time.Duration
import kotlin.math.absoluteValue

class FragmentToolMetalDetector : BoundFragment<FragmentToolMetalDetectorBinding>() {
    private val sensors by lazy { SensorService(requireContext()) }
    private val magnetometer by lazy { sensors.getMagnetometer() }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val metalDetectionService = PhysicsService()
    private val lowPassMagnetometer by lazy { sensors.getMagnetometer(true) }
    private val orientation by lazy { SensorService(requireContext()).getGyroscope() }
    private val gravity by lazy { sensors.getGravity() }

    private val filter = LowPassFilter(0.2f, 0f)

    private var isVibrating = false

    private lateinit var chart: MetalDetectorChart
    private var lastReadingTime = System.currentTimeMillis() + 1000L

    private var threshold = 5f

    private val readings = mutableListOf<Float>()

    private val throttle = Throttle(20)
    private val prefs by lazy { UserPreferences(requireContext()) }

    private var isHighSensitivity = false
    private var calibratedField = Vector3.zero
    private var calibratedOrientation = Quaternion.zero
    private var referenceMagnitude = 0f

    private val calibrateTimer = CoroutineTimer {
        calibrate()
    }

    private val haptics by lazy { HapticSubsystem.getInstance(requireContext()) }

    private val isMetalDetected = Debouncer(Duration.ofMillis(100))

    private  var whistle: ISoundPlayer? = null

    private var volume: Float = 0.0f // Initial volume

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chart = MetalDetectorChart(
            binding.metalChart,
            Resources.getPrimaryColor(requireContext())
        )
        binding.calibrateBtn.setOnClickListener {
            calibrate()
        }
        binding.magnetometerView.isVisible = prefs.metalDetector.showMetalDirection

        binding.highSensitivityToggle.setOnCheckedChangeListener { _, isChecked ->
            isHighSensitivity = isChecked
            if (isChecked){
                isMetalDetected.debounceTime = Duration.ofMillis(50)
            } else {
                isMetalDetected.debounceTime = Duration.ofMillis(100)
            }
        }



        binding.metalDetectorTitle.rightButton.setOnClickListener {
            if (prefs.isMetalAudioEnabled){
                prefs.isMetalAudioEnabled = false
                whistle?.off()
            } else {
                prefs.isMetalAudioEnabled = true
                whistle?.on()
            }
            CustomUiUtils.setButtonState(binding.metalDetectorTitle.rightButton, prefs.isMetalAudioEnabled)
        }
    }

    /**
     *  initialize Whistle onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inBackground {
            whistle = Whistle()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.magnetometerView.setSinglePoleMode(prefs.metalDetector.showSinglePole)
        magnetometer.start(this::onMagnetometerUpdate)
        if (prefs.metalDetector.showMetalDirection) {
            lowPassMagnetometer.start(this::onLowPassMagnetometerUpdate)
            orientation.start(this::onMagnetometerUpdate)
            gravity.start(this::onMagnetometerUpdate)
        }
        calibrateTimer.once(Duration.ofSeconds(2))

        if (prefs.isMetalAudioEnabled){
            whistle?.on()
        }
        CustomUiUtils.setButtonState(binding.metalDetectorTitle.rightButton, prefs.isMetalAudioEnabled)
    }

    override fun onPause() {
        super.onPause()
        magnetometer.stop(this::onMagnetometerUpdate)
        if (prefs.metalDetector.showMetalDirection) {
            lowPassMagnetometer.stop(this::onLowPassMagnetometerUpdate)
            orientation.stop(this::onMagnetometerUpdate)
            gravity.stop(this::onMagnetometerUpdate)
        }
        calibrateTimer.stop()
        haptics.off()
        isVibrating = false
        whistle?.off()
    }

    private fun calibrate(){
        referenceMagnitude = readings.takeLast(20).average().toFloat()
        calibratedField = lowPassMagnetometer.magneticField
        calibratedOrientation = orientation.orientation
        calibrateTimer.stop()
    }

    private fun onLowPassMagnetometerUpdate(): Boolean {
        update()
        return true
    }

    private fun update() {

        if (throttle.isThrottled()) {
            return
        }

        // Update the metal direction dial
        if (prefs.metalDetector.showMetalDirection) {
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

        val magneticField = getCurrentMagneticFieldStrength()

        // Record the magnetic field
        if (canAddReading(magneticField)) {
            addReading(magneticField)
            updateChart()
        }




        // Update the threshold from the slider
        updateThreshold()

        // Update the title
        binding.metalDetectorTitle.title.text = formatService.formatMagneticField(magneticField)

        // TODO: Have two methods: onMetalFound and onMetalLost
        val metalDetected = isMetalDetected(magneticField)
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

        updateMetalSoundIntensity(magneticField)
    }
    private fun updateMetalSoundIntensity(reading: Float) {
        val delta = (reading - referenceMagnitude).absoluteValue
        volume = SolMath.map(delta - threshold, 0f, 30f, 0f, 1f, true)
        whistle?.setVolume(volume) // Set the volume
    }

    private fun getCurrentMagneticFieldStrength(): Float {
        val filtered = filter.filter(magnetometer.magneticField.magnitude())
        return if (isHighSensitivity){
            magnetometer.magneticField.magnitude()
        } else {
            filtered
        }
    }

    private fun isMetalDetected(reading: Float):  Boolean {
        val delta = (reading - referenceMagnitude).absoluteValue
        val current = delta >= threshold && referenceMagnitude != 0f

        isMetalDetected.update(current)
        return isMetalDetected.value
    }

    private fun canAddReading(reading: Float): Boolean {
        return System.currentTimeMillis() - lastReadingTime > 20 && reading != 0f
    }

    private fun updateThreshold(){
        threshold = (binding.threshold.progress.toFloat() / 10f).coerceAtLeast(0.1f)
        binding.thresholdAmount.text = formatService.formatMagneticField(threshold, decimalPlaces = 1)
    }

    private fun addReading(reading: Float){
        readings.add(reading)
        if (readings.size > 150){
            readings.removeAt(0)
        }
        lastReadingTime = System.currentTimeMillis()
    }

    private fun updateChart(){
        chart.plot(readings, referenceMagnitude - threshold, referenceMagnitude + threshold)
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