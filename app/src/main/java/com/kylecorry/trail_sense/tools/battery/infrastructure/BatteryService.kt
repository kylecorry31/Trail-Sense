package com.kylecorry.trail_sense.tools.battery.infrastructure

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.kylecorry.andromeda.battery.IBattery
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.battery.domain.BatteryReading
import com.kylecorry.trail_sense.tools.battery.domain.BatteryUsage
import com.kylecorry.trail_sense.tools.battery.domain.PowerService
import com.kylecorry.trail_sense.tools.battery.domain.RunningService
import com.kylecorry.trail_sense.tools.battery.domain.SystemBatteryTip
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration

class BatteryService {

    private val powerService = PowerService()

    fun getRunningServices(context: Context): List<RunningService> {
        val tools = Tools.getTools(context)
        return tools
            .flatMap { it.services }
            .filter { it.isRunning() }
            .map {
                RunningService(
                    it.name,
                    it.getFrequency()
                ) {
                    it.disable()
                }
            }
    }

    fun getTimeUntilEmpty(battery: IBattery, readings: List<BatteryReading>): Duration? {
        val capacity = battery.percent
        val rates = powerService.getRates(readings, Duration.ofMinutes(5), false)
        val lastDischargeRate = rates.lastOrNull { it < 0f } ?: return null
        return powerService.getTimeUntilEmpty(capacity, lastDischargeRate)
    }

    fun getTimeUntilFull(battery: IBattery, readings: List<BatteryReading>): Duration? {
        val capacity = battery.percent
        val rates = powerService.getRates(readings, Duration.ofMinutes(5), false)
        val lastChargeRate = rates.lastOrNull { it > 0f } ?: return null
        val maxCapacity = 100f
        return powerService.getTimeUntilFull(capacity, maxCapacity, lastChargeRate)
    }

    fun getSystemBatteryTips(context: Context): List<SystemBatteryTip> {
        val isAirplaneOn = tryOrDefault(false) { SystemSettings.isAirplaneModeEnabled(context) }
        val isWifiOn = tryOrDefault(true) { SystemSettings.isWifiEnabled(context) }
        val isBluetoothOn = tryOrDefault(true) { SystemSettings.isBluetoothEnabled(context) }
        val isNfcOn = tryOrDefault(true) { SystemSettings.isNfcEnabled(context) }
        val isPowerSaverOn = tryOrDefault(false) { SystemSettings.isPowerSaverEnabled(context) }
        val isLocationOn = tryOrDefault(true) { SystemSettings.isLocationEnabled(context) }
        val isAutomaticBrightnessOn =
            tryOrDefault(false) { SystemSettings.isAutomaticBrightnessEnabled(context) }
        val screenTimeout =
            tryOrDefault(60000) { SystemSettings.getScreenOffTimeout(context) } / 1000f
        val isDarkTheme = tryOrDefault(false) { SystemSettings.isDarkThemeEnabled(context) }

        return listOf(
            SystemBatteryTip(
                context.getString(R.string.airplane_mode),
                if (isAirplaneOn) context.getString(R.string.on) else context.getString(R.string.battery_tip_airplane_mode),
                if (isAirplaneOn) BatteryUsage.Low else BatteryUsage.High,
                getSystemAction(context, Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            ),
            SystemBatteryTip(
                context.getString(R.string.wifi),
                if (!isWifiOn) context.getString(R.string.off) else context.getString(R.string.battery_tip_wifi),
                if (isWifiOn) BatteryUsage.High else BatteryUsage.Low,
                getSystemAction(
                    context, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Settings.Panel.ACTION_WIFI
                    } else {
                        Settings.ACTION_WIFI_SETTINGS
                    }
                )
            ),
            SystemBatteryTip(
                context.getString(R.string.bluetooth),
                if (!isBluetoothOn) context.getString(R.string.off) else context.getString(R.string.battery_tip_bluetooth),
                if (isBluetoothOn) BatteryUsage.High else BatteryUsage.Low,
                getSystemAction(context, Settings.ACTION_BLUETOOTH_SETTINGS)
            ),
            SystemBatteryTip(
                context.getString(R.string.nfc),
                if (!isNfcOn) context.getString(R.string.off) else context.getString(R.string.battery_tip_nfc),
                if (isNfcOn) BatteryUsage.High else BatteryUsage.Low,
                getSystemAction(
                    context, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Settings.Panel.ACTION_NFC
                    } else {
                        Settings.ACTION_NFC_SETTINGS
                    }
                )
            ),
            SystemBatteryTip(
                context.getString(R.string.location),
                if (!isLocationOn) context.getString(R.string.off) else context.getString(R.string.battery_tip_location),
                if (isLocationOn) BatteryUsage.High else BatteryUsage.Low,
                getSystemAction(context, Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            ),
            SystemBatteryTip(
                context.getString(R.string.battery_saver),
                if (isPowerSaverOn) context.getString(R.string.on) else context.getString(R.string.battery_tip_battery_saver),
                if (isPowerSaverOn) BatteryUsage.Low else BatteryUsage.High,
                getSystemAction(context, Settings.ACTION_BATTERY_SAVER_SETTINGS)
            ),
            SystemBatteryTip(
                context.getString(R.string.data_saver),
                context.getString(R.string.battery_tip_data_saver),
                BatteryUsage.Unknown,
                getSystemAction(context, "android.settings.DATA_SAVER_SETTINGS")
            ),
            SystemBatteryTip(
                context.getString(R.string.adaptive_brightness),
                if (isAutomaticBrightnessOn) context.getString(R.string.on) else context.getString(R.string.battery_tip_adaptive_brightness),
                if (isAutomaticBrightnessOn) BatteryUsage.Low else BatteryUsage.Moderate,
                getSystemAction(context, Settings.ACTION_DISPLAY_SETTINGS)
            ),
            SystemBatteryTip(
                context.getString(R.string.screen_timeout),
                context.getString(R.string.battery_tip_display_timeout),
                if (screenTimeout <= 60) BatteryUsage.Low else BatteryUsage.Moderate,
                getSystemAction(context, Settings.ACTION_DISPLAY_SETTINGS)
            ),
            SystemBatteryTip(
                context.getString(R.string.dark_theme),
                if (isDarkTheme) context.getString(R.string.on) else context.getString(R.string.battery_tip_dark_theme),
                if (isDarkTheme) BatteryUsage.Low else BatteryUsage.Moderate,
                getSystemAction(context, Settings.ACTION_DISPLAY_SETTINGS)
            ),
            SystemBatteryTip(
                context.getString(R.string.restrict_app_background_activity),
                context.getString(R.string.battery_tip_background_apps),
                BatteryUsage.Unknown,
                getSystemAction(context, Settings.ACTION_APPLICATION_SETTINGS)
            ),
            SystemBatteryTip(
                context.getString(R.string.other_tips),
                context.getString(R.string.battery_tip_other),
                BatteryUsage.Unknown,
                null
            )
        )
    }

    private fun getSystemAction(context: Context, action: String): ((Context) -> Unit)? {
        val intent = Intent(action)
        if (Intents.hasReceiver(context, intent)) {
            return { context: Context -> context.startActivity(intent) }
        }
        return null
    }


}