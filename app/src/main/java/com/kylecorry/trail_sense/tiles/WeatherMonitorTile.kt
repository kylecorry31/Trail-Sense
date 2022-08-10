package com.kylecorry.trail_sense.tiles

import android.os.Build
import android.service.quicksettings.Tile
import androidx.annotation.RequiresApi
import com.kylecorry.andromeda.services.AndromedaTileService
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem
import java.time.Duration

@RequiresApi(Build.VERSION_CODES.N)
class WeatherMonitorTile : AndromedaTileService() {

    private val formatService by lazy { FormatService(this) }
    private val weather by lazy { WeatherSubsystem.getInstance(this) }

    override fun onClick() {
        super.onClick()
        when (weather.weatherMonitorStateChanged.value.get()) {
            FeatureState.On -> weather.disableMonitor()
            FeatureState.Off -> weather.enableMonitor()
            FeatureState.Unavailable -> {}
        }
    }

    override fun onStartListening() {
        onFrequencyChanged(weather.weatherMonitorFrequencyChanged.value.get())
        onStateChanged(weather.weatherMonitorStateChanged.value.get())
        weather.weatherMonitorFrequencyChanged.subscribe(this::onFrequencyChanged)
        weather.weatherMonitorStateChanged.subscribe(this::onStateChanged)
    }

    override fun onStopListening() {
        weather.weatherMonitorFrequencyChanged.unsubscribe(this::onFrequencyChanged)
        weather.weatherMonitorStateChanged.unsubscribe(this::onStateChanged)
    }

    private fun onFrequencyChanged(frequency: Duration): Boolean {
        setSubtitle(
            formatService.formatDuration(
                frequency,
                includeSeconds = true
            )
        )
        return true
    }

    private fun onStateChanged(state: FeatureState): Boolean {
        setState(
            when (state) {
                FeatureState.On -> Tile.STATE_ACTIVE
                FeatureState.Off -> Tile.STATE_INACTIVE
                FeatureState.Unavailable -> Tile.STATE_UNAVAILABLE
            }
        )
        return true
    }
}