package com.kylecorry.trail_sense.shared.sensors

import android.util.Log
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.sensors.ISensor
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.fragments.repeatInBackground
import com.kylecorry.luna.coroutines.IFlowable
import com.kylecorry.luna.coroutines.ListenerFlowWrapper
import com.kylecorry.trail_sense.shared.extensions.onDefault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import kotlin.coroutines.CoroutineContext

suspend fun readAll(
    sensors: List<ISensor>,
    timeout: Duration = Duration.ofMinutes(1),
    onlyIfInvalid: Boolean = false,
    forceStopOnCompletion: Boolean = false
) = onDefault {
    try {
        withTimeoutOrNull(timeout.toMillis()) {
            val jobs = mutableListOf<Job>()
            for (sensor in sensors) {
                if (!onlyIfInvalid || !sensor.hasValidReading) {
                    jobs.add(launch { sensor.read() })
                }
            }
            jobs.joinAll()
        }
    } finally {
        if (forceStopOnCompletion) {
            sensors.forEach {
                tryOrLog {
                    it.stop(null)
                }
            }
        }
    }
}

fun <T> Fragment.observeFlow2(
    flow: Flow<T>,
    state: BackgroundMinimumState = BackgroundMinimumState.Any,
    collectOn: CoroutineContext = Dispatchers.Default,
    observeOn: CoroutineContext = Dispatchers.Main,
    listener: suspend (T) -> Unit
) {
    repeatInBackground(state) {
        withContext(collectOn) {
            flow.collect {
                withContext(observeOn) {
                    listener(it)
                }
            }
        }
    }
}