package com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.os.Bundle
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.luna.concurrency.BackgroundTask
import com.kylecorry.luna.topics.generic.ITopic
import com.kylecorry.luna.topics.generic.Topic
import com.kylecorry.luna.topics.generic.distinct
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.pedometer.PedometerToolRegistration
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackerService
import com.kylecorry.trail_sense.tools.pedometer.domain.StrideLengthPaceCalculator
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Optional
import kotlin.jvm.optionals.getOrDefault

class PedometerSubsystem private constructor(private val context: Context) : IPedometerSubsystem {

    private val sharedPrefs by lazy { PreferencesSubsystem.getInstance(context).preferences }
    private val prefsChanged by lazy { sharedPrefs.onChange }
    private val prefs by lazy { UserPreferences(context) }
    private val stepTrackerService by lazy { getAppService<StepTrackerService>() }
    private val _steps = Topic(defaultValue = Optional.of(0L))
    private val _distance = Topic(defaultValue = Optional.of(calculateDistance()))
    private val _state = Topic(defaultValue = Optional.of(calculateState()))

    override val steps: ITopic<Long>
        get() = _steps.distinct()
    override val distance: ITopic<Distance>
        get() = _distance.distinct()
    override val state: ITopic<FeatureState>
        get() = _state.distinct()

    override fun enable() {
        prefs.pedometer.isEnabled = true
        Tools.broadcast(PedometerToolRegistration.BROADCAST_PEDOMETER_ENABLED)
        StepCounterService.start(context)
    }

    override fun disable() {
        prefs.pedometer.isEnabled = false
        Tools.broadcast(PedometerToolRegistration.BROADCAST_PEDOMETER_DISABLED)
        StepCounterService.stop(context)
    }

    private val stateChangePrefKeys = listOf(
        R.string.pref_pedometer_enabled,
        R.string.pref_low_power_mode
    ).map { context.getString(it) }

    private val distanceChangePrefKeys = listOf(
        context.getString(R.string.pref_stride_length)
    )

    private val stepsMutex = Mutex()
    private val initialPopulationTask = BackgroundTask {
        updateSteps()
    }

    init {
        initialPopulationTask.start()
        Tools.subscribe(PedometerToolRegistration.BROADCAST_STEPS_CHANGED) {
            updateSteps(it)
        }

        // Keep them up to date
        state.subscribe { true }
        steps.subscribe { true }
        distance.subscribe {
            Tools.broadcast(PedometerToolRegistration.BROADCAST_DISTANCE_CHANGED)
            true
        }

        prefsChanged.subscribe {
            if (it in stateChangePrefKeys) {
                _state.publish(calculateState())
            }

            if (it in distanceChangePrefKeys) {
                _distance.publish(calculateDistance())
            }

            true
        }
    }

    private suspend fun updateSteps(data: Bundle? = null) = stepsMutex.withLock {
        val stepCount = data?.getLong(PedometerToolRegistration.BROADCAST_PARAM_STEPS)
            ?: stepTrackerService.getOpenStepTrackingPeriod()?.steps
            ?: 0L
        _steps.publish(stepCount)
        _distance.publish(calculateDistance())
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

    fun isDisabledDueToPermissions(): Boolean {
        return !Permissions.canRecognizeActivity(context) && !prefs.isLowPowerModeOn
    }

    fun recalculateState() {
        _state.publish(calculateState())
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
        return paceCalculator.distance(steps.value.getOrDefault(0L)).meters()
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
