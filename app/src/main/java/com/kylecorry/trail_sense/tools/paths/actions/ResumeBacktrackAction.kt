package com.kylecorry.trail_sense.tools.paths.actions

import android.content.Context
import android.os.Bundle
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Action
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class ResumeBacktrackAction : Action {
    override suspend fun onReceive(context: Context, data: Bundle) {
        val service = Tools.getService(context, PathsToolRegistration.SERVICE_BACKTRACK)
        service?.restart()
    }
}