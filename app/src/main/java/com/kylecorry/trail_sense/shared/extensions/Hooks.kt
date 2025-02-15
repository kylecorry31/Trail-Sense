package com.kylecorry.trail_sense.shared.extensions

import com.kylecorry.andromeda.core.ui.ReactiveComponent
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.trail_sense.shared.hooks.HookTriggers

fun ReactiveComponent.useCoroutineQueue(): CoroutineQueueRunner {
    return useMemo {
        CoroutineQueueRunner()
    }
}

fun ReactiveComponent.useTriggers(): HookTriggers {
    return useMemo {
        HookTriggers()
    }
}