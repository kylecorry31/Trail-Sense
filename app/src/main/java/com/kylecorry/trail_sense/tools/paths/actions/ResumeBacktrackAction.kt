package com.kylecorry.trail_sense.tools.paths.actions

import android.content.Context
import android.os.Bundle
import com.kylecorry.trail_sense.tools.paths.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.tools.tools.infrastructure.Action

class ResumeBacktrackAction : Action {
    override suspend fun onReceive(context: Context, data: Bundle) {
        if (BacktrackScheduler.isOn(context)) {
            BacktrackScheduler.start(context, false)
        }
    }
}