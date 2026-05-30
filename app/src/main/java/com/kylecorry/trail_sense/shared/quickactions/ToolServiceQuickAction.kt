package com.kylecorry.trail_sense.shared.quickactions

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.getFeatureState

abstract class ToolServiceQuickAction(
    btn: QuickActionButtonView,
    fragment: Fragment,
    private val serviceId: String,
    private val stateChangeBroadcastId: String,
    private val hideWhenUnavailable: Boolean = false
) : QuickActionButton(btn, fragment) {

    protected val service by lazy { Tools.getService(context, serviceId) }
    protected val state: FeatureState
        get() = service?.getFeatureState() ?: FeatureState.Unavailable

    override fun onResume() {
        super.onResume()
        updateState()
        Tools.subscribe(stateChangeBroadcastId, this::onStateChange)
    }

    override fun onPause() {
        super.onPause()
        Tools.unsubscribe(stateChangeBroadcastId, this::onStateChange)
    }

    override fun onDestroy() {
        super.onDestroy()
        onPause()
    }

    private suspend fun onStateChange(data: Bundle) = onMain {
        updateState()
    }

    private fun updateState() {
        button.isVisible = !hideWhenUnavailable || state != FeatureState.Unavailable
        setState(state == FeatureState.On)
    }


}
