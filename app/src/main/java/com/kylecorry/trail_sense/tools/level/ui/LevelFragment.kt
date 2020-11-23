package com.kylecorry.trail_sense.tools.level.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.trail_sense.databinding.FragmentLevelBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.infrastructure.time.Throttle
import kotlin.math.abs

class LevelFragment : Fragment() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }
    private val orientationSensor by lazy { sensorService.getOrientationSensor() }
    private var _binding: FragmentLevelBinding? = null
    private val binding get() = _binding!!
    private val throttle = Throttle(20)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLevelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        orientationSensor.start(this::onOrientationUpdate)
    }

    override fun onPause() {
        orientationSensor.stop(this::onOrientationUpdate)
        super.onPause()
    }

    private fun onOrientationUpdate(): Boolean {

        if (throttle.isThrottled()){
            return true
        }

        val x = orientationSensor.orientation.x
        val y = orientationSensor.orientation.y

        binding.bubble.x = (x + 90) / 180f * binding.root.width - binding.bubble.width / 2f
        binding.bubble.y = (y + 90) / 180f * binding.root.height - binding.bubble.height / 2f
        binding.angleX.text = formatService.formatDegrees(abs(x))
        binding.angleX2.text = formatService.formatDegrees(abs(x))
        binding.angleY.text = formatService.formatDegrees(abs(y))
        binding.angleY2.text = formatService.formatDegrees(abs(y))
        binding.bubbleOutline.x = binding.root.width / 2f - binding.bubbleOutline.width / 2f
        binding.bubbleOutline.y = binding.root.height / 2f - binding.bubbleOutline.height / 2f

        return true
    }

}