package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.andromeda.fragments.IPermissionRequester
import com.kylecorry.trail_sense.shared.alerts.IAlerter
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.preferences.Flag
import com.kylecorry.trail_sense.shared.preferences.PreferencesFlag
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem

class RequestRemoveBatteryRestrictionCommand<T>(
    private val fragment: T,
    private val flag: Flag = PreferencesFlag(PreferencesSubsystem.getInstance(fragment.requireContext()).preferences, SHOWN_KEY),
    private val alerter: IAlerter = RemoveBatteryRestrictionsAlerter(fragment),
    private val isRequired: Specification<Context> = IsBatteryExemptionRequired()
) : Command where T: Fragment, T: IPermissionRequester {

    override fun execute() {
        if (!isRequired.isSatisfiedBy(fragment.requireContext())) {
            flag.set(false)
            return
        }

        if (flag.get()) {
            return
        }

        flag.set(true)
        alerter.alert()
    }

    companion object {
        private const val SHOWN_KEY = "cache_battery_exemption_requested"
    }
}