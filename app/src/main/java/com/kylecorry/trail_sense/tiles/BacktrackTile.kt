package com.kylecorry.trail_sense.tiles

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.BacktrackScheduler
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import java.time.Duration

@RequiresApi(Build.VERSION_CODES.N)
class BacktrackTile: TileService() {

    private val prefs by lazy { UserPreferences(this) }

    private val stateChecker = Intervalometer {
        when {
            BacktrackScheduler.isOn(this) -> {
                qsTile.state = Tile.STATE_ACTIVE
            }
            BacktrackScheduler.isDisabled(this) -> {
                qsTile.state = Tile.STATE_UNAVAILABLE
            }
            else -> {
                qsTile.state = Tile.STATE_INACTIVE
            }
        }
        qsTile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        stateChecker.interval(Duration.ofMillis(100))
    }

    override fun onStopListening() {
        super.onStopListening()
        stateChecker.stop()
    }

    override fun onClick() {
        super.onClick()
        if (BacktrackScheduler.isOn(this)){
            prefs.backtrackEnabled = false
            BacktrackScheduler.stop(this)
            qsTile.state = Tile.STATE_INACTIVE
        } else if (!BacktrackScheduler.isDisabled(this)) {
            prefs.backtrackEnabled = true
            BacktrackScheduler.start(this)
            qsTile.state = Tile.STATE_ACTIVE
        }

        qsTile.updateTile()
    }
}