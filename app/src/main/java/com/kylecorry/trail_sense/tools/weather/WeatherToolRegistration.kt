package com.kylecorry.trail_sense.tools.weather

import android.content.Context
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.ui.Tool
import com.kylecorry.trail_sense.tools.tools.ui.ToolCategory
import com.kylecorry.trail_sense.tools.tools.ui.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.ui.Tools
import com.kylecorry.trail_sense.tools.weather.quickactions.QuickActionWeatherMonitor

object WeatherToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.WEATHER,
            context.getString(R.string.weather),
            R.drawable.cloud,
            R.id.action_weather,
            ToolCategory.Weather,
            guideId = R.raw.guide_tool_weather,
            settingsNavAction = R.id.weatherSettingsFragment,
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_WEATHER_MONITOR,
                    context.getString(R.string.weather_monitor),
                    ::QuickActionWeatherMonitor
                )
            ),
            isAvailable = { Sensors.hasBarometer(it) }
        )
    }
}