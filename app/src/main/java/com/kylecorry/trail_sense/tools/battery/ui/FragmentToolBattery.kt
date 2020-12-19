package com.kylecorry.trail_sense.tools.battery.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.databinding.FragmentToolBatteryBinding
import com.kylecorry.trail_sense.shared.DecimalFormatter
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.sensors.battery.Battery
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import kotlin.math.roundToInt

class FragmentToolBattery: Fragment() {

    private var _binding: FragmentToolBatteryBinding? = null
    private val binding get() = _binding!!

    private val formatService by lazy { FormatService(requireContext()) }
    private val battery by lazy { Battery(requireContext()) }

    private var lastReading = 0f

    private val intervalometer = Intervalometer {
        update()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentToolBatteryBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        battery.start(this::onBatteryUpdate)
        intervalometer.interval(20)
    }

    override fun onPause() {
        super.onPause()
        battery.stop(this::onBatteryUpdate)
        intervalometer.stop()
    }

    private fun onBatteryUpdate(): Boolean {
        return true
    }

    private fun update() {
        val capacity = battery.capacity
        val pct = battery.percent.roundToInt()
        binding.batteryPercentage.text = formatService.formatPercentage(pct)
        binding.batteryCapacity.text = formatService.formatBatteryCapacity(capacity)
        binding.batteryLevelBar.progress = pct

        if (capacity - lastReading > 0){
            // Increasing
            binding.batteryChargeIndicator.rotation = 0f
            binding.batteryChargeIndicator.visibility = View.VISIBLE
        } else if (capacity - lastReading < 0){
            // Decreasing
            binding.batteryChargeIndicator.rotation = 180f
            binding.batteryChargeIndicator.visibility = View.VISIBLE
        }

        lastReading = capacity
    }

}