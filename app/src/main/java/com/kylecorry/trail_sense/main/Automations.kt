package com.kylecorry.trail_sense.main

import android.content.Context
import android.content.IntentFilter
import androidx.core.os.bundleOf
import com.kylecorry.andromeda.core.system.BroadcastReceiverTopic
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.automations.Automation
import com.kylecorry.trail_sense.shared.automations.AutomationReceiver
import com.kylecorry.trail_sense.tools.battery.BatteryToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration
import com.kylecorry.trail_sense.tools.weather.receivers.SetWeatherMonitorStateReceiver

object Automations {
    fun setup(context: Context) {
        val tools = Tools.getTools(context, false)
        val actions = tools.flatMap { it.broadcasts.map { it.action } }
        val prefs = UserPreferences(context)

        // TODO: This should be loaded from the disk and kept up to date
        val automations = listOf(
            Automation(
                BatteryToolRegistration.ACTION_POWER_SAVING_MODE_CHANGED,
                listOfNotNull(
                    if (prefs.lowPowerModeDisablesWeather) {
                        AutomationReceiver(
                            WeatherToolRegistration.RECEIVER_SET_WEATHER_MONITOR_STATE
                        ) {
                            bundleOf(
                                SetWeatherMonitorStateReceiver.PARAM_WEATHER_MONITOR_STATE to !it.getBoolean(
                                    BatteryToolRegistration.PARAM_POWER_SAVING_MODE_ENABLED,
                                    false
                                )
                            )
                        }
                    } else {
                        null
                    }
                )

            )
        )

        actions.forEach { action ->
            val receiver = BroadcastReceiverTopic(context, IntentFilter(action))
            // TODO: Should there be a way to unregister? Maybe only register when something is listening?
            receiver.subscribe { intent ->
                // TODO: This should be run in the background
                val automationsToRun = automations.filter { it.broadcast == action }
                val receiversToRun = automationsToRun.flatMap { it.receivers.map { it.receiverId } }

                val availableReceivers = tools
                    .flatMap { it.receivers }
                    .filter { receiversToRun.contains(it.id) && it.isEnabled(context) }

                automationsToRun.forEach {
                    it.receivers.forEach { receiver ->
                        val toolReceiver =
                            availableReceivers.firstOrNull { it.id == receiver.receiverId }
                                ?: return@forEach
                        toolReceiver.receiver.onReceive(
                            context,
                            receiver.parameterTransform(intent.extras ?: bundleOf())
                        )
                    }
                }
                true
            }
        }
    }
}