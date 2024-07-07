package com.kylecorry.trail_sense.tools.paths.tiles

import android.os.Build
import androidx.annotation.RequiresApi
import com.kylecorry.trail_sense.shared.tiles.ToolServiceTile
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
class BacktrackTile : ToolServiceTile(
    PathsToolRegistration.SERVICE_BACKTRACK,
    PathsToolRegistration.BROADCAST_BACKTRACK_STATE_CHANGED,
    PathsToolRegistration.BROADCAST_BACKTRACK_FREQUENCY_CHANGED
) {

    override fun stop() {
        CoroutineScope(Dispatchers.Default).launch {
            service?.disable()
        }
    }

    override fun start() {
        startForegroundService {
            service?.enable()
        }
    }
}