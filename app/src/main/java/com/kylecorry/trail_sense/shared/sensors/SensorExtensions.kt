package com.kylecorry.trail_sense.shared.sensors

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kylecorry.trailsensecore.infrastructure.sensors.ISensor
import kotlinx.coroutines.*
import kotlin.coroutines.resume

fun <T: ISensor> T.asLiveData(): LiveData<T> {
    lateinit var liveData: MutableLiveData<T>

    val callback: () -> Boolean = {
        liveData.value = this
        true
    }

    liveData = object : MutableLiveData<T>(null) {
        override fun onActive() {
            super.onActive()
            start(callback)
        }

        override fun onInactive() {
            super.onInactive()
            stop(callback)
        }
    }

    return liveData
}
suspend fun <T: ISensor> T.read() = suspendCancellableCoroutine<T> { cont ->
    val callback: () -> Boolean = {
        cont.resume(this)
        false
    }
    cont.invokeOnCancellation {
        stop(callback)
    }
    start(callback)
}