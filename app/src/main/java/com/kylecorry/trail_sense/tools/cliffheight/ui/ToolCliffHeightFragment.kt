package com.kylecorry.trail_sense.tools.cliffheight.ui

import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolCliffHeightBinding
import com.kylecorry.trail_sense.databinding.FragmentToolDepthBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.domain.depth.DepthService
import com.kylecorry.trailsensecore.domain.physics.PhysicsService
import com.kylecorry.trailsensecore.domain.units.DistanceUnits
import com.kylecorry.trailsensecore.domain.units.UnitService
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import com.kylecorry.trailsensecore.infrastructure.time.Throttle
import java.time.Duration
import java.time.Instant

class ToolCliffHeightFragment: Fragment() {

    private var _binding: FragmentToolCliffHeightBinding? = null
    private val binding get() = _binding!!

    private val physicsService = PhysicsService()
    private val intervalometer = Intervalometer {
        update()
    }
    private val unitService = UnitService()
    private val formatService by lazy { FormatService(requireContext()) }
    private val userPrefs by lazy { UserPreferences(requireContext()) }

    private lateinit var units: DistanceUnits
    private var startTime: Instant? = null
    private var running = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentToolCliffHeightBinding.inflate(inflater, container, false)
        binding.startBtn.setOnClickListener {
            if (running){
                binding.startBtn.text = getString(R.string.cliff_height_start_measure)
                intervalometer.stop()
                running = false
            } else {
                binding.startBtn.text = getString(R.string.cliff_height_stop_measure)
                startTime = Instant.now()
                intervalometer.interval(16)
                running = true
            }
        }
        return binding.root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UiUtils.alert(requireContext(), getString(R.string.disclaimer_message_title), getString(R.string.tool_cliff_height_disclaimer))
    }

    override fun onResume() {
        super.onResume()
        units = if (userPrefs.distanceUnits == UserPreferences.DistanceUnits.Meters){
            DistanceUnits.Meters
        } else {
            DistanceUnits.Feet
        }
    }

    override fun onPause() {
        super.onPause()
        intervalometer.stop()
    }

    fun update() {
        if (startTime == null){
            return
        }

        val duration = Duration.between(startTime, Instant.now())
        val height = physicsService.fallHeight(duration)
        val converted = unitService.convert(height, DistanceUnits.Meters, units)
        val formatted = formatService.formatDepth(converted, units)

        binding.height.text = formatted
    }

}