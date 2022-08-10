package com.kylecorry.trail_sense.tiles

import android.os.Build
import android.service.quicksettings.Tile
import androidx.annotation.RequiresApi
import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.andromeda.services.AndromedaTileService
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import java.time.Duration

@RequiresApi(Build.VERSION_CODES.N)
abstract class TopicTile : AndromedaTileService() {

    private val formatService by lazy { FormatService(this) }

    abstract val stateTopic: ITopic<FeatureState>

    // TODO: Make this the subtitle topic (map duration on consumer)
    abstract val frequencyTopic: ITopic<Duration>

    abstract fun stop()
    abstract fun start()

    override fun onClick() {
        super.onClick()

        when (stateTopic.value.get()) {
            FeatureState.On -> stop()
            FeatureState.Off -> start()
            else -> {}
        }
    }

    override fun onStartListening() {
        onFrequencyChanged(frequencyTopic.value.get())
        onStateChanged(stateTopic.value.get())
        frequencyTopic.subscribe(this::onFrequencyChanged)
        stateTopic.subscribe(this::onStateChanged)
    }

    override fun onStopListening() {
        frequencyTopic.unsubscribe(this::onFrequencyChanged)
        stateTopic.unsubscribe(this::onStateChanged)
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