package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.andromeda.fragments.IPermissionRequester
import com.kylecorry.trail_sense.shared.alerts.IAlerter
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.preferences.Flag

abstract class RequestOptionalPermissionCommand<T>(
    private val fragment: T,
    private val flag: Flag,
    private val alerter: IAlerter,
    private val isRequired: Specification<Context>
) : Command where T : Fragment, T : IPermissionRequester {

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
}