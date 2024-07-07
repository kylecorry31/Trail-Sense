package com.kylecorry.trail_sense.tools.paths.quickactions

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.permissions.RequestRemoveBatteryRestrictionCommand
import com.kylecorry.trail_sense.shared.permissions.requestBacktrackPermission
import com.kylecorry.trail_sense.shared.quickactions.ToolServiceQuickAction
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration

class QuickActionBacktrack(btn: ImageButton, fragment: Fragment) :
    ToolServiceQuickAction(
        btn,
        fragment,
        PathsToolRegistration.SERVICE_BACKTRACK,
        PathsToolRegistration.BROADCAST_BACKTRACK_STATE_CHANGED,
        hideWhenUnavailable = false
    ) {

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
        fragment.inBackground {
            when (state) {
                FeatureState.On -> service?.disable()
                FeatureState.Off -> {
                    fragment.requestBacktrackPermission { success ->
                        if (success) {
                            fragment.inBackground {
                                service?.enable()
                                RequestRemoveBatteryRestrictionCommand(fragment).execute()
                            }
                        }
                    }
                }

                FeatureState.Unavailable -> fragment.toast(context.getString(R.string.backtrack_disabled_low_power_toast))
            }
        }
    }
}