package com.kylecorry.trail_sense.tiles

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import java.time.Duration

@RequiresApi(Build.VERSION_CODES.N)
class WhiteNoiseTile : TileService() {

    private val stateChecker = Intervalometer {
        when {
            WhiteNoiseService.isRunning -> {
                qsTile.state = Tile.STATE_ACTIVE
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
        if (WhiteNoiseService.isRunning) {
            WhiteNoiseService.stop(this)
            qsTile.state = Tile.STATE_INACTIVE
        } else {
            WhiteNoiseService.start(this)
            qsTile.state = Tile.STATE_ACTIVE
        }

        qsTile.updateTile()
    }
}