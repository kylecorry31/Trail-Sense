package com.kylecorry.trail_sense.tools.battery.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.battery.Battery
import com.kylecorry.andromeda.battery.BatteryChargingMethod
import com.kylecorry.andromeda.battery.BatteryChargingStatus
import com.kylecorry.andromeda.battery.BatteryHealth
import com.kylecorry.andromeda.core.sensors.asLiveData
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolBatteryBinding
import com.kylecorry.trail_sense.databinding.ListItemServiceBinding
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.tools.battery.domain.RunningService
import com.kylecorry.trail_sense.tools.battery.infrastructure.BatteryService
import com.kylecorry.trail_sense.tools.battery.infrastructure.persistence.BatteryRepo
import com.kylecorry.trailsensecore.domain.power.BatteryReading
import com.kylecorry.trailsensecore.domain.power.PowerService
import java.time.Duration
import java.time.Instant
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class FragmentToolBattery : BoundFragment<FragmentToolBatteryBinding>() {

    private val formatService by lazy { FormatServiceV2(requireContext()) }
    private val battery by lazy { Battery(requireContext()) }
    private val batteryRepo by lazy { BatteryRepo.getInstance(requireContext()) }

    private val lowPowerMode by lazy { LowPowerMode(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val batteryService = BatteryService()
    private lateinit var servicesList: ListView<RunningService>

    private var readings = listOf<BatteryReading>()

    private val powerService = PowerService()

    private val intervalometer = Timer {
        update()
    }

    private val serviceIntervalometer = Timer {
        updateServices()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.batteryLevelProgress.horizontal = false
        servicesList =
            ListView(binding.runningServices, R.layout.list_item_service) { serviceView, service ->
                val serviceBinding = ListItemServiceBinding.bind(serviceView)
                serviceBinding.title.text = service.name
                serviceBinding.description.text = if (service.frequency == Duration.ZERO) {
                    getString(R.string.always_on)
                } else {
                    getString(
                        R.string.service_update_frequency,
                        formatService.formatDuration(service.frequency)
                    )
                } + " - " + getBatteryUsage(service)
                serviceBinding.disableBtn.setOnClickListener {
                    service.disable()
                    updateServices()
                }
            }
        servicesList.addLineSeparator()
        binding.lowPowerModeSwitch.isChecked = lowPowerMode.isEnabled()
        binding.lowPowerModeSwitch.setOnClickListener {
            if (lowPowerMode.isEnabled()) {
                prefs.power.userEnabledLowPower = false
                lowPowerMode.disable(requireActivity())
            } else {
                prefs.power.userEnabledLowPower = true
                lowPowerMode.enable(requireActivity())
            }
            updateServices()
        }

        CustomUiUtils.setButtonState(binding.batteryPhoneBatterySettings, false)
        CustomUiUtils.setButtonState(binding.batteryHistoryBtn, false)

        binding.batteryPhoneBatterySettings.setOnClickListener {
            val intentBatteryPhoneSettings = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
            startActivity(intentBatteryPhoneSettings)
        }

        binding.batteryHistoryBtn.setOnClickListener {
            val readingDuration =
                Duration.between(readings.firstOrNull()?.time, readings.lastOrNull()?.time)
            val view = View.inflate(context, R.layout.view_chart_prompt, null)
            val chart = BatteryChart(view.findViewById(R.id.chart))
            chart.plot(readings, false)
            Alerts.dialog(
                requireContext(),
                getString(
                    R.string.battery_history,
                    formatService.formatDuration(readingDuration, false)
                ),
                contentView = view,
                cancelText = null
            )
        }

        batteryRepo.get().observe(viewLifecycleOwner, {
            readings = it.sortedBy { it.time }.map { it.toBatteryReading() } + listOfNotNull(
                if (battery.hasValidReading)
                    BatteryReading(
                        Instant.now(),
                        battery.percent,
                        battery.capacity,
                        battery.chargingStatus == BatteryChargingStatus.Charging
                    )
                else null
            )

            binding.batteryHistoryBtn.isVisible = readings.size >= 2

            update()
        })

        battery.asLiveData().observe(viewLifecycleOwner, {})
    }

    override fun onResume() {
        super.onResume()
        intervalometer.interval(20)
        serviceIntervalometer.interval(1000)
        binding.batteryCurrent.text = ""
    }

    override fun onPause() {
        super.onPause()
        serviceIntervalometer.stop()
        intervalometer.stop()
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

        // If charging and current is negative, invert current
        val current = battery.current.absoluteValue * if (isCharging) 1 else -1
        val capacity = battery.capacity
        val pct = battery.percent.roundToInt()
        val time = if (isCharging) getTimeUntilFull() else getTimeUntilEmpty()

        binding.batteryPercentage.text = formatService.formatPercentage(pct.toFloat())
        binding.batteryCapacity.text = formatService.formatElectricalCapacity(capacity)
        binding.batteryCapacity.isVisible = capacity != 0f
        binding.batteryTime.isVisible = time != null

        if (time != null) {
            binding.batteryTime.text = formatService.formatDuration(time)
        }

        if (time != null && !isCharging) {
            binding.batteryTimeLbl.text = getString(R.string.time_until_empty)
        } else if (time != null && isCharging) {
            binding.batteryTimeLbl.text = getString(R.string.time_until_full)
        }
        binding.batteryHealth.isVisible = battery.health != BatteryHealth.Good
        binding.batteryHealth.text =
            getString(R.string.battery_health, getHealthString(battery.health))
        binding.batteryLevelProgress.progress = pct / 100f

        binding.batteryPercentage.setShadowLayer(
            6f,
            0f,
            0f,
            Resources.getAndroidColorAttr(requireContext(), android.R.attr.textColorPrimaryInverse)
        )
        binding.batteryCapacity.setShadowLayer(
            6f,
            0f,
            0f,
            Resources.getAndroidColorAttr(requireContext(), android.R.attr.textColorPrimaryInverse)
        )

        binding.batteryLevelProgress.progressColor = when {
            pct >= 75 -> AppColor.Green.color
            pct >= 50 -> AppColor.Yellow.color
            pct >= 25 -> AppColor.Orange.color
            else -> AppColor.Red.color
        }


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
    }

    private fun updateServices(){
        val services = batteryService.getRunningServices(requireContext())
        servicesList.setData(services)
    }

    private fun getTimeUntilEmpty(): Duration? {
        val hasCapacity = false // battery.capacity != 0f
        val capacity = if (hasCapacity) battery.capacity else battery.percent
        val rates = powerService.getRates(readings, Duration.ofMinutes(5), hasCapacity)
        val lastDischargeRate = rates.lastOrNull { it < 0f } ?: return null
        return powerService.getTimeUntilEmpty(capacity, lastDischargeRate)
    }

    private fun getTimeUntilFull(): Duration? {
        val hasCapacity = false // battery.capacity != 0f
        val capacity = if (hasCapacity) battery.capacity else battery.percent
        val rates = powerService.getRates(readings, Duration.ofMinutes(5), hasCapacity)
        val lastChargeRate = rates.lastOrNull { it > 0f } ?: return null
        val maxCapacity = if (hasCapacity) getMaxCapacity() else 100f
        return powerService.getTimeUntilFull(capacity, maxCapacity, lastChargeRate)
    }

    private fun getMaxCapacity(): Float {
        return if (battery.percent != 0f) {
            battery.maxCapacity
        } else {
            100f
        }
//        return powerService.getMaxCapacity(readings) ?: (battery.capacity / battery.percent) * 100f
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

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolBatteryBinding {
        return FragmentToolBatteryBinding.inflate(layoutInflater, container, false)
    }

}