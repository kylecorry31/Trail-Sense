package com.kylecorry.trail_sense.tiles

import android.os.Build
import android.service.quicksettings.Tile
import androidx.annotation.RequiresApi
import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.services.AndromedaTileService
import com.kylecorry.trail_sense.shared.FeatureState

@RequiresApi(Build.VERSION_CODES.N)
abstract class TopicTile : AndromedaTileService() {
    abstract val stateTopic: ITopic<FeatureState>
    abstract val subtitleTopic: ITopic<String>

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
        subtitleTopic.subscribe(this::onSubtitleChanged)
        stateTopic.subscribe(this::onStateChanged)
    }

    override fun onStopListening() {
        subtitleTopic.unsubscribe(this::onSubtitleChanged)
        stateTopic.unsubscribe(this::onStateChanged)
    }

    private fun onSubtitleChanged(subtitle: String): Boolean {
        tryOrLog {
            setSubtitle(subtitle)
        }
        return true
    }

    private fun onStateChanged(state: FeatureState): Boolean {
        tryOrLog {
            setState(
                when (state) {
                    FeatureState.On -> Tile.STATE_ACTIVE
                    FeatureState.Off -> Tile.STATE_INACTIVE
                    FeatureState.Unavailable -> Tile.STATE_UNAVAILABLE
                }
            )
        }
        return true
    }
}