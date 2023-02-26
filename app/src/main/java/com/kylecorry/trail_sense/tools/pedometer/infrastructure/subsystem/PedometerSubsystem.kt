package com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.andromeda.core.topics.generic.Topic
import com.kylecorry.andromeda.core.topics.generic.distinct
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Speed
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.ZERO_SPEED
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.pedometer.domain.StrideLengthPaceCalculator
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounter
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService
import java.time.Duration
import java.time.Instant
import java.util.*

class PedometerSubsystem private constructor(private val context: Context) : IPedometerSubsystem {

    private val sharedPrefs by lazy { PreferencesSubsystem.getInstance(context).preferences }
    private val prefsChanged by lazy { sharedPrefs.onChange }
    private val prefs by lazy { UserPreferences(context) }
    private val stepCounter by lazy { StepCounter(sharedPrefs) }

    private val _steps = Topic(defaultValue = Optional.of(stepCounter.steps))
    private val _distance = Topic(defaultValue = Optional.of(calculateDistance()))
    private val _pace = Topic(defaultValue = Optional.of(calculatePace()))
    private val _state = Topic(defaultValue = Optional.of(calculateState()))

    override val steps: ITopic<Long>
        get() = _steps.distinct()
    override val distance: ITopic<Distance>
        get() = _distance.distinct()
    override val pace: ITopic<Speed>
        get() = _pace.distinct()
    override val state: ITopic<FeatureState>
        get() = _state.distinct()

    override fun enable() {
        prefs.pedometer.isEnabled = true
        StepCounterService.start(context)
    }

    override fun disable() {
        prefs.pedometer.isEnabled = false
        StepCounterService.stop(context)
    }

    private val stateChangePrefKeys = listOf(
        R.string.pref_pedometer_enabled,
        R.string.pref_low_power_mode
    ).map { context.getString(it) }

    private val stepChangePrefKeys = listOf(
        StepCounter.STEPS_KEY
    )

    private val distanceChangePrefKeys = listOf(
        context.getString(R.string.pref_stride_length),
        StepCounter.STEPS_KEY
    )

    private val paceChangePrefKeys = listOf(
        context.getString(R.string.pref_stride_length),
        StepCounter.STEPS_KEY,
        StepCounter.LAST_RESET_KEY
    )


    init {
        // Keep them up to date
        state.subscribe { true }
        steps.subscribe { true }
        distance.subscribe { true }
        pace.subscribe { true }

        prefsChanged.subscribe {
            if (it in stateChangePrefKeys) {
                _state.publish(calculateState())
            }

            if (it in stepChangePrefKeys) {
                _steps.publish(stepCounter.steps)
            }

            if (it in distanceChangePrefKeys) {
                _distance.publish(calculateDistance())
            }

            if (it in paceChangePrefKeys) {
                _pace.publish(calculatePace())
            }

            true
        }
    }


    private fun calculateState(): FeatureState {
        return if (isDisabled()) {
            FeatureState.Unavailable
        } else if (prefs.pedometer.isEnabled) {
            FeatureState.On
        } else {
            FeatureState.Off
        }
    }

    private fun isDisabled(): Boolean {
        val hasPermission = Permissions.canRecognizeActivity(context)
        return !Sensors.hasSensor(
            context,
            Sensor.TYPE_STEP_COUNTER
        ) || !hasPermission || prefs.isLowPowerModeOn
    }

    private fun calculateDistance(): Distance {
        val paceCalculator = StrideLengthPaceCalculator(prefs.pedometer.strideLength)
        return paceCalculator.distance(stepCounter.steps).meters()
    }

    private fun calculatePace(): Speed {
        val paceCalculator = StrideLengthPaceCalculator(prefs.pedometer.strideLength)
        val lastReset = stepCounter.startTime
        val steps = stepCounter.steps

        if (lastReset == null) {
            return ZERO_SPEED
        }

        return paceCalculator.speed(steps, Duration.between(lastReset, Instant.now()))
    }


    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: PedometerSubsystem? = null

        @Synchronized
        fun getInstance(context: Context): PedometerSubsystem {
            if (instance == null) {
                instance = PedometerSubsystem(context.applicationContext)
            }
            return instance!!
        }
    }


}