package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.trail_sense.shared.alerts.IAlerter
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.preferences.Flag

abstract class RequestOptionalPermissionCommand(
    private val context: Context,
    private val flag: Flag,
    private val alerter: IAlerter,
    private val isRequired: Specification<Context>
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
}