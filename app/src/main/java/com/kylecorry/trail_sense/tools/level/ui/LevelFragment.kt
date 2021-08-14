package com.kylecorry.trail_sense.tools.level.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentLevelBinding
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trailsensecore.domain.math.Vector3
import com.kylecorry.trailsensecore.domain.math.toDegrees
import com.kylecorry.trailsensecore.infrastructure.sensors.orientation.GravityOrientationSensor
import com.kylecorry.trailsensecore.infrastructure.system.*
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.andromeda.fragments.BoundFragment
import kotlin.math.abs
import kotlin.math.atan2

class LevelFragment : BoundFragment<FragmentLevelBinding>() {

    private val formatService by lazy { FormatServiceV2(requireContext()) }
    // TODO: Eventually switch to the rotation sensors
    private val orientationSensor by lazy { GravityOrientationSensor(requireContext()) }
    private val throttle = Throttle(20)

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

        val euler = orientationSensor.orientation.toEuler()
        val x = when {
            euler.roll in -90f..90f -> euler.roll
            euler.roll > 90f -> 180 - euler.roll
            else -> -(180 + euler.roll)
        }
        val y = euler.pitch
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

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentLevelBinding {
        return FragmentLevelBinding.inflate(layoutInflater, container, false)
    }

}