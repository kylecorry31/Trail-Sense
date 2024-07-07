package com.kylecorry.trail_sense.tools.paths.tiles

import android.os.Build
import androidx.annotation.RequiresApi
import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.andromeda.core.topics.generic.map
import com.kylecorry.andromeda.core.topics.generic.replay
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.tiles.TopicTile
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.paths.infrastructure.subsystem.BacktrackSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
class BacktrackTile : TopicTile() {
    private val backtrack by lazy { BacktrackSubsystem.getInstance(this) }
    private val backtrackService by lazy {
        Tools.getService(
            this,
            PathsToolRegistration.SERVICE_BACKTRACK
        )
    }
    private val formatter by lazy { FormatService.getInstance(this) }

    override val stateTopic: ITopic<FeatureState>
        get() = backtrack.state.replay()

    override val subtitleTopic: ITopic<String>
        get() = backtrack.frequency.map { formatter.formatDuration(it, includeSeconds = true) }
            .replay()

    override fun stop() {
        CoroutineScope(Dispatchers.Default).launch {
            backtrackService?.disable()
        }
    }

    override fun start() {
        startForegroundService {
            backtrackService?.enable()
        }
    }
}