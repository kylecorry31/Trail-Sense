package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.andromeda.core.sensors.ISensor
import com.kylecorry.andromeda.core.topics.ITopic
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.trail_sense.shared.extensions.onDefault
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import kotlin.coroutines.resume

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

suspend fun ISensor.readUntilValid() = suspendCancellableCoroutine { cont ->
    val callback: () -> Boolean = {
        if (hasValidReading) {
            cont.resume(Unit)
            false
        } else {
            true
        }
    }
    cont.invokeOnCancellation {
        unsubscribe(callback)
    }
    subscribe(callback)
}

suspend fun readAllUntilValid(
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
                    jobs.add(launch { sensor.readUntilValid() })
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