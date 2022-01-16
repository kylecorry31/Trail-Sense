package com.kylecorry.trail_sense.tools.cliffheight.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolCliffHeightBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.PressState
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.cliffheight.domain.CliffHeightService
import java.time.Duration
import java.time.Instant

class ToolCliffHeightFragment : BoundFragment<FragmentToolCliffHeightBinding>() {

    private val service = CliffHeightService()
    private val timer = Timer {
        update()
    }
    private val formatService by lazy { FormatService(requireContext()) }
    private val userPrefs by lazy { UserPreferences(requireContext()) }
    private val gps by lazy { SensorService(requireContext()).getGPS(false) }

    private lateinit var units: DistanceUnits
    private var startTime: Instant? = null
    private var running = false
    private var location: Coordinate? = null

    private val singlePressDuration = Duration.ofMillis(200)

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.startBtn.setOnTouchListener { _, event ->
            when (event.action){
                MotionEvent.ACTION_UP -> updateState(PressState.Up)
                MotionEvent.ACTION_DOWN -> updateState(PressState.Down)
            }
            true
        }
    }

    private fun updateState(action: PressState) {
        if (!running) {
            if (action == PressState.Down) {
                start()
                running = true
            }
        } else {
            if (action == PressState.Up && Duration.between(
                    startTime,
                    Instant.now()
                ) > singlePressDuration
            ) {
                stop()
                running = false
            }

            if (action == PressState.Down) {
                stop()
                running = false
            }
        }
    }

    private fun stop() {
        timer.stop()
        binding.startBtn.setState(false)

    }

    private fun start() {
        startTime = Instant.now()
        timer.interval(16)
        binding.startBtn.setState(true)
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
        location = gps.location
    }

    override fun onPause() {
        super.onPause()
        timer.stop()
    }

    fun update() {
        val start = startTime ?: return
        val height = service.getCliffHeight(start, Instant.now(), location)
        val converted = height.convertTo(units)
        val formatted = formatService.formatDistance(converted, 2)
        binding.cliffHeightTitle.title.text = formatted
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolCliffHeightBinding {
        return FragmentToolCliffHeightBinding.inflate(layoutInflater, container, false)
    }
}