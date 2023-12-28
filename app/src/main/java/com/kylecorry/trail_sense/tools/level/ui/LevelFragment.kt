package com.kylecorry.trail_sense.tools.level.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.sense.level.Level
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentLevelBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.sensors.SensorService
import kotlin.math.abs

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

        binding.level.xAngle = x
        binding.level.yAngle = y

        binding.levelTitle.title.text = getString(
            R.string.bubble_level_angles,
            formatService.formatDegrees(abs(x), 1),
            formatService.formatDegrees(abs(y), 1)
        )
        return true
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentLevelBinding {
        return FragmentLevelBinding.inflate(layoutInflater, container, false)
    }

}