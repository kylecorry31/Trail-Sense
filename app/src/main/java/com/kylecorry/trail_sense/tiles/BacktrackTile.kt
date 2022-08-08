package com.kylecorry.trail_sense.tiles

import android.os.Build
import android.service.quicksettings.Tile
import androidx.annotation.RequiresApi
import com.kylecorry.andromeda.services.AndromedaTileService
import com.kylecorry.trail_sense.navigation.paths.infrastructure.subsystem.BacktrackSubsystem
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import java.time.Duration

@RequiresApi(Build.VERSION_CODES.N)
class BacktrackTile : AndromedaTileService() {

    private val formatService by lazy { FormatService(this) }
    private val backtrack by lazy { BacktrackSubsystem.getInstance(this) }

    override fun onClick() {
        super.onClick()

        when (backtrack.backtrackState) {
            FeatureState.On -> backtrack.disable()
            FeatureState.Off -> backtrack.enable(true)
            FeatureState.Unavailable -> {}
        }
    }

    override fun onStartListening() {
        onFrequencyChanged(backtrack.backtrackFrequency)
        onStateChanged(backtrack.backtrackState)
        backtrack.backtrackFrequencyChanged.subscribe(this::onFrequencyChanged)
        backtrack.backtrackStateChanged.subscribe(this::onStateChanged)
    }

    override fun onStopListening() {
        backtrack.backtrackFrequencyChanged.unsubscribe(this::onFrequencyChanged)
        backtrack.backtrackStateChanged.unsubscribe(this::onStateChanged)
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