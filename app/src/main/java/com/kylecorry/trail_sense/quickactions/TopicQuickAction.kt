package com.kylecorry.trail_sense.quickactions

import android.widget.ImageButton
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.QuickActionButton

abstract class TopicQuickAction(
    btn: ImageButton,
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
        CustomUiUtils.setButtonState(button, state == FeatureState.On)
        return true
    }


}