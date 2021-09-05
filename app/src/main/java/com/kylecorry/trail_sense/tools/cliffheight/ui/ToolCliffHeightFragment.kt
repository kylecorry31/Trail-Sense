package com.kylecorry.trail_sense.tools.cliffheight.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.science.physics.PhysicsService
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolCliffHeightBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.Duration
import java.time.Instant

class ToolCliffHeightFragment : BoundFragment<FragmentToolCliffHeightBinding>() {

    private val physicsService = PhysicsService()
    private val intervalometer = Timer {
        update()
    }
    private val formatService by lazy { FormatService(requireContext()) }
    private val userPrefs by lazy { UserPreferences(requireContext()) }

    private lateinit var units: DistanceUnits
    private var startTime: Instant? = null
    private var running = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.startBtn.setOnClickListener {
            if (running) {
                intervalometer.stop()
                running = false
            } else {
                startTime = Instant.now()
                intervalometer.interval(16)
                running = true
            }
            binding.startBtn.setState(running)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CustomUiUtils.disclaimer(
            requireContext(),
            getString(R.string.disclaimer_message_title),
            getString(R.string.tool_cliff_height_disclaimer),
            "cache_dialog_tool_cliff_height"
        )
    }

    override fun onResume() {
        super.onResume()
        units = userPrefs.baseDistanceUnits
    }

    override fun onPause() {
        super.onPause()
        intervalometer.stop()
    }

    fun update() {
        if (startTime == null) {
            return
        }

        val duration = Duration.between(startTime, Instant.now())
        val height = physicsService.fallHeight(duration)
        val converted = height.convertTo(units)
        val formatted = formatService.formatDistance(converted, 2)

        binding.height.text = formatted
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolCliffHeightBinding {
        return FragmentToolCliffHeightBinding.inflate(layoutInflater, container, false)
    }

}