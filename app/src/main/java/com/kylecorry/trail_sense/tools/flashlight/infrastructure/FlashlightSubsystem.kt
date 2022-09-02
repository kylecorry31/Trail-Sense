package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.andromeda.core.topics.generic.Topic
import com.kylecorry.andromeda.core.topics.generic.distinct
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.torch.Torch
import com.kylecorry.andromeda.torch.TorchStateChangedTopic
import com.kylecorry.sol.math.SolMath
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.FlashlightPreferenceRepo
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.getOrNull
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import java.time.Duration
import java.time.Instant
import java.util.*


class FlashlightSubsystem private constructor(private val context: Context) : IFlashlightSubsystem {

    private val torchChanged = TorchStateChangedTopic(context)
    private val cache by lazy { Preferences(context) }
    private val prefs by lazy { UserPreferences(context) }
    private val flashlightSettings by lazy { FlashlightPreferenceRepo(context) }
    private val torch by lazy { Torch(context) }

    private val _mode = Topic(defaultValue = Optional.of(getMode()))
    override val mode: ITopic<FlashlightMode>
        get() = _mode.distinct()

    override val brightnessLevels: Int
        get() = torch.brightnessLevels - 1

    private var isAvailable: Boolean = torch.isAvailable()

    private var isTransitioning = false
    private val transitionTimer = Timer {
        isTransitioning = false
    }

    private var brightness: Float = 1f

    private val modeLock = Object()
    private val torchLock = Object()

    init {
        _mode.subscribe { true }
        torchChanged.subscribe(this::onTorchStateChanged)
        brightness = prefs.flashlight.brightness
    }

    private fun on(newMode: FlashlightMode, bySystem: Boolean = false) = synchronized(modeLock) {
        clearTimeout()
        if (!bySystem) {
            isTransitioning = true
            transitionTimer.once(Duration.ofSeconds(1))
            setTimeout()
        } else {
            isTransitioning = false
            transitionTimer.stop()
        }
        val mode = getMode()
        _mode.publish(newMode)
        if (mode == FlashlightMode.Off) {
            FlashlightService.start(context)
        }
    }

    private fun off(bySystem: Boolean = false) = synchronized(modeLock) {
        clearTimeout()
        if (!bySystem) {
            isTransitioning = true
            transitionTimer.once(Duration.ofSeconds(1))
        } else {
            isTransitioning = false
            transitionTimer.stop()
        }
        _mode.publish(FlashlightMode.Off)
        FlashlightService.stop(context)
        torch.off()
    }

    override fun toggle() {
        if (getMode() == FlashlightMode.Torch) {
            off()
        } else {
            set(FlashlightMode.Torch)
        }
    }


    override fun set(mode: FlashlightMode) {
        when (mode) {
            FlashlightMode.Off -> off()
            else -> on(mode)
        }
    }

    override fun getMode(): FlashlightMode {
        return tryOrDefault(FlashlightMode.Off) {
            mode.getOrNull() ?: FlashlightMode.Off
        }
    }

    override fun isAvailable(): Boolean {
        return isAvailable
    }

    internal fun turnOn() = synchronized(torchLock) {
        if (brightnessLevels > 0) {
            val mapped = SolMath.map(brightness, 0f, 1f, 1f / (brightnessLevels + 1), 1f)
            torch.on(mapped)
        } else {
            torch.on()
        }
    }

    internal fun turnOff() = synchronized(torchLock) {
        torch.off()
    }

    private fun onTorchStateChanged(enabled: Boolean): Boolean {
        tryOrLog {
            if (!flashlightSettings.toggleWithSystem) {
                return@tryOrLog
            }

            synchronized(modeLock) {
                if (isTransitioning) {
                    return@tryOrLog
                }
            }

            if (!enabled && getMode() == FlashlightMode.Torch) {
                off(true)
            }

            if (enabled && getMode() == FlashlightMode.Off) {
                setBrightness(1f)
                on(FlashlightMode.Torch, true)
            }
        }
        return true
    }

    override fun setBrightness(brightness: Float) {
        prefs.flashlight.brightness = brightness
        this.brightness = brightness
        if (getMode() == FlashlightMode.Torch) {
            turnOn()
        }
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