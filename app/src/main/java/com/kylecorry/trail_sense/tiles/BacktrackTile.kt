package com.kylecorry.trail_sense.tiles

import android.os.Build
import androidx.annotation.RequiresApi
import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.andromeda.core.topics.generic.map
import com.kylecorry.andromeda.core.topics.generic.replay
import com.kylecorry.trail_sense.navigation.paths.infrastructure.subsystem.BacktrackSubsystem
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService

@RequiresApi(Build.VERSION_CODES.N)
class BacktrackTile : TopicTile() {
    private val backtrack by lazy { BacktrackSubsystem.getInstance(this) }
    private val formatter by lazy { FormatService.getInstance(this) }

    override val stateTopic: ITopic<FeatureState>
        get() = backtrack.state.replay()

    override val subtitleTopic: ITopic<String>
        get() = backtrack.frequency.map { formatter.formatDuration(it, includeSeconds = true) }.replay()

    override fun stop() {
        backtrack.disable()
    }

    override fun start() {
        backtrack.enable(true)
    }
}