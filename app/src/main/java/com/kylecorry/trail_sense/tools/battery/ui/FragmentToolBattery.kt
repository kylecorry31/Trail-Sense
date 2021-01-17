package com.kylecorry.trail_sense.tools.battery.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolBatteryBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.weather.domain.LowPassFilter
import com.kylecorry.trailsensecore.infrastructure.sensors.battery.Battery
import com.kylecorry.trailsensecore.infrastructure.sensors.battery.BatteryChargingMethod
import com.kylecorry.trailsensecore.infrastructure.sensors.battery.BatteryChargingStatus
import com.kylecorry.trailsensecore.infrastructure.sensors.battery.BatteryHealth
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class FragmentToolBattery : Fragment() {

    private var _binding: FragmentToolBatteryBinding? = null
    private val binding get() = _binding!!

    private val formatService by lazy { FormatService(requireContext()) }
    private val battery by lazy { Battery(requireContext()) }

    private val intervalometer = Intervalometer {
        update()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
        binding.batteryChargeIndicator.visibility = View.INVISIBLE
        binding.batteryCurrent.text = ""
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
        // If charging, show up arrow
        val chargingStatus = battery.chargingStatus
        val isCharging = chargingStatus == BatteryChargingStatus.Charging
        when (chargingStatus) {
            BatteryChargingStatus.Charging -> {
                binding.batteryChargeIndicator.rotation = 0f
                binding.batteryChargeIndicator.visibility = View.VISIBLE
            }
            BatteryChargingStatus.Discharging -> {
                binding.batteryChargeIndicator.rotation = 180f
                binding.batteryChargeIndicator.visibility = View.VISIBLE
            }
            else -> binding.batteryChargeIndicator.visibility = View.INVISIBLE
        }

        // If charging and current is negative, invert current
        val current = battery.current.absoluteValue * if (isCharging) 1 else -1
        val capacity = battery.capacity
        val pct = battery.percent.roundToInt()

        binding.batteryPercentage.text = formatService.formatPercentage(pct)
        binding.batteryCapacity.text = formatService.formatBatteryCapacity(capacity)
        binding.batteryCapacity.visibility = if (capacity == 0f) View.GONE else View.VISIBLE
        binding.batteryHealth.text =
            getString(R.string.battery_health, getHealthString(battery.health))
        binding.batteryLevelBar.progress = pct

        if (current != 0f) {
            val formattedCurrent = formatService.formatCurrent(current.absoluteValue)
            binding.batteryCurrent.text = when {
                current > 500 -> getString(R.string.charging_fast, formattedCurrent)
                current > 0 -> getString(R.string.charging_slow, formattedCurrent)
                current < -500 -> getString(R.string.discharging_fast, formattedCurrent)
                else -> getString(R.string.discharging_slow, formattedCurrent)
            }
        } else {
            val chargeMethod = battery.chargingMethod
            binding.batteryCurrent.text = when {
                isCharging && chargeMethod == BatteryChargingMethod.AC -> getString(
                    R.string.charging_fast,
                    getString(R.string.battery_power_ac)
                )
                isCharging && chargeMethod == BatteryChargingMethod.USB -> getString(
                    R.string.charging_slow,
                    getString(R.string.battery_power_usb)
                )
                isCharging && chargeMethod == BatteryChargingMethod.Wireless -> getString(
                    R.string.charging_wireless,
                    getString(R.string.battery_power_wireless)
                )
                else -> ""
            }
        }
    }


    private fun getHealthString(health: BatteryHealth): String {
        return when (health) {
            BatteryHealth.Cold -> getString(R.string.battery_health_cold)
            BatteryHealth.Dead -> getString(R.string.battery_health_dead)
            BatteryHealth.Good -> getString(R.string.battery_health_good)
            BatteryHealth.Overheat -> getString(R.string.battery_health_overheat)
            BatteryHealth.OverVoltage -> getString(R.string.battery_health_over_voltage)
            BatteryHealth.Unknown -> getString(R.string.battery_health_unknown)
        }
    }

}