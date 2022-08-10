package com.kylecorry.trail_sense.tiles

import android.os.Build
import androidx.annotation.RequiresApi
import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem
import java.time.Duration

@RequiresApi(Build.VERSION_CODES.N)
class WeatherMonitorTile : TopicTile() {
    private val weather by lazy { WeatherSubsystem.getInstance(this) }

    override val stateTopic: ITopic<FeatureState>
        get() = weather.weatherMonitorStateChanged
    override val frequencyTopic: ITopic<Duration>
        get() = weather.weatherMonitorFrequencyChanged

    override fun stop() {
        weather.disableMonitor()
    }

    override fun start() {
        weather.enableMonitor()
    }
}