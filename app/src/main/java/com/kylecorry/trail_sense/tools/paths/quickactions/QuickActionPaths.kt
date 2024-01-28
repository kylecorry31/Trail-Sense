package com.kylecorry.trail_sense.tools.paths.quickactions

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.andromeda.core.topics.generic.replay
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.paths.infrastructure.subsystem.BacktrackSubsystem
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.quickactions.TopicQuickAction

class QuickActionPaths(btn: ImageButton, fragment: Fragment) :
    TopicQuickAction(btn, fragment, hideWhenUnavailable = false) {

    override fun onCreate() {
        super.onCreate()
        button.setImageResource(R.drawable.ic_tool_backtrack)
        button.setOnClickListener {
            fragment.findNavController().navigateWithAnimation(R.id.fragmentBacktrack)
        }
    }

    override val state: ITopic<FeatureState> =
        BacktrackSubsystem.getInstance(context).state.replay()

}