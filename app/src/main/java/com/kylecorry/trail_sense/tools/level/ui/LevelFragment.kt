package com.kylecorry.trail_sense.tools.level.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.andromeda.core.ui.HorizontalConstraint
import com.kylecorry.andromeda.core.ui.HorizontalConstraintType
import com.kylecorry.andromeda.core.ui.VerticalConstraint
import com.kylecorry.andromeda.core.ui.VerticalConstraintType
import com.kylecorry.andromeda.core.ui.align
import com.kylecorry.andromeda.core.ui.alignToVector
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.sense.level.Level
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.math.Vector3
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentLevelBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.sensors.SensorService
import kotlin.math.abs
import kotlin.math.atan2

class LevelFragment : BoundFragment<FragmentLevelBinding>() {

    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val level by lazy { Level(SensorService(requireContext()).getOrientation()) }
    private val throttle = Throttle(20)

    override fun onResume() {
        super.onResume()
        level.start(this::onLevelUpdate)
    }

    override fun onPause() {
        level.stop(this::onLevelUpdate)
        super.onPause()
    }

    private fun onLevelUpdate(): Boolean {

        if (throttle.isThrottled()) {
            return true
        }

        val x = level.x
        val y = -level.y
        binding.bubbleX.align(
            null,
            HorizontalConstraint(binding.bubbleXBackground, HorizontalConstraintType.Left),
            null,
            HorizontalConstraint(binding.bubbleXBackground, HorizontalConstraintType.Right),
            0f,
            (x + 90) / 180f
        )
        binding.bubbleY.align(
            VerticalConstraint(binding.bubbleYBackground, VerticalConstraintType.Top),
            null,
            VerticalConstraint(binding.bubbleYBackground, VerticalConstraintType.Bottom),
            null,
            (y + 90) / 180f,
            0f
        )

        binding.bubble.alignToVector(
            binding.crosshairs,
            180f * Vector3(x / 90f, y / 90f, 0f).magnitude(),
            atan2(y, x).toDegrees() + 180
        )

        binding.levelTitle.title.text = getString(
            R.string.bubble_level_angles,
            formatService.formatDegrees(abs(x), 1),
            formatService.formatDegrees(abs(y), 1)
        )
        binding.bubbleX.text = formatService.formatDegrees(abs(x))
        binding.bubbleY.text = formatService.formatDegrees(abs(y))

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