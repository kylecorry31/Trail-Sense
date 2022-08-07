package com.kylecorry.trail_sense.navigation.paths.infrastructure.subsystem

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.andromeda.core.topics.generic.Topic
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.navigation.paths.infrastructure.commands.StopBacktrackCommand
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.Duration

class BacktrackSubsystem private constructor(private val context: Context) {

    private val _backtrackStateChanged = Topic<FeatureState>()
    val backtrackStateChanged: ITopic<FeatureState> = _backtrackStateChanged

    private val _backtrackFrequencyChanged = Topic<Duration>()
    val backtrackFrequencyChanged: ITopic<Duration> = _backtrackFrequencyChanged

    private val sharedPrefs by lazy { Preferences(context) }
    private val prefs by lazy { UserPreferences(context) }

    private val stateChangePrefKeys = listOf(
        R.string.pref_backtrack_enabled,
        R.string.pref_low_power_mode,
        R.string.pref_low_power_mode_backtrack,
        R.string.pref_backtrack_frequency
    ).map { context.getString(it) }

    var backtrackState: FeatureState = calculateBacktrackState()
        private set

    var backtrackFrequency: Duration = calculateBacktrackFrequency()
        private set

    init {
        sharedPrefs.onChange.subscribe { key ->
            if (key in stateChangePrefKeys) {
                val state = calculateBacktrackState()
                if (state != backtrackState) {
                    backtrackState = state
                    _backtrackStateChanged.notifySubscribers(state)
                }

                val frequency = calculateBacktrackFrequency()
                if (frequency != backtrackFrequency) {
                    backtrackFrequency = frequency
                    _backtrackFrequencyChanged.notifySubscribers(frequency)
                }
            }
            true
        }
    }

    fun enable(startNewPath: Boolean) {
        prefs.backtrackEnabled = true
        BacktrackScheduler.start(context, startNewPath)
    }

    fun disable() {
        StopBacktrackCommand(context).execute()
    }

    private fun calculateBacktrackState(): FeatureState {
        return if (BacktrackScheduler.isOn(context)) {
            FeatureState.On
        } else if (BacktrackScheduler.isDisabled(context)) {
            FeatureState.Unavailable
        } else {
            FeatureState.Off
        }
    }

    private fun calculateBacktrackFrequency(): Duration {
        return prefs.backtrackRecordFrequency
    }


    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: BacktrackSubsystem? = null

        @Synchronized
        fun getInstance(context: Context): BacktrackSubsystem {
            if (instance == null) {
                instance = BacktrackSubsystem(context.applicationContext)
            }
            return instance!!
        }
    }

}