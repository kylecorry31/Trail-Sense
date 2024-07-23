package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.trail_sense.shared.alerts.IAlerter
import com.kylecorry.trail_sense.shared.preferences.Flag
import com.kylecorry.trail_sense.shared.preferences.PreferencesFlag
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem

class RequestRemoveBatteryRestrictionCommand(
    private val fragment: Fragment,
    flag: Flag = PreferencesFlag(
        PreferencesSubsystem.getInstance(fragment.requireContext()).preferences,
        SHOWN_KEY
    ),
    alerter: IAlerter = RemoveBatteryRestrictionsAlerter(fragment),
    isRequired: Specification<Context> = IsBatteryExemptionRequired()
) : RequestOptionalPermissionCommand(
    fragment.requireContext(),
    flag,
    alerter,
    isRequired
) {
    companion object {
        private const val SHOWN_KEY = "cache_battery_exemption_requested"
    }
}