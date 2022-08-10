package com.kylecorry.trail_sense.tiles

import android.os.Build
import androidx.annotation.RequiresApi
import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.trail_sense.navigation.paths.infrastructure.subsystem.BacktrackSubsystem
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.topics.map

@RequiresApi(Build.VERSION_CODES.N)
class BacktrackTile : TopicTile() {
    private val backtrack by lazy { BacktrackSubsystem.getInstance(this) }
    private val formatter by lazy { FormatService.getInstance(this) }

    override val stateTopic: ITopic<FeatureState>
        get() = backtrack.backtrackStateChanged

    override val subtitleTopic: ITopic<String>
        get() = backtrack.backtrackFrequencyChanged.map { formatter.formatDuration(it, includeSeconds = true) }

    override fun stop() {
        backtrack.disable()
    }

    override fun start() {
        backtrack.enable(true)
    }
}