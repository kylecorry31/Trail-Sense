package com.kylecorry.trail_sense.tools.paths.quickactions

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.andromeda.core.topics.generic.replay
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.permissions.RequestRemoveBatteryRestrictionCommand
import com.kylecorry.trail_sense.shared.permissions.requestBacktrackPermission
import com.kylecorry.trail_sense.shared.quickactions.TopicQuickAction
import com.kylecorry.trail_sense.tools.paths.infrastructure.subsystem.BacktrackSubsystem

class QuickActionBacktrack(btn: ImageButton, fragment: Fragment) :
    TopicQuickAction(btn, fragment, hideWhenUnavailable = false) {

    private val backtrack = BacktrackSubsystem.getInstance(context)

    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.ic_tool_backtrack)
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        fragment.findNavController().navigateWithAnimation(R.id.fragmentBacktrack)
        return true
    }

    override fun onClick() {
        super.onClick()
        when (backtrack.getState()) {
            FeatureState.On -> backtrack.disable()
            FeatureState.Off -> {
                fragment.requestBacktrackPermission { success ->
                    if (success) {
                        fragment.inBackground {
                            backtrack.enable(true)
                            RequestRemoveBatteryRestrictionCommand(fragment).execute()
                        }
                    }
                }
            }

            FeatureState.Unavailable -> fragment.toast(context.getString(R.string.backtrack_disabled_low_power_toast))
        }
    }

    override val state: ITopic<FeatureState> = backtrack.state.replay()

}