package com.kylecorry.trail_sense.tiles

import android.os.Build
import androidx.annotation.RequiresApi
import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.andromeda.core.topics.generic.map
import com.kylecorry.andromeda.core.topics.generic.replay
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem

@RequiresApi(Build.VERSION_CODES.N)
class WeatherMonitorTile : TopicTile() {
    private val weather by lazy { WeatherSubsystem.getInstance(this) }
    private val formatter by lazy { FormatService.getInstance(this) }

    override val stateTopic: ITopic<FeatureState>
        get() = weather.weatherMonitorState.replay()

    override val subtitleTopic: ITopic<String>
        get() = weather.weatherMonitorFrequency.map {
            formatter.formatDuration(
                it,
                includeSeconds = true
            )
        }.replay()

    override fun stop() {
        weather.disableMonitor()
    }

    override fun start() {
        weather.enableMonitor()
    }
}