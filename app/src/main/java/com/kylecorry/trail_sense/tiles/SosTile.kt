package com.kylecorry.trail_sense.tiles

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightState
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightHandler
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.SosService
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import java.time.Duration

@RequiresApi(Build.VERSION_CODES.N)
class SosTile : TileService() {

    private val flashlight by lazy { FlashlightHandler.getInstance(this) }

    private val stateChecker = Intervalometer {
        if (flashlight.getState() == FlashlightState.SOS) {
            qsTile.state = Tile.STATE_ACTIVE
        } else {
            qsTile.state = Tile.STATE_INACTIVE
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
        if (flashlight.getState() == FlashlightState.SOS) {
            flashlight.set(FlashlightState.Off)
            qsTile.state = Tile.STATE_INACTIVE
        } else {
            flashlight.set(FlashlightState.SOS)
            qsTile.state = Tile.STATE_ACTIVE
        }

        qsTile.updateTile()
    }
}