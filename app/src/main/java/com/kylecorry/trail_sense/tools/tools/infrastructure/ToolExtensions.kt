package com.kylecorry.trail_sense.tools.tools.infrastructure

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