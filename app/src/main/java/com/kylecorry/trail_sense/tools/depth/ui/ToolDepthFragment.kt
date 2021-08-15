package com.kylecorry.trail_sense.tools.depth.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.math.roundPlaces
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.andromeda.core.units.Distance
import com.kylecorry.andromeda.core.units.DistanceUnits
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolDepthBinding
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.domain.depth.DepthService
import com.kylecorry.trailsensecore.domain.units.*
import java.time.Duration
import java.time.Instant
import kotlin.math.max

class ToolDepthFragment : BoundFragment<FragmentToolDepthBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val barometer by lazy { sensorService.getBarometer() }
    private val depthService = DepthService()
    private val formatService by lazy { FormatServiceV2(requireContext()) }
    private val userPrefs by lazy { UserPreferences(requireContext()) }
    private val cache by lazy { Preferences(requireContext()) }
    private val throttle = Throttle(20)

    private var lastDepth: Float = 0f
    private var maxDepth: Float = 0f
    private var underwaterOverride = false

    private var seaLevelPressure: Float = 0f

    private var lastBackPress = Instant.MIN

    private lateinit var units: DistanceUnits

    private var backListener: OnBackPressedCallback? = null

    private var backConfirmDuration = Duration.ofSeconds(3)

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

        underwaterOverride = false

        binding.underwaterModeBtn.setOnClickListener {
            underwaterOverride = true
        }

        backListener?.remove()
        backListener = onBackPress {
            val isUnderwater = lastDepth != 0f || underwaterOverride
            val now = Instant.now()
            if (isUnderwater && Duration.between(lastBackPress, now).abs() >= backConfirmDuration) {
                Alerts.toast(requireContext(), getString(R.string.back_press_confirm))
            } else if (isUnderwater) {
                return@onBackPress true
            }
            lastBackPress = now
            !isUnderwater
        }
    }

    override fun onPause() {
        super.onPause()
        barometer.stop(this::update)
        requireBottomNavigation().isVisible = true
        backListener?.remove()
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

        val isUnderwater = depth != 0f || underwaterOverride

        binding.saltwaterSwitch.isEnabled = !isUnderwater
        requireBottomNavigation().isVisible = !isUnderwater
        binding.underwaterMode.isInvisible = !isUnderwater
        binding.underwaterModeBtn.isInvisible = isUnderwater

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

    private fun onBackPress(listener: () -> Boolean): OnBackPressedCallback {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (listener()) {
                    remove()
                    findNavController().popBackStack()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        return callback
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