package com.kylecorry.trail_sense.shared.quickactions

import androidx.fragment.app.Fragment
import com.kylecorry.luna.topics.generic.ITopic
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.QuickActionButton

abstract class TopicQuickAction(
    btn: QuickActionButtonView,
    fragment: Fragment,
    private val hideWhenUnavailable: Boolean = false
) :
    QuickActionButton(btn, fragment) {

    abstract val state: ITopic<FeatureState>

    override fun onResume() {
        super.onResume()
        state.subscribe(this::onStateChange)
    }

    override fun onPause() {
        super.onPause()
        state.unsubscribe(this::onStateChange)
    }

    override fun onDestroy() {
        super.onDestroy()
        onPause()
    }

    private fun onStateChange(state: FeatureState): Boolean {
        button.isVisible = !hideWhenUnavailable || state != FeatureState.Unavailable
        setState(state == FeatureState.On)
        return true
    }


}
