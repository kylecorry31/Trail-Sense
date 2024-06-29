package com.kylecorry.trail_sense.main

import android.content.Context
import android.content.IntentFilter
import androidx.core.os.bundleOf
import com.kylecorry.andromeda.core.system.BroadcastReceiverTopic
import com.kylecorry.trail_sense.tools.battery.BatteryToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration

object Automations {
    fun setup(context: Context) {
        val tools = Tools.getTools(context, false)
        val actions = tools.flatMap { it.broadcasts.map { it.action } }

        // TODO: This should be loaded from the disk and kept up to date
        val automations = mapOf(
            BatteryToolRegistration.ACTION_LOW_POWER_MODE_CHANGED to listOf(
                WeatherToolRegistration.RECEIVER_WEATHER_MONITOR_LOW_POWER_MODE
            )
        )

        // Register all broadcast receivers
        actions.forEach { action ->
            // Register the broadcast receiver
            val receiver = BroadcastReceiverTopic(context, IntentFilter(action))
            // TODO: Should there be a way to unregister? Mabye only register when something is listening?
            receiver.subscribe { intent ->
                // TODO: This should be run in the background
                Tools.getTools(context)
                    .flatMap { it.receivers }
                    .filter { automations.getOrDefault(action, emptyList()).contains(it.id) && it.isEnabled(context) }
                    .forEach { it.receiver.onReceive(context, intent.extras ?: bundleOf()) }
                true
            }
        }
    }
}