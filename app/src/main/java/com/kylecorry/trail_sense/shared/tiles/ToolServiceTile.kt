package com.kylecorry.trail_sense.shared.tiles

import android.os.Build
import android.os.Bundle
import android.service.quicksettings.Tile
import androidx.annotation.RequiresApi
import com.kylecorry.andromeda.background.services.AndromedaTileService
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.getFeatureState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@RequiresApi(Build.VERSION_CODES.N)
abstract class ToolServiceTile(
    private val serviceId: String,
    private val stateChangeBroadcastId: String,
    private val frequencyChangeBroadcastId: String? = null,
    private val isForegroundService: Boolean = false
) : AndromedaTileService() {
    protected val service by lazy { Tools.getService(this, serviceId) }

    private val formatter by lazy { FormatService.getInstance(this) }

    open fun stop() {
        CoroutineScope(Dispatchers.Default).launch {
            service?.disable()
        }
    }

    open fun start() {
        if (isForegroundService) {
            startForegroundService {
                service?.enable()
            }
        } else {
            CoroutineScope(Dispatchers.Default).launch {
                service?.enable()
            }
        }
    }

    override fun onClick() {
        super.onClick()

        when (service?.getFeatureState()) {
            FeatureState.On -> stop()
            FeatureState.Off -> start()
            else -> {
                // Do nothing, the feature is disabled
            }
        }
    }

    override fun onStartListening() {
        Tools.subscribe(stateChangeBroadcastId, this::onStateChanged)
        updateState()
        if (frequencyChangeBroadcastId != null) {
            Tools.subscribe(frequencyChangeBroadcastId, this::onFrequencyChanged)
            updateFrequency()
        }
    }

    override fun onStopListening() {
        Tools.unsubscribe(stateChangeBroadcastId, this::onStateChanged)
        if (frequencyChangeBroadcastId != null) {
            Tools.unsubscribe(frequencyChangeBroadcastId, this::onFrequencyChanged)
        }
    }

    private fun onFrequencyChanged(data: Bundle): Boolean {
        updateFrequency()
        return true
    }

    private fun onStateChanged(data: Bundle): Boolean {
        updateState()
        return true
    }

    private fun updateState() {
        setState(
            when (service?.getFeatureState()) {
                FeatureState.On -> Tile.STATE_ACTIVE
                FeatureState.Off -> Tile.STATE_INACTIVE
                else -> Tile.STATE_UNAVAILABLE
            }
        )
    }

    private fun updateFrequency() {
        val frequency = service?.getFrequency() ?: return
        val formatted = formatter.formatDuration(frequency, includeSeconds = true)
        tryOrLog {
            setSubtitle(formatted)
        }
    }
}