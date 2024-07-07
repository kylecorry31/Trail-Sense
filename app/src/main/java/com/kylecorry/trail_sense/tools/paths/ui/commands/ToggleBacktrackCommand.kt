package com.kylecorry.trail_sense.tools.paths.ui.commands

import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.permissions.RequestRemoveBatteryRestrictionCommand
import com.kylecorry.trail_sense.shared.permissions.requestBacktrackPermission
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.getFeatureState

class ToggleBacktrackCommand(
    private val fragment: Fragment,
    private val onPermissionDenied: () -> Unit = {}
) : Command {
    override fun execute() {
        fragment.inBackground {
            val service =
                Tools.getService(fragment.requireContext(), PathsToolRegistration.SERVICE_BACKTRACK)
                    ?: return@inBackground
            when (service.getFeatureState()) {
                FeatureState.On -> service.disable()
                FeatureState.Off -> {
                    fragment.requestBacktrackPermission { success ->
                        if (success) {
                            fragment.inBackground {
                                service.enable()
                                RequestRemoveBatteryRestrictionCommand(fragment).execute()
                            }
                        } else {
                            onPermissionDenied()
                        }
                    }
                }

                FeatureState.Unavailable -> fragment.toast(fragment.getString(R.string.backtrack_disabled_low_power_toast))
            }
        }
    }


}