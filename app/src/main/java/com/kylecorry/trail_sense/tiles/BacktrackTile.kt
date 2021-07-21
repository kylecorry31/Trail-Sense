package com.kylecorry.trail_sense.tiles

import android.os.Build
import androidx.annotation.RequiresApi
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.BacktrackScheduler

@RequiresApi(Build.VERSION_CODES.N)
class BacktrackTile: CustomTileService() {

    private val prefs by lazy { UserPreferences(this) }
    private val formatService by lazy { FormatServiceV2(this) }

    override fun isOn(): Boolean {
        return BacktrackScheduler.isOn(this)
    }

    override fun isDisabled(): Boolean {
        return BacktrackScheduler.isDisabled(this)
    }

    override fun onInterval() {
        setSubtitle(formatService.formatDuration(prefs.backtrackRecordFrequency))
    }

    override fun start() {
        prefs.backtrackEnabled = true
        BacktrackScheduler.start(this)
    }

    override fun stop() {
        prefs.backtrackEnabled = false
        BacktrackScheduler.stop(this)
    }
}