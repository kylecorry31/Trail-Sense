package com.kylecorry.trail_sense.tools.level.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentLevelBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.toDegrees
import com.kylecorry.trailsensecore.domain.math.Vector3
import com.kylecorry.trailsensecore.domain.math.cosDegrees
import com.kylecorry.trailsensecore.domain.math.sinDegrees
import com.kylecorry.trailsensecore.infrastructure.sensors.orientation.DeviceOrientationSensor
import com.kylecorry.trailsensecore.infrastructure.system.*
import com.kylecorry.trailsensecore.infrastructure.time.Throttle
import kotlin.math.*

class LevelFragment : Fragment() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }
    private val orientationSensor by lazy { OrientationSensor2(requireContext()) }
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

        if (throttle.isThrottled()) {
            return true
        }

        val x = orientationSensor.orientation.x
        val y = orientationSensor.orientation.y

        align(
            binding.bubbleX,
            null,
            HorizontalConstraint(binding.bubbleXBackground, HorizontalConstraintType.Left),
            null,
            HorizontalConstraint(binding.bubbleXBackground, HorizontalConstraintType.Right),
            0f,
            (x + 90) / 180f
        )
        align(
            binding.bubbleY,
            VerticalConstraint(binding.bubbleYBackground, VerticalConstraintType.Top),
            null,
            VerticalConstraint(binding.bubbleYBackground, VerticalConstraintType.Bottom),
            null,
            (y + 90) / 180f,
            0f
        )

        alignToVector(
            binding.crosshairs,
            binding.bubble,
            180f * Vector3(x / 90f, y / 90f, 0f).magnitude(),
            atan2(y, x).toDegrees() + 180
        )

        binding.angles.text = getString(
            R.string.bubble_level_angles,
            formatService.formatDegrees(abs(x)),
            formatService.formatDegrees(abs(y))
        )
        binding.bubbleOutline.x = binding.root.width / 2f - binding.bubbleOutline.width / 2f
        binding.bubbleOutline.y = binding.root.height / 2f - binding.bubbleOutline.height / 2f

        return true
    }

}