package com.kylecorry.trail_sense.tools.cliffheight.ui

import android.os.Bundle
import android.view.LayoutInflater
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
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.cliffheight.domain.CliffHeightService
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.startBtn.setOnClickListener {
            if (running) {
                timer.stop()
                running = false
            } else {
                startTime = Instant.now()
                timer.interval(16)
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
        binding.height.text = formatted
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolCliffHeightBinding {
        return FragmentToolCliffHeightBinding.inflate(layoutInflater, container, false)
    }

}