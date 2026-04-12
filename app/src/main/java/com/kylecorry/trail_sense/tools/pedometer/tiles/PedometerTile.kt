package com.kylecorry.trail_sense.tools.pedometer.tiles

import android.os.Build
import androidx.annotation.RequiresApi
import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.andromeda.core.topics.generic.map
import com.kylecorry.andromeda.core.topics.generic.replay
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.tiles.TopicTile
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounter
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem

@RequiresApi(Build.VERSION_CODES.N)
class PedometerTile : TopicTile() {

    private val pedometer by lazy { PedometerSubsystem.getInstance(this) }
    private val formatter by lazy { FormatService.getInstance(this) }
    private val prefs by lazy { UserPreferences(this) }
    private val stepCounter by lazy { StepCounter(PreferencesSubsystem.getInstance(this).preferences) }

    override val stateTopic: ITopic<FeatureState>
        get() = pedometer.state.replay()

    // QS tile subtitle: steps and distance (distance topic also updates when stride changes)
    override val subtitleTopic: ITopic<String>
        get() = pedometer.distance.map { dist ->
            val steps = stepCounter.steps
            val stepsText = resources.getQuantityString(
                R.plurals.number_steps,
                steps.toInt(),
                steps.toInt()
            )
            val converted = dist.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
            val distText = formatter.formatDistance(
                converted,
                Units.getDecimalPlaces(converted.units)
            )
            "$stepsText · $distText"
        }.replay()

    override fun stop() {
        pedometer.disable()
    }

    override fun start() {
        startForegroundService {
            pedometer.enable()
        }
    }

}
