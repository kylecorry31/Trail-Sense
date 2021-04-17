package com.kylecorry.trail_sense.tools.depth.ui

import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolDepthBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.domain.depth.DepthService
import com.kylecorry.trailsensecore.domain.units.DistanceUnits
import com.kylecorry.trailsensecore.domain.units.Pressure
import com.kylecorry.trailsensecore.domain.units.PressureUnits
import com.kylecorry.trailsensecore.domain.units.UnitService
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.time.Throttle
import com.kylecorry.trailsensecore.infrastructure.view.BoundFragment
import kotlin.math.max

class ToolDepthFragment : BoundFragment<FragmentToolDepthBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val barometer by lazy { sensorService.getBarometer() }
    private val depthService = DepthService()
    private val unitService = UnitService()
    private val formatService by lazy { FormatService(requireContext()) }
    private val userPrefs by lazy { UserPreferences(requireContext()) }
    private val cache by lazy { Cache(requireContext()) }
    private val throttle = Throttle(20)

    private var lastDepth: Float = 0f
    private var maxDepth: Float = 0f

    private lateinit var units: DistanceUnits

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (cache.getBoolean("cache_dialog_tool_depth") != true) {
            UiUtils.alert(
                requireContext(),
                getString(R.string.disclaimer_message_title),
                getString(R.string.depth_disclaimer)
            ){
                cache.putBoolean("cache_dialog_tool_depth", true)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.saltwaterSwitch.isChecked = cache.getBoolean(CACHE_SALTWATER) ?: true

        binding.saltwaterSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            cache.putBoolean(CACHE_SALTWATER, isChecked)
            update()
        }

        units = if (userPrefs.distanceUnits == UserPreferences.DistanceUnits.Meters) {
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
        if (throttle.isThrottled()) {
            return true
        }

        val depth = depthService.calculateDepth(
            Pressure(barometer.pressure, PressureUnits.Hpa),
            Pressure(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, PressureUnits.Hpa),
            binding.saltwaterSwitch.isChecked
        ).distance

        if (lastDepth == 0f && depth > 0) {
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

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolDepthBinding {
        return FragmentToolDepthBinding.inflate(layoutInflater, container, false)
    }

    companion object {
        private const val CACHE_SALTWATER = "cache_saltwater_switch"
    }

}