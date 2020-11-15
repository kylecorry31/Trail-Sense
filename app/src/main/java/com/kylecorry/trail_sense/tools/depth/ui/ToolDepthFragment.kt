package com.kylecorry.trail_sense.tools.depth.ui

import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolDepthBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.domain.depth.DepthService
import com.kylecorry.trailsensecore.domain.units.DistanceUnits
import com.kylecorry.trailsensecore.domain.units.UnitService
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.time.Throttle
import kotlin.math.max

class ToolDepthFragment: Fragment() {

    private var _binding: FragmentToolDepthBinding? = null
    private val binding get() = _binding!!

    private val sensorService by lazy { SensorService(requireContext()) }
    private val barometer by lazy { sensorService.getBarometer() }
    private val depthService = DepthService()
    private val unitService = UnitService()
    private val formatService by lazy { FormatService(requireContext()) }
    private val userPrefs by lazy { UserPreferences(requireContext()) }
    private val throttle = Throttle(20)

    private var lastDepth: Float = 0f
    private var maxDepth: Float = 0f

    private lateinit var units: DistanceUnits

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentToolDepthBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UiUtils.alert(requireContext(), getString(R.string.disclaimer_message_title), getString(R.string.depth_disclaimer))
    }

    override fun onResume() {
        super.onResume()
        units = if (userPrefs.distanceUnits == UserPreferences.DistanceUnits.Meters){
            DistanceUnits.Meters
        } else {
            DistanceUnits.Feet
        }
        barometer.start(this::update)
    }

    override fun onPause() {
        super.onPause()
        barometer.stop(this::update)
    }

    fun update(): Boolean {
        if (throttle.isThrottled()){
            return true
        }

        val depth = depthService.calculateDepth(barometer.pressure, SensorManager.PRESSURE_STANDARD_ATMOSPHERE)

        if (lastDepth == 0f && depth > 0){
            maxDepth = depth
        }

        lastDepth = depth
        maxDepth = max(depth, maxDepth)

        val converted = unitService.convert(depth, DistanceUnits.Meters, units)
        val formatted = formatService.formatDepth(converted, units)

        val convertedMax = unitService.convert(maxDepth, DistanceUnits.Meters, units)
        val formattedMax = formatService.formatDepth(convertedMax, units)

        binding.depth.text = formatted
        binding.maxDepth.text = getString(R.string.max_depth, formattedMax)

        return true
    }

}