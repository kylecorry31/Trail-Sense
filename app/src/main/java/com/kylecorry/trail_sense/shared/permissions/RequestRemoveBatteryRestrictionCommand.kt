package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.shared.IAlerter
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.preferences.Flag
import com.kylecorry.trail_sense.shared.preferences.PreferencesFlag

class RequestRemoveBatteryRestrictionCommand(
    private val context: Context,
    private val flag: Flag = PreferencesFlag(Preferences(context), SHOWN_KEY),
    private val alerter: IAlerter = RemoveBatteryRestrictionsAlerter(context),
    private val isRequired: Specification<Context> = IsBatteryExemptionRequired()
) : Command {

    override fun execute() {
        if (!isRequired.isSatisfiedBy(context)) {
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