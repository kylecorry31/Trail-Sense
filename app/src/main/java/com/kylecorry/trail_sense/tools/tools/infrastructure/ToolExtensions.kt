package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kylecorry.trail_sense.shared.FeatureState

fun ToolService.getFeatureState(): FeatureState {
    return if (isBlocked()) {
        FeatureState.Unavailable
    } else if (isRunning()) {
        FeatureState.On
    } else {
        FeatureState.Off
    }
}

fun Tools.observe(
    fragment: Fragment,
    toolBroadCastId: String,
    onReceiveBroadcast: (data: Bundle) -> Boolean
) {
    fragment.viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                subscribe(toolBroadCastId, onReceiveBroadcast)
            }

            Lifecycle.Event.ON_PAUSE -> {
                unsubscribe(toolBroadCastId, onReceiveBroadcast)
            }

            else -> {} // Do nothing
        }
    })
}