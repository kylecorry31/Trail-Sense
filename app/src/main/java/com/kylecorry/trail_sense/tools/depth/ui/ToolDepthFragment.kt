package com.kylecorry.trail_sense.tools.depth.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolDepthBinding
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.domain.depth.DepthService
import com.kylecorry.trailsensecore.domain.units.*
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.time.Throttle
import com.kylecorry.trailsensecore.infrastructure.view.BoundFragment
import kotlin.math.max

class ToolDepthFragment : BoundFragment<FragmentToolDepthBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val barometer by lazy { sensorService.getBarometer() }
    private val depthService = DepthService()
    private val formatService by lazy { FormatServiceV2(requireContext()) }
    private val userPrefs by lazy { UserPreferences(requireContext()) }
    private val cache by lazy { Cache(requireContext()) }
    private val throttle = Throttle(20)

    private var lastDepth: Float = 0f
    private var maxDepth: Float = 0f

    private var seaLevelPressure: Float = 0f

    private lateinit var units: DistanceUnits

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CustomUiUtils.disclaimer(
            requireContext(),
            getString(R.string.disclaimer_message_title),
            getString(R.string.depth_disclaimer),
            "cache_dialog_tool_depth"
        )
    }

    override fun onResume() {
        super.onResume()
        binding.saltwaterSwitch.isChecked = cache.getBoolean(CACHE_SALTWATER) ?: true

        binding.saltwaterSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            cache.putBoolean(CACHE_SALTWATER, isChecked)
            update()
        }

        units = userPrefs.baseDistanceUnits
        seaLevelPressure = 0f
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

        if (!isSeaLevelSet()) {
            seaLevelPressure = barometer.pressure
        }

        // If it is still not set, wait for the next barometer update
        if (!isSeaLevelSet()) {
            return true
        }

        val depth = depthService.calculateDepth(
            Pressure(barometer.pressure, PressureUnits.Hpa),
            Pressure(seaLevelPressure, PressureUnits.Hpa),
            binding.saltwaterSwitch.isChecked
        ).distance.roundPlaces(1)

        binding.saltwaterSwitch.isEnabled = depth == 0f

        if (lastDepth == 0f && depth > 0) {
            maxDepth = depth
        }

        lastDepth = depth
        maxDepth = max(depth, maxDepth)

        val converted = Distance.meters(depth).convertTo(units)
        val formatted = formatService.formatDistance(converted, 1)

        val convertedMax = Distance.meters(maxDepth).convertTo(units)
        val formattedMax = formatService.formatDistance(convertedMax, 1)

        binding.depth.text = formatted
        binding.maxDepth.text = getString(R.string.max_depth, formattedMax)

        return true
    }

    private fun isSeaLevelSet(): Boolean {
        return seaLevelPressure != 0f
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