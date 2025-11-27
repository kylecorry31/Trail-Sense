package com.kylecorry.trail_sense.tools.battery.ui

import android.content.Intent
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.battery.Battery
import com.kylecorry.andromeda.battery.BatteryChargingMethod
import com.kylecorry.andromeda.battery.BatteryChargingStatus
import com.kylecorry.andromeda.battery.BatteryHealth
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.ui.useCallback
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.useTopic
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.sol.math.filters.MedianFilter
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useLiveData
import com.kylecorry.trail_sense.shared.extensions.useNavController
import com.kylecorry.trail_sense.shared.extensions.useTimer
import com.kylecorry.trail_sense.shared.extensions.useTrigger
import com.kylecorry.trail_sense.shared.views.ProgressBar
import com.kylecorry.trail_sense.tools.battery.domain.BatteryReading
import com.kylecorry.trail_sense.tools.battery.domain.RunningService
import com.kylecorry.trail_sense.tools.battery.domain.SystemBatteryTip
import com.kylecorry.trail_sense.tools.battery.infrastructure.BatteryService
import com.kylecorry.trail_sense.tools.battery.infrastructure.LowPowerMode
import com.kylecorry.trail_sense.tools.battery.infrastructure.persistence.BatteryRepo
import java.time.Duration
import java.time.Instant
import kotlin.math.absoluteValue

class FragmentToolBattery : TrailSenseReactiveFragment(R.layout.fragment_tool_battery) {
    private val currentFilterSize = 100

    override fun update() {
        // Views
        val percentageTextView = useView<TextView>(R.id.battery_percentage)
        val capacityTextView = useView<TextView>(R.id.battery_capacity)
        val healthTextView = useView<TextView>(R.id.battery_health)
        val currentTextView = useView<TextView>(R.id.battery_current)
        val titleView = useView<Toolbar>(R.id.battery_title)
        val lowPowerSwitchView = useView<SwitchCompat>(R.id.low_power_mode_switch)
        val progressView = useView<ProgressBar>(R.id.battery_level_progress)
        val servicesListView = useView<AndromedaListView>(R.id.running_services)

        // Services
        val context = useAndroidContext()
        val navController = useNavController()
        val prefs = useService<UserPreferences>()
        val formatter = useService<FormatService>()
        val batteryService = useMemo { BatteryService() }
        val battery = useMemo(context) { Battery(context) }
        val batteryRepo = useMemo(context) { BatteryRepo.getInstance(context) }
        val lowPowerMode = useMemo(context) { LowPowerMode(context) }

        // State
        val (batteryKey, triggerBatteryUpdate) = useTrigger()
        val (percent, setPercent) = useState(0f)
        val (capacity, setCapacity) = useState(0f)
        val (health, setHealth) = useState(BatteryHealth.Unknown)
        val (current, setCurrent) = useState(0f)
        val (chargingStatus, setChargingStatus) = useState(BatteryChargingStatus.Unknown)
        val (chargeMethod, setChargeMethod) = useState(BatteryChargingMethod.Unknown)
        val currentFilter = useMemo(resetOnResume, chargingStatus) {
            MedianFilter(currentFilterSize)
        }
        val isCharging = chargingStatus == BatteryChargingStatus.Charging
        val rawReadings = useLiveData(batteryRepo.get(), emptyList()) { allReadings ->
            allReadings.sortedBy { it.time }
        }
        val readings = useMemo(rawReadings, percent, capacity, isCharging) {
            rawReadings + listOfNotNull(
                if (battery.hasValidReading)
                    BatteryReading(
                        Instant.now(),
                        percent,
                        capacity,
                        isCharging
                    )
                else null
            )
        }
        val (services, triggerServicesUpdate) = useRunningServices(batteryService)
        val tips = useSystemBatteryTips(batteryService)
        val time = useMemo(isCharging, readings, current, percent) {
            if (isCharging) {
                batteryService.getTimeUntilFull(battery, readings)
            } else {
                batteryService.getTimeUntilEmpty(battery, readings)
            }
        }

        // Battery
        // TODO: Extract battery reading to a custom hook
        useEffect(battery, batteryKey) {
            setChargingStatus(battery.chargingStatus)
            val isCharging = battery.chargingStatus == BatteryChargingStatus.Charging

            // If charging and current is negative, invert current
            setCurrent(currentFilter.filter(battery.current.absoluteValue * if (isCharging) 1 else -1))
            setCapacity(battery.capacity)
            setPercent(battery.percent)
            setHealth(battery.health)
            setChargeMethod(battery.chargingMethod)
        }

        useTopic(battery) {
            triggerBatteryUpdate()
        }

        useTimer(INTERVAL_30_FPS) {
            triggerBatteryUpdate()
        }


        // View - Services List
        val onServiceDisable = useCallback<RunningService, Unit> { service: RunningService ->
            runInBackground {
                service.disable()
                triggerServicesUpdate()
            }
        }

        useEffect(servicesListView, context, services, onServiceDisable, tips) {
            val serviceMapper = RunningServiceListItemMapper(context, onServiceDisable)
            val tipMapper = SystemBatteryTipListItemMapper(context)
            val items = mutableListOf<ListItem>()
            if (services.isNotEmpty()) {
                items.add(
                    ListItem(
                        -1,
                        getString(R.string.app_name),
                        getString(R.string.battery_tip_disable_services)
                    )
                )
            }
            items.addAll(services.map { serviceMapper.map(it) })

            items.add(
                ListItem(
                    -2,
                    getString(R.string.system),
                    getString(R.string.battery_tip_system)
                )
            )
            items.addAll(tips.map { tipMapper.map(it) })

            servicesListView.setItems(items)
        }

        // View - Percentage
        useEffect(percentageTextView, progressView, percent) {
            percentageTextView.text = formatter.formatPercentage(percent)
            progressView.progress = percent / 100f
            progressView.trackOpacity = 50
            progressView.progressColor = when {
                percent >= 75 -> AppColor.Green.color
                percent >= 50 -> AppColor.Yellow.color
                percent >= 25 -> AppColor.Orange.color
                else -> AppColor.Red.color
            }
        }

        // View - Capacity
        useEffect(capacityTextView, capacity) {
            capacityTextView.isVisible = capacity != 0f
            capacityTextView.text = formatter.formatElectricalCapacity(capacity)
        }

        // View - Title
        useEffect(titleView, time, isCharging) {
            titleView.title.isVisible = time != null

            if (time != null) {
                titleView.title.text = formatter.formatDuration(time)
            }

            if (time != null && !isCharging) {
                titleView.subtitle.isVisible = true
                titleView.subtitle.text = getString(R.string.time_until_empty)
            } else if (time != null) {
                titleView.subtitle.isVisible = true
                titleView.subtitle.text = getString(R.string.time_until_full)
            } else {
                titleView.subtitle.isVisible = false
            }
        }

        useEffect(titleView, readings) {
            titleView.leftButton.isVisible = readings.size >= 2
            titleView.leftButton.setOnClickListener {
                if (prefs.power.enableBatteryLog) {
                    val readingDuration =
                        Duration.between(readings.firstOrNull()?.time, readings.lastOrNull()?.time)
                    CustomUiUtils.showChart(
                        this,
                        getString(
                            R.string.battery_history,
                            formatter.formatDuration(readingDuration, false)
                        )
                    ) {
                        val chart = BatteryChart(it)
                        chart.plot(readings, false)
                    }
                } else {
                    Alerts.dialog(
                        requireContext(),
                        getString(R.string.pref_tiles_battery_log),
                        getString(R.string.pref_dialog_battery_log_summary),
                        okText = getString(R.string.settings),
                        onClose = {
                            if (it.not()) {
                                navController.navigate(
                                    R.id.action_settings_to_power_settings,
                                )
                            }

                        }
                    )
                }
            }
        }

        useEffect(titleView) {
            CustomUiUtils.setButtonState(titleView.leftButton, false)
            CustomUiUtils.setButtonState(titleView.rightButton, false)

            val settingsIntent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
            titleView.rightButton.isVisible = Intents.hasReceiver(context, settingsIntent)
            titleView.rightButton.setOnClickListener {
                startActivity(settingsIntent)
            }
        }

        // View - Health
        useEffect(healthTextView, health) {
            healthTextView.isVisible = health != BatteryHealth.Good
            healthTextView.text =
                getString(R.string.battery_health, formatter.formatBatteryHealth(health))
        }

        // View - Current
        useShowCurrent(
            currentTextView,
            current,
            isCharging,
            chargeMethod,
            formatter
        )

        // View - Low power toggle
        useLowPowerToggle(lowPowerSwitchView, lowPowerMode, prefs, triggerServicesUpdate)
    }

    private fun useRunningServices(batteryService: BatteryService): Pair<List<RunningService>, () -> Unit> {
        val context = useAndroidContext()
        val (servicesKey, triggerServicesUpdate) = useTrigger()
        val (services, setServices) = useState(emptyList<RunningService>())

        useEffect(servicesKey) {
            setServices(batteryService.getRunningServices(context))
        }

        useTimer(1000) {
            triggerServicesUpdate()
        }

        return services to triggerServicesUpdate
    }

    private fun useSystemBatteryTips(batteryService: BatteryService): List<SystemBatteryTip> {
        val context = useAndroidContext()
        val (tipsKey, triggerTipsUpdate) = useTrigger()
        val (tips, setTips) = useState(emptyList<SystemBatteryTip>())

        useEffect(tipsKey, resetOnResume) {
            setTips(batteryService.getSystemBatteryTips(context))
        }

        useTimer(10000) {
            triggerTipsUpdate()
        }

        return tips
    }

    private fun useShowCurrent(
        textView: TextView,
        current: Float,
        isCharging: Boolean,
        chargeMethod: BatteryChargingMethod,
        formatter: FormatService
    ) {
        useEffect(textView, current, isCharging, chargeMethod) {
            if (current.absoluteValue >= 0.5f) {
                val formattedCurrent = formatter.formatCurrent(current.absoluteValue)
                textView.text = when {
                    current > 500 -> getString(R.string.charging_fast, formattedCurrent)
                    current > 0 -> getString(R.string.charging_slow, formattedCurrent)
                    current < -500 -> getString(R.string.discharging_fast, formattedCurrent)
                    else -> getString(R.string.discharging_slow, formattedCurrent)
                }
            } else {
                textView.text = when {
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
    }

    private fun useLowPowerToggle(
        switch: SwitchCompat,
        lowPowerMode: LowPowerMode,
        prefs: UserPreferences,
        onChange: () -> Unit
    ) {
        useEffect(switch, lowPowerMode, prefs, onChange) {
            switch.isChecked = lowPowerMode.isEnabled()

            switch.setOnClickListener {
                if (lowPowerMode.isEnabled()) {
                    prefs.power.userEnabledLowPower = false
                    lowPowerMode.disable()
                } else {
                    prefs.power.userEnabledLowPower = true
                    lowPowerMode.enable()
                }
                onChange()
            }
        }
    }

}