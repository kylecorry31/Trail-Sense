package com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance

import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ARGuidanceManager {

    private val _target: MutableStateFlow<ARGuidanceTarget?> = MutableStateFlow(null)
    val target: StateFlow<ARGuidanceTarget?> = _target.asStateFlow()

    private val _targetState: MutableStateFlow<ARGuidanceTargetState?> = MutableStateFlow(null)
    val targetState: StateFlow<ARGuidanceTargetState?> = _targetState.asStateFlow()

    private val refreshRunner = CoroutineQueueRunner()
    private val targetLock = Any()

    fun setTarget(newTarget: ARGuidanceTarget?) {
        synchronized(targetLock) {
            if (_target.value == newTarget) {
                return
            }

            _targetState.value = null
            _target.value = newTarget
        }
    }

    suspend fun updateTargetState(request: ARGuidanceRefreshRequest) {
        val currentTarget = target.value ?: return
        refreshRunner.enqueue {
            tryOrLog {
                val state = currentTarget.refresh(request)
                synchronized(targetLock) {
                    if (target.value === currentTarget) {
                        _targetState.value = state
                    }
                }
            }
        }
    }
}
