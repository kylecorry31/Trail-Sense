package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.andromeda.fragments.IPermissionRequester
import com.kylecorry.trail_sense.shared.alerts.IAlerter
import com.kylecorry.trail_sense.shared.preferences.Flag
import com.kylecorry.trail_sense.shared.preferences.PreferencesFlag
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem

class RequestBackgroundLocationCommand<T>(
    private val fragment: T,
    flag: Flag = PreferencesFlag(
        PreferencesSubsystem.getInstance(fragment.requireContext()).preferences,
        SHOWN_KEY
    ),
    alerter: IAlerter = BackgroundLocationAlerter(fragment),
    isRequired: Specification<Context> = IsBackgroundLocationRequired()
) : RequestOptionalPermissionCommand(
    fragment.requireContext(),
    flag,
    alerter,
    isRequired
) where T : Fragment, T : IPermissionRequester {
    companion object {
        private const val SHOWN_KEY = "cache_background_location_requested"
    }
}