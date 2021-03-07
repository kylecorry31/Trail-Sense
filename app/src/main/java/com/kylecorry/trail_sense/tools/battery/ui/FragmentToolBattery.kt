package com.kylecorry.trail_sense.tools.battery.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolBatteryBinding
import com.kylecorry.trail_sense.databinding.ListItemInventoryBinding
import com.kylecorry.trail_sense.databinding.ListItemPlainBinding
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.LowPowerMode
import com.kylecorry.trail_sense.tools.battery.domain.RunningService
import com.kylecorry.trail_sense.tools.battery.infrastructure.BatteryService
import com.kylecorry.trailsensecore.infrastructure.sensors.battery.Battery
import com.kylecorry.trailsensecore.infrastructure.sensors.battery.BatteryChargingMethod
import com.kylecorry.trailsensecore.infrastructure.sensors.battery.BatteryChargingStatus
import com.kylecorry.trailsensecore.infrastructure.sensors.battery.BatteryHealth
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import com.kylecorry.trailsensecore.infrastructure.view.ListView
import java.time.Duration
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class FragmentToolBattery : Fragment() {

    private var _binding: FragmentToolBatteryBinding? = null
    private val binding get() = _binding!!

    private val formatService by lazy { FormatServiceV2(requireContext()) }
    private val battery by lazy { Battery(requireContext()) }

    private val lowPowerMode by lazy { LowPowerMode(requireContext()) }
    private val batteryService = BatteryService()
    private lateinit var servicesList: ListView<RunningService>

    private val intervalometer = Intervalometer {
        update()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToolBatteryBinding.inflate(inflater, container, false)
        servicesList = ListView(binding.runningServices, R.layout.list_item_plain){ serviceView, service ->
            val serviceBinding = ListItemPlainBinding.bind(serviceView)
            serviceBinding.title.text = service.name
            serviceBinding.description.text = if (service.frequency == Duration.ZERO){
                getString(R.string.always_on)
            } else {
                getString(R.string.service_update_frequency, formatService.formatDuration(service.frequency))
            } + " - " + getBatteryUsage(service)
        }
        servicesList.addLineSeparator()
        binding.lowPowerModeSwitch.isChecked = lowPowerMode.isEnabled()
        binding.lowPowerModeSwitch.setOnClickListener {
            if (lowPowerMode.isEnabled()) {
                lowPowerMode.disable(requireActivity())
            } else {
                lowPowerMode.enable(requireActivity())
            }
        }
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

    private fun getBatteryUsage(service: RunningService): String {
        val usage = when {
            service.frequency <= Duration.ofMinutes(10) -> {
                getString(R.string.high)
            }
            service.frequency <= Duration.ofMinutes(25) -> {
                getString(R.string.moderate)
            }
            else -> {
                getString(R.string.low)
            }
        }
        return getString(R.string.battery_usage, usage)
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

        binding.batteryPercentage.text = formatService.formatPercentage(pct.toFloat())
        binding.batteryCapacity.text = formatService.formatElectricalCapacity(capacity)
        binding.batteryCapacity.visibility = if (capacity == 0f) View.GONE else View.VISIBLE
        binding.batteryHealth.text =
            getString(R.string.battery_health, getHealthString(battery.health))
        binding.batteryLevelBar.progress = pct

        if (current.absoluteValue >= 0.5f) {
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

        val services = batteryService.getRunningServices(requireContext())
        servicesList.setData(services)
    }


    private fun getHealthString(health: BatteryHealth): String {
        return when (health) {
            BatteryHealth.Cold -> getString(R.string.battery_health_cold)
            BatteryHealth.Dead -> getString(R.string.battery_health_dead)
            BatteryHealth.Good -> getString(R.string.battery_health_good)
            BatteryHealth.Overheat -> getString(R.string.battery_health_overheat)
            BatteryHealth.OverVoltage -> getString(R.string.battery_health_over_voltage)
            BatteryHealth.Unknown -> getString(R.string.unknown)
        }
    }

}