package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.andromeda.core.topics.generic.Topic
import com.kylecorry.andromeda.core.topics.generic.distinct
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.torch.Torch
import com.kylecorry.andromeda.torch.TorchStateChangedTopic
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.FlashlightPreferenceRepo
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightState
import java.time.Instant
import java.util.*


class FlashlightSubsystem private constructor(private val context: Context) : IFlashlightSubsystem {

    private val torchChanged = TorchStateChangedTopic(context)
    private val cache by lazy { Preferences(context) }
    private val prefs by lazy { UserPreferences(context) }
    private var isTurningOff = false
    private val flashlightSettings by lazy { FlashlightPreferenceRepo(context) }
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    private val _state = Topic(defaultValue = Optional.of(getState()))
    override val state: ITopic<FlashlightState>
        get() = _state.distinct()

    private var isAvailable: Boolean = Torch.isAvailable(context)

    init {
        _state.subscribe { true }
        torchChanged.subscribe(this::onTorchStateChanged)
    }

    override fun on(handleTimeout: Boolean) {
        clearTimeout()
        if (handleTimeout) {
            setTimeout()
        }
        SosService.stop(context)
        StrobeService.stop(context)
        FlashlightService.start(context)
        _state.publish(FlashlightState.On)
    }

    override fun off() {
        clearTimeout()
        SosService.stop(context)
        StrobeService.stop(context)
        FlashlightService.stop(context)
        isTurningOff = true
        forceOff(200)
        _state.publish(FlashlightState.Off)
    }

    override fun toggle(handleTimeout: Boolean) {
        if (getState() == FlashlightState.On) {
            off()
        } else {
            on(handleTimeout)
        }
    }

    private fun forceOff(millis: Long) {
        val increment = 20L
        if ((millis - increment) < 0) {
            isTurningOff = false
            return
        }
        handler.postDelayed({
            val flashlight = Torch(context)
            flashlight.off()
            forceOff(millis - increment)
        }, increment)
    }

    override fun sos(handleTimeout: Boolean) {
        clearTimeout()
        if (handleTimeout) {
            setTimeout()
        }
        StrobeService.stop(context)
        FlashlightService.stop(context)
        SosService.start(context)
        _state.publish(FlashlightState.SOS)
    }

    override fun strobe(handleTimeout: Boolean) {
        clearTimeout()
        if (handleTimeout) {
            setTimeout()
        }
        SosService.stop(context)
        FlashlightService.stop(context)
        StrobeService.start(context)
        _state.publish(FlashlightState.Strobe)
    }

    override fun set(state: FlashlightState, handleTimeout: Boolean) {
        when (state) {
            FlashlightState.Off -> off()
            FlashlightState.On -> on(handleTimeout)
            FlashlightState.SOS -> sos(handleTimeout)
            FlashlightState.Strobe -> strobe(handleTimeout)
        }
    }

    override fun getState(): FlashlightState {
        return when {
            FlashlightService.isRunning -> FlashlightState.On
            SosService.isRunning -> FlashlightState.SOS
            StrobeService.isRunning -> FlashlightState.Strobe
            else -> FlashlightState.Off
        }
    }

    override fun isAvailable(): Boolean {
        return isAvailable
    }

    private fun onTorchStateChanged(enabled: Boolean): Boolean {
        tryOrLog {
            if (!flashlightSettings.toggleWithSystem) {
                return@tryOrLog
            }
            if (isTurningOff) {
                return@tryOrLog
            }

            _state.publish(getState())

            if (!enabled && FlashlightService.isRunning) {
                off()
            }

            if (enabled && !FlashlightService.isRunning && !SosService.isRunning && !StrobeService.isRunning) {
                on(false)
            }
        }
        return true
    }

    private fun setTimeout() {
        if (prefs.flashlight.shouldTimeout) {
            cache.putInstant(
                context.getString(R.string.pref_flashlight_timeout_instant),
                Instant.now().plus(prefs.flashlight.timeout)
            )
        } else {
            clearTimeout()
        }
    }

    private fun clearTimeout() {
        cache.remove(context.getString(R.string.pref_flashlight_timeout_instant))
    }

    companion object {
        private var instance: FlashlightSubsystem? = null
        fun getInstance(context: Context): FlashlightSubsystem {
            if (instance == null) {
                instance = FlashlightSubsystem(context.applicationContext)
            }
            return instance!!
        }
    }

}