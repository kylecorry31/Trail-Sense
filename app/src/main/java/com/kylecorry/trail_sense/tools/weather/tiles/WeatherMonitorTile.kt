package com.kylecorry.trail_sense.tools.weather.tiles

import com.kylecorry.trail_sense.shared.tiles.ToolServiceTile
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration

class WeatherMonitorTile : ToolServiceTile(
    WeatherToolRegistration.SERVICE_WEATHER_MONITOR,
    WeatherToolRegistration.BROADCAST_WEATHER_MONITOR_STATE_CHANGED,
    WeatherToolRegistration.BROADCAST_WEATHER_MONITOR_FREQUENCY_CHANGED,
    isForegroundService = true
)
