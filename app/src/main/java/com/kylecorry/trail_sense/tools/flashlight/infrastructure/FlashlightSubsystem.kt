package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.andromeda.core.topics.generic.Topic
import com.kylecorry.andromeda.core.topics.generic.distinct
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.torch.ITorch
import com.kylecorry.andromeda.torch.Torch
import com.kylecorry.andromeda.torch.TorchStateChangedTopic
import com.kylecorry.sol.math.SolMath
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.getOrNull
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.flashlight.FlashlightToolRegistration
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.util.Optional

class FlashlightSubsystem private constructor(private val context: Context) : IFlashlightSubsystem {

    private val torchChanged by lazy { TorchStateChangedTopic(context) }
    private val cache by lazy { PreferencesSubsystem.getInstance(context).preferences }
    private val prefs by lazy { UserPreferences(context) }
    private var torch: ITorch? = null

    private val transitionDuration = Duration.ofSeconds(1)

    private val _mode = Topic(defaultValue = Optional.of(getMode()))
    override val mode: ITopic<FlashlightMode>
        get() = _mode.distinct()

    override val brightnessLevels: Int
        get() = (torch?.brightnessLevels ?: 1) - 1

    private var isAvailable: Boolean = Torch.isAvailable(context, true)

    private var isTransitioning = false
    private val transitionTimer = CoroutineTimer {
        isTransitioning = false
    }

    private var brightness: Float = 1f

    private val modeLock = Any()
    private val torchLock = Any()
    private val systemMonitorLock = Any()

    private val scope = CoroutineScope(Dispatchers.Default)

    private var systemMonitorCount = 0

    init {
        _mode.subscribe {
            Tools.broadcast(FlashlightToolRegistration.BROADCAST_FLASHLIGHT_STATE_CHANGED)
            true
        }
        scope.launch {
            brightness = prefs.flashlight.brightness
            torch = Torch(context)
            isAvailable = Torch.isAvailable(context)
        }
    }

    fun startSystemMonitor() {
        synchronized(systemMonitorLock) {
            systemMonitorCount++
            if (systemMonitorCount == 1) {
                torchChanged.subscribe(this@FlashlightSubsystem::onTorchStateChanged)
            }
        }
    }

    fun stopSystemMonitor() {
        synchronized(systemMonitorLock) {
            systemMonitorCount--
            if (systemMonitorCount == 0) {
                torchChanged.unsubscribe(this@FlashlightSubsystem::onTorchStateChanged)
            }
        }
    }

    private fun on(newMode: FlashlightMode, bySystem: Boolean = false) = synchronized(modeLock) {
        clearTimeout()
        if (!bySystem) {
            isTransitioning = true
            transitionTimer.once(transitionDuration)
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
            transitionTimer.once(transitionDuration)
        } else {
            isTransitioning = false
            transitionTimer.stop()
        }
        _mode.publish(FlashlightMode.Off)
        FlashlightService.stop(context)
        torch?.off()
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
            torch?.on(mapped)
        } else {
            torch?.on()
        }
    }

    internal fun turnOff() = synchronized(torchLock) {
        torch?.off()
    }

    private fun onTorchStateChanged(enabled: Boolean): Boolean {
        tryOrLog {
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
        @SuppressLint("StaticFieldLeak")
        private var instance: FlashlightSubsystem? = null
        fun getInstance(context: Context): FlashlightSubsystem {
            if (instance == null) {
                instance = FlashlightSubsystem(context.applicationContext)
            }
            return instance!!
        }
    }

}