package com.kylecorry.trail_sense.tools.metaldetector.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.databinding.FragmentInclinometerBinding
import com.kylecorry.trail_sense.databinding.FragmentToolMetalDetectorBinding
import com.kylecorry.trail_sense.databinding.FragmentToolTriangulateBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Vibrator
import com.kylecorry.trailsensecore.infrastructure.sensors.magnetometer.Magnetometer
import java.time.Duration
import kotlin.math.roundToInt

class FragmentToolMetalDetector : Fragment() {

    private var _binding: FragmentToolMetalDetectorBinding? = null
    private val binding get() = _binding!!

    private val magnetometer by lazy { Magnetometer(requireContext()) }
    private val vibrator by lazy { Vibrator(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }
    private var isVibrating = false

    private var threshold = 65f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentToolMetalDetectorBinding.inflate(layoutInflater, container, false)
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
        val magneticField = magnetometer.magneticField.magnitude()

        threshold = binding.threshold.progress.toFloat()
        binding.thresholdAmount.text = formatService.formatMagneticField(threshold)

        val metalDetected = magneticField >= threshold
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