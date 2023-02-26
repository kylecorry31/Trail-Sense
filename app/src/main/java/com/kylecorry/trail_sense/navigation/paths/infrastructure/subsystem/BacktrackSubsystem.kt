package com.kylecorry.trail_sense.navigation.paths.infrastructure.subsystem

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.andromeda.core.topics.generic.Topic
import com.kylecorry.andromeda.core.topics.generic.distinct
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.navigation.paths.infrastructure.commands.StopBacktrackCommand
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.getOrNull
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import java.time.Duration
import java.util.*

class BacktrackSubsystem private constructor(private val context: Context) {

    private val sharedPrefs by lazy { PreferencesSubsystem.getInstance(context).preferences }
    private val prefs by lazy { UserPreferences(context) }

    private val _state = Topic(defaultValue = Optional.of(calculateBacktrackState()))
    val state: ITopic<FeatureState> = _state.distinct()

    private val _frequency = Topic(defaultValue = Optional.of(calculateBacktrackFrequency()))
    val frequency: ITopic<Duration> = _frequency.distinct()

    private val stateChangePrefKeys = listOf(
        R.string.pref_backtrack_enabled,
        R.string.pref_low_power_mode,
        R.string.pref_low_power_mode_backtrack
    ).map { context.getString(it) }

    private val frequencyChangePrefKeys = listOf(
        R.string.pref_backtrack_frequency
    ).map { context.getString(it) }

    init {
        // Keep them up to date
        state.subscribe { true }
        frequency.subscribe { true }

        sharedPrefs.onChange.subscribe { key ->
            if (key in stateChangePrefKeys) {
                val state = calculateBacktrackState()
                _state.publish(state)
            }

            if (key in frequencyChangePrefKeys) {
                val frequency = calculateBacktrackFrequency()
                _frequency.publish(frequency)
            }
            true
        }
    }

    fun getState(): FeatureState {
        return state.getOrNull() ?: FeatureState.Off
    }

    fun getFrequency(): Duration {
        return frequency.getOrNull() ?: Duration.ofMinutes(30)
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