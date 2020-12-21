package com.kylecorry.trail_sense.tools.metaldetector.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolMetalDetectorBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trailsensecore.domain.metaldetection.MetalDetectionService
import com.kylecorry.trailsensecore.infrastructure.sensors.magnetometer.Magnetometer
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.vibration.Vibrator
import java.time.Duration
import kotlin.math.roundToInt

class FragmentToolMetalDetector : Fragment() {

    private var _binding: FragmentToolMetalDetectorBinding? = null
    private val binding get() = _binding!!

    private val magnetometer by lazy { Magnetometer(requireContext()) }
    private val vibrator by lazy { Vibrator(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }
    private val metalDetectionService = MetalDetectionService()

    private var isVibrating = false

    private lateinit var chart: MetalDetectorChart
    private var lastReadingTime = System.currentTimeMillis() + 1000L

    private var threshold = 65f

    private val readings = mutableListOf<Float>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentToolMetalDetectorBinding.inflate(layoutInflater, container, false)
        chart = MetalDetectorChart(
            binding.metalChart,
            UiUtils.color(requireContext(), R.color.colorPrimary)
        )
        binding.calibrateBtn.setOnClickListener {
            binding.threshold.progress = magnetometer.magneticField.magnitude().roundToInt() + 5
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        magnetometer.start(this::onMagnetometerUpdate)
    }

    override fun onPause() {
        super.onPause()
        magnetometer.stop(this::onMagnetometerUpdate)
        vibrator.stop()
        isVibrating = false
    }

    private fun onMagnetometerUpdate(): Boolean {
        val magneticField = metalDetectionService.getFieldStrength(magnetometer.magneticField)

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

        val metalDetected = metalDetectionService.isMetal(magnetometer.magneticField, threshold)
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
        return true
    }

    companion object {
        private const val VIBRATION_DURATION = 100L
    }

}