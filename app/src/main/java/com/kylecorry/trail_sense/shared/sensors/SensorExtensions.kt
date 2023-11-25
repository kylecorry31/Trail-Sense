package com.kylecorry.trail_sense.shared.sensors

import android.util.Log
import com.kylecorry.andromeda.core.sensors.ISensor
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.luna.coroutines.IFlowable
import com.kylecorry.luna.coroutines.ListenerFlowWrapper
import com.kylecorry.trail_sense.shared.extensions.onDefault
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration

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

fun ISensor.asFlowable(): IFlowable<Unit> {
    return object : ListenerFlowWrapper<Unit>() {
        override fun start() {
            Log.d("Sensor", "Starting ${this@asFlowable}")
            subscribe(this::onSensorUpdate)
        }

        override fun stop() {
            Log.d("Sensor", "Stopping ${this@asFlowable}")
            unsubscribe(this::onSensorUpdate)
        }

        private fun onSensorUpdate(): Boolean {
            emit(Unit)
            return true
        }
    }
}