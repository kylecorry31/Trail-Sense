package com.kylecorry.trail_sense.tools.battery.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.kylecorry.andromeda.battery.Battery
import com.kylecorry.andromeda.battery.BatteryChargingMethod
import com.kylecorry.andromeda.battery.BatteryChargingStatus
import com.kylecorry.andromeda.battery.BatteryHealth
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.sol.math.filters.IFilter
import com.kylecorry.sol.math.filters.MedianFilter
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolBatteryBinding
import com.kylecorry.trail_sense.databinding.ListItemServiceBinding
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.extensions.onDefault
import com.kylecorry.trail_sense.tools.battery.domain.BatteryReading
import com.kylecorry.trail_sense.tools.battery.domain.RunningService
import com.kylecorry.trail_sense.tools.battery.infrastructure.BatteryService
import com.kylecorry.trail_sense.tools.battery.infrastructure.persistence.BatteryRepo
import java.time.Duration
import java.time.Instant
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class FragmentToolBattery : BoundFragment<FragmentToolBatteryBinding>() {

    private val formatService by lazy { FormatService(requireContext()) }
    private val battery by lazy { Battery(requireContext()) }
    private val batteryRepo by lazy { BatteryRepo.getInstance(requireContext()) }

    private val lowPowerMode by lazy { LowPowerMode(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val batteryService = BatteryService()
    private lateinit var servicesList: ListView<RunningService>

    private val currentFilterSize = 100
    private var currentFilter: IFilter = MedianFilter(currentFilterSize)
    private var lastChargingStatus = BatteryChargingStatus.Unknown
    private var current = 0f
    private var pct = 0
    private var capacity = 0f

    private var readings = listOf<BatteryReading>()

    private val intervalometer = Timer {
        update()
    }

    private val serviceIntervalometer = Timer {
        updateServices()
    }

    private val batteryUpdateTimer = Timer(lifecycleScope) {
        onDefault {
            // If charging, show up arrow
            val chargingStatus = battery.chargingStatus
            val isCharging = chargingStatus == BatteryChargingStatus.Charging

            if (chargingStatus != lastChargingStatus) {
                resetCurrentFilter()
            }

            lastChargingStatus = chargingStatus

            // If charging and current is negative, invert current
            current =
                currentFilter.filter(battery.current.absoluteValue * if (isCharging) 1 else -1)
            capacity = battery.capacity
            pct = battery.percent.roundToInt()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.batteryLevelProgress.horizontal = false
        servicesList =
            ListView(binding.runningServices, R.layout.list_item_service) { serviceView, service ->
                val serviceBinding = ListItemServiceBinding.bind(serviceView)
                serviceBinding.title.text = service.name
                val frequency = if (service.frequency == Duration.ZERO) {
                    getString(R.string.always_on)
                } else {
                    getString(
                        R.string.service_update_frequency,
                        formatService.formatDuration(service.frequency)
                    )
                }
                serviceBinding.description.text =
                    getString(R.string.dash_separated_pair, frequency, getBatteryUsage(service))
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

        CustomUiUtils.setButtonState(binding.batteryTitle.leftButton, false)
        CustomUiUtils.setButtonState(binding.batteryTitle.rightButton, false)

        val settingsIntent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
        binding.batteryTitle.rightButton.isVisible =
            Intents.hasReceiver(requireContext(), settingsIntent)
        binding.batteryTitle.rightButton.setOnClickListener {
            startActivity(settingsIntent)
        }

        binding.batteryTitle.leftButton.setOnClickListener {
            val readingDuration =
                Duration.between(readings.firstOrNull()?.time, readings.lastOrNull()?.time)
            CustomUiUtils.showChart(
                this,
                getString(
                    R.string.battery_history,
                    formatService.formatDuration(readingDuration, false)
                )
            ) {
                val chart = BatteryChart(it)
                chart.plot(readings, false)
            }
        }

        observe(batteryRepo.get()) {
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

            binding.batteryTitle.leftButton.isVisible = readings.size >= 2

            update()
        }

        observe(battery) { }
    }

    private fun resetCurrentFilter() {
        currentFilter = MedianFilter(currentFilterSize)
    }

    override fun onResume() {
        super.onResume()
        resetCurrentFilter()
        intervalometer.interval(INTERVAL_1_FPS)
        batteryUpdateTimer.interval(20)
        serviceIntervalometer.interval(1000)
        binding.batteryCurrent.text = ""
    }

    override fun onPause() {
        super.onPause()
        serviceIntervalometer.stop()
        batteryUpdateTimer.stop()
        intervalometer.stop()
    }

    private fun getBatteryUsage(service: RunningService): String {
        val usage = when {
            service.frequency < Duration.ofMinutes(15) -> {
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

        val time = if (isCharging) getTimeUntilFull() else getTimeUntilEmpty()

        binding.batteryPercentage.text = formatService.formatPercentage(pct.toFloat())
        binding.batteryCapacity.text = formatService.formatElectricalCapacity(capacity)
        binding.batteryCapacity.isVisible = capacity != 0f
        binding.batteryTitle.title.isVisible = time != null

        if (time != null) {
            binding.batteryTitle.title.text = formatService.formatDuration(time)
        }

        if (time != null && !isCharging) {
            binding.batteryTitle.subtitle.text = getString(R.string.time_until_empty)
        } else if (time != null && isCharging) {
            binding.batteryTitle.subtitle.text = getString(R.string.time_until_full)
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

    private fun updateServices() {
        val services = batteryService.getRunningServices(requireContext())
        servicesList.setData(services)
    }

    private fun getTimeUntilEmpty(): Duration? {
        return batteryService.getTimeUntilEmpty(battery, readings)
    }

    private fun getTimeUntilFull(): Duration? {
        return batteryService.getTimeUntilFull(battery, readings)
    }


    private fun getHealthString(health: BatteryHealth): String {
        return formatService.formatBatteryHealth(health)
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolBatteryBinding {
        return FragmentToolBatteryBinding.inflate(layoutInflater, container, false)
    }

}