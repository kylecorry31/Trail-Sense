package com.kylecorry.trail_sense.tools.paths.tiles

import com.kylecorry.trail_sense.shared.tiles.ToolServiceTile
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration

class BacktrackTile : ToolServiceTile(
    PathsToolRegistration.SERVICE_BACKTRACK,
    PathsToolRegistration.BROADCAST_BACKTRACK_STATE_CHANGED,
    PathsToolRegistration.BROADCAST_BACKTRACK_FREQUENCY_CHANGED,
    isForegroundService = true
)
