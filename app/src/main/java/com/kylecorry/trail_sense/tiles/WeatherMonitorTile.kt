package com.kylecorry.trail_sense.tiles

import android.os.Build
import androidx.annotation.RequiresApi
import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.topics.map
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem

@RequiresApi(Build.VERSION_CODES.N)
class WeatherMonitorTile : TopicTile() {
    private val weather by lazy { WeatherSubsystem.getInstance(this) }
    private val formatter by lazy { FormatService.getInstance(this) }

    override val stateTopic: ITopic<FeatureState>
        get() = weather.weatherMonitorStateChanged

    override val subtitleTopic: ITopic<String>
        get() = weather.weatherMonitorFrequencyChanged.map {
            formatter.formatDuration(
                it,
                includeSeconds = true
            )
        }

    override fun stop() {
        weather.disableMonitor()
    }

    override fun start() {
        weather.enableMonitor()
    }
}